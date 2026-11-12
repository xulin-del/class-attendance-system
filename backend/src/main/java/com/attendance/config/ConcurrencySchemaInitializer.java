package com.attendance.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConcurrencySchemaInitializer implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(ConcurrencySchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public ConcurrencySchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (tableExists("attendance_records")) {
            addColumnIfMissing("attendance_records", "session_key", "VARCHAR(80) NULL");
            ensureIndex(
                    "attendance_records",
                    "idx_attendance_course_student",
                    "INDEX idx_attendance_course_student (course_id, student_id, create_time)"
            );
            ensureIndex(
                    "attendance_records",
                    "uk_attendance_session_student",
                    "UNIQUE INDEX uk_attendance_session_student (session_key, student_id)"
            );
        }

        if (tableExists("attendance_applications")) {
            ensureIndex(
                    "attendance_applications",
                    "uk_record_id",
                    "UNIQUE INDEX uk_record_id (record_id)"
            );
        }

        if (tableExists("digital_tokens")) {
            ensureIndex(
                    "digital_tokens",
                    "idx_digital_token_lookup",
                    "INDEX idx_digital_token_lookup (token, status, expire_time)"
            );
        }

        if (tableExists("location_signs")) {
            ensureIndex(
                    "location_signs",
                    "idx_location_course_status",
                    "INDEX idx_location_course_status (course_id, status, create_time)"
            );
        }

        if (tableExists("sign_reminders")) {
            addColumnIfMissing("sign_reminders", "reminder_type", "VARCHAR(20) DEFAULT 'sign'");
            addColumnIfMissing("sign_reminders", "schedule_id", "INT NULL");
            addColumnIfMissing("sign_reminders", "reminder_date", "DATE NULL");
            removeDuplicateReminders();
            ensureIndex(
                    "sign_reminders",
                    "uk_schedule_reminder_day",
                    "UNIQUE INDEX uk_schedule_reminder_day (schedule_id, reminder_date, reminder_type)"
            );
            ensureIndex(
                    "sign_reminders",
                    "uk_sign_reminder_code",
                    "UNIQUE INDEX uk_sign_reminder_code (sign_code)"
            );
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition
            );
            log.info("已为 {}.{} 添加并发控制字段", tableName, columnName);
        }
    }

    private void ensureIndex(String tableName, String indexName, String indexDefinition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD " + indexDefinition);
            log.info("已创建并发控制索引 {}.{}", tableName, indexName);
        } catch (DataAccessException e) {
            log.error("创建并发控制索引 {}.{} 失败，请先清理冲突数据", tableName, indexName, e);
        }
    }

    private void removeDuplicateReminders() {
        int duplicateClassReminders = jdbcTemplate.update(
                "DELETE duplicate_reminder FROM sign_reminders duplicate_reminder " +
                        "JOIN sign_reminders kept_reminder " +
                        "ON duplicate_reminder.schedule_id = kept_reminder.schedule_id " +
                        "AND duplicate_reminder.reminder_date = kept_reminder.reminder_date " +
                        "AND duplicate_reminder.reminder_type = kept_reminder.reminder_type " +
                        "AND duplicate_reminder.id > kept_reminder.id " +
                        "WHERE duplicate_reminder.schedule_id IS NOT NULL " +
                        "AND duplicate_reminder.reminder_date IS NOT NULL"
        );
        int duplicateSignReminders = jdbcTemplate.update(
                "DELETE duplicate_reminder FROM sign_reminders duplicate_reminder " +
                        "JOIN sign_reminders kept_reminder " +
                        "ON duplicate_reminder.sign_code = kept_reminder.sign_code " +
                        "AND duplicate_reminder.id > kept_reminder.id " +
                        "WHERE duplicate_reminder.sign_code IS NOT NULL"
        );
        if (duplicateClassReminders > 0 || duplicateSignReminders > 0) {
            log.warn("已清理重复提醒：课前提醒 {} 条，签到提醒 {} 条",
                    duplicateClassReminders, duplicateSignReminders);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}

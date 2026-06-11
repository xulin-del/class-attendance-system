package com.attendance.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AuthSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    public AuthSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        addColumnIfMissing("teachers", "phone", "VARCHAR(20)");
        addColumnIfMissing("students", "password", "VARCHAR(100)");
        addColumnIfMissing("students", "phone", "VARCHAR(20)");
        addColumnIfMissing("sign_reminders", "reminder_type", "VARCHAR(20) DEFAULT 'sign'");
        addColumnIfMissing("sign_reminders", "schedule_id", "INT NULL");
        addColumnIfMissing("sign_reminders", "reminder_date", "DATE NULL");
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }
}

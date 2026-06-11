package com.attendance.service;

import com.attendance.entity.Schedule;
import com.attendance.entity.SignReminder;
import com.attendance.mapper.ScheduleMapper;
import com.attendance.mapper.SignReminderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ScheduleReminderService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleReminderService.class);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ScheduleMapper scheduleMapper;
    private final SignReminderMapper signReminderMapper;

    public ScheduleReminderService(ScheduleMapper scheduleMapper, SignReminderMapper signReminderMapper) {
        this.scheduleMapper = scheduleMapper;
        this.signReminderMapper = signReminderMapper;
    }

    @Scheduled(fixedDelay = 60000)
    public void generateUpcomingClassReminders() {
        try {
            LocalDate today = LocalDate.now(ZONE_ID);
            LocalTime now = LocalTime.now(ZONE_ID);
            generateUpcomingClassReminders(today, now);
        } catch (Exception e) {
            log.error("生成课前提醒失败", e);
        }
    }

    public void generateUpcomingClassRemindersNow() {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalTime now = LocalTime.now(ZONE_ID);
        generateUpcomingClassReminders(today, now);
    }

    private void generateUpcomingClassReminders(LocalDate today, LocalTime now) {
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalTime remindUntil = now.plusMinutes(5);
        String reminderDate = today.toString();
        List<Schedule> schedules = scheduleMapper.selectByDayOfWeek(dayOfWeek);

        for (Schedule schedule : schedules) {
            if (schedule.getId() == null || schedule.getStartTime() == null) {
                continue;
            }

            LocalTime startTime = parseTime(schedule.getStartTime());
            if (startTime == null || startTime.isBefore(now) || startTime.isAfter(remindUntil)) {
                continue;
            }

            if (signReminderMapper.countClassReminder(schedule.getId(), reminderDate) > 0) {
                continue;
            }

            signReminderMapper.insert(buildReminder(schedule, today, startTime));
        }
    }

    private SignReminder buildReminder(Schedule schedule, LocalDate today, LocalTime startTime) {
        SignReminder reminder = new SignReminder();
        reminder.setCourseId(schedule.getCourseId());
        reminder.setCourseName(schedule.getCourseName());
        reminder.setClassName(schedule.getClassName());
        reminder.setTeacherId(schedule.getTeacherId());
        reminder.setSignCode("CLASS_" + schedule.getId() + "_" + today.format(DateTimeFormatter.BASIC_ISO_DATE));
        reminder.setStatus("pending");
        reminder.setReminderType("class");
        reminder.setScheduleId(schedule.getId());
        reminder.setReminderDate(Date.valueOf(today));
        reminder.setExpireTime(toDate(today, resolveExpireTime(schedule, startTime)));
        return reminder;
    }

    private LocalTime resolveExpireTime(Schedule schedule, LocalTime startTime) {
        LocalTime endTime = parseTime(schedule.getEndTime());
        if (endTime == null || !endTime.isAfter(startTime)) {
            return startTime.plusMinutes(30);
        }
        return endTime;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("课表时间格式不正确: {}", value);
            return null;
        }
    }

    private java.util.Date toDate(LocalDate date, LocalTime time) {
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return java.util.Date.from(dateTime.atZone(ZONE_ID).toInstant());
    }
}

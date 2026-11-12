package com.attendance.service;

import com.attendance.entity.AttendanceApplication;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.Course;
import com.attendance.entity.DigitalToken;
import com.attendance.entity.Schedule;
import com.attendance.entity.SignReminder;
import com.attendance.entity.SignCode;
import com.attendance.mapper.AttendanceApplicationMapper;
import com.attendance.mapper.AttendanceRecordMapper;
import com.attendance.mapper.CourseMapper;
import com.attendance.mapper.DigitalTokenMapper;
import com.attendance.mapper.LocationSignMapper;
import com.attendance.mapper.ScheduleMapper;
import com.attendance.mapper.SignCodeMapper;
import com.attendance.mapper.SignReminderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConcurrencyControlServiceTest {

    @Test
    void onlyOneConcurrentSignRequestCanWinTheConditionalUpdate() throws Exception {
        AttendanceRecordMapper recordMapper = mock(AttendanceRecordMapper.class);
        CourseMapper courseMapper = mock(CourseMapper.class);
        SignCodeMapper signCodeMapper = mock(SignCodeMapper.class);
        AttendanceService service = attendanceService(recordMapper, courseMapper, signCodeMapper);

        SignCode signCode = new SignCode();
        signCode.setId(7);
        signCode.setCourseId(1);
        signCode.setSignCode("ATT_1_123456");
        when(signCodeMapper.selectActiveBySignCode("ATT_1_123456")).thenReturn(signCode);
        Course course = new Course();
        course.setCourseId(1);
        when(courseMapper.selectByIdForShare(1)).thenReturn(course);

        AtomicInteger updateCalls = new AtomicInteger();
        when(recordMapper.markSignedBySession(eq("SIGN:7"), eq("S001"), any()))
                .thenAnswer(invocation -> updateCalls.getAndIncrement() == 0 ? 1 : 0);

        List<Boolean> results = runConcurrently(
                () -> service.signInByCode("ATT_1_123456", "S001"),
                () -> service.signInByCode("ATT_1_123456", "S001")
        );

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, results.stream().filter(result -> !result).count());
        verify(recordMapper, times(2)).markSignedBySession(eq("SIGN:7"), eq("S001"), any());
    }

    @Test
    void onlyOneConcurrentApprovalCanChangeTheRecord() throws Exception {
        AttendanceApplicationMapper applicationMapper = mock(AttendanceApplicationMapper.class);
        AttendanceRecordMapper recordMapper = mock(AttendanceRecordMapper.class);
        ApplicationService service = new ApplicationService();
        ReflectionTestUtils.setField(service, "applicationMapper", applicationMapper);
        ReflectionTestUtils.setField(service, "recordMapper", recordMapper);

        AttendanceApplication application = new AttendanceApplication();
        application.setId(5);
        application.setRecordId(11);
        application.setStatus("pending");
        when(applicationMapper.selectById(5)).thenReturn(application);

        AtomicInteger updateCalls = new AtomicInteger();
        when(applicationMapper.updateStatusIfPending(
                eq(5), eq("approved"), any(), any()
        )).thenAnswer(invocation -> updateCalls.getAndIncrement() == 0 ? 1 : 0);

        AttendanceRecord record = new AttendanceRecord();
        record.setRecordId(11);
        when(recordMapper.selectById(11)).thenReturn(record);

        List<ApplicationService.ApplicationResult> results = runConcurrently(
                () -> service.approveApplication(5),
                () -> service.approveApplication(5)
        );

        assertEquals(1, results.stream().filter(result -> result.success).count());
        assertEquals(1, results.stream().filter(result -> !result.success).count());
        verify(recordMapper, times(1)).updateById(record);
    }

    @Test
    void digitalTokenUsesDatabaseIdempotencyInsteadOfProcessMemory() throws Exception {
        AttendanceRecordMapper recordMapper = mock(AttendanceRecordMapper.class);
        CourseMapper courseMapper = mock(CourseMapper.class);
        DigitalTokenMapper tokenMapper = mock(DigitalTokenMapper.class);
        DigitalTokenService service = new DigitalTokenService();
        ReflectionTestUtils.setField(service, "attendanceRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "digitalTokenMapper", tokenMapper);
        ReflectionTestUtils.setField(service, "signCodeMapper", mock(SignCodeMapper.class));
        ReflectionTestUtils.setField(service, "locationSignMapper", mock(LocationSignMapper.class));

        DigitalToken token = new DigitalToken();
        token.setId(9);
        token.setCourseId(1);
        token.setToken("1234");
        when(tokenMapper.selectByToken("1234")).thenReturn(token);
        Course course = new Course();
        course.setCourseId(1);
        when(courseMapper.selectByIdForShare(1)).thenReturn(course);

        AtomicInteger updateCalls = new AtomicInteger();
        when(recordMapper.markSignedBySession(eq("TOKEN:9"), eq("S001"), any()))
                .thenAnswer(invocation -> updateCalls.getAndIncrement() == 0 ? 1 : 0);

        List<DigitalTokenService.DigitalSignInResult> results = runConcurrently(
                () -> service.signInByToken("1234", "S001"),
                () -> service.signInByToken("1234", "S001")
        );

        assertEquals(1, results.stream().filter(result -> result.success).count());
        assertEquals(1, results.stream().filter(result -> !result.success).count());
    }

    @Test
    void reminderGenerationReliesOnDatabaseInsertIfAbsent() {
        ScheduleMapper scheduleMapper = mock(ScheduleMapper.class);
        SignReminderMapper reminderMapper = mock(SignReminderMapper.class);
        ScheduleReminderService service = new ScheduleReminderService(scheduleMapper, reminderMapper);

        Schedule schedule = new Schedule();
        schedule.setId(3);
        schedule.setStartTime("09:03");
        schedule.setEndTime("10:00");
        when(scheduleMapper.selectByDayOfWeek(anyInt())).thenReturn(Collections.singletonList(schedule));
        when(reminderMapper.insertIfAbsent(any(SignReminder.class))).thenReturn(0);

        ReflectionTestUtils.invokeMethod(
                service,
                "generateUpcomingClassReminders",
                LocalDate.of(2026, 11, 12),
                LocalTime.of(9, 0)
        );

        verify(reminderMapper).insertIfAbsent(any(SignReminder.class));
        verify(reminderMapper, times(0)).insert(any(SignReminder.class));
    }

    private AttendanceService attendanceService(
            AttendanceRecordMapper recordMapper,
            CourseMapper courseMapper,
            SignCodeMapper signCodeMapper
    ) {
        AttendanceService service = new AttendanceService();
        ReflectionTestUtils.setField(service, "attendanceRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "signCodeMapper", signCodeMapper);
        ReflectionTestUtils.setField(service, "locationSignMapper", mock(LocationSignMapper.class));
        ReflectionTestUtils.setField(service, "digitalTokenMapper", mock(DigitalTokenMapper.class));
        ReflectionTestUtils.setField(service, "scheduleMapper", mock(ScheduleMapper.class));
        return service;
    }

    @SafeVarargs
    private final <T> List<T> runConcurrently(Callable<T>... tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = Arrays.stream(tasks)
                    .map(task -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return task.call();
                    }))
                    .collect(Collectors.toList());
            ready.await();
            start.countDown();
            return futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.toList());
        } finally {
            executor.shutdownNow();
        }
    }
}

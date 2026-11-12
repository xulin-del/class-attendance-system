package com.attendance.service;

import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.Course;
import com.attendance.entity.LocationSign;
import com.attendance.entity.SignCode;
import com.attendance.mapper.AttendanceRecordMapper;
import com.attendance.mapper.CourseMapper;
import com.attendance.mapper.DigitalTokenMapper;
import com.attendance.mapper.LocationSignMapper;
import com.attendance.mapper.SignCodeMapper;
import com.attendance.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private LocationSignMapper locationSignMapper;
    @Autowired
    private SignCodeMapper signCodeMapper;
    @Autowired
    private DigitalTokenMapper digitalTokenMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;

    @Transactional
    public StartResult startAttendance(Integer courseId, String className) {
        Course course = lockCourseForWrite(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        closePreviousSessions(courseId);
        attendanceRecordMapper.deleteByCourseId(courseId);

        Date now = new Date();
        SignCode signCode = createSignCode(courseId, now);
        String sessionKey = signSessionKey(signCode.getId());
        attendanceRecordMapper.insertRoster(courseId, course.getClassName(), sessionKey, now);

        StartResult sr = new StartResult();
        sr.signCode = signCode.getSignCode();
        return sr;
    }

    private SignCode createSignCode(Integer courseId, Date now) {
        String signCodeStr = generateSignCode(courseId);
        SignCode signCode = new SignCode();
        signCode.setSignCode(signCodeStr);
        signCode.setCourseId(courseId);
        signCode.setStatus("active");
        signCode.setCreateTime(now);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 30);
        signCode.setExpireTime(cal.getTime());
        signCodeMapper.insert(signCode);
        return signCode;
    }

    public static class StartResult {
        public String signCode;
    }

    private String generateSignCode(Integer courseId) {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return "ATT_" + courseId + "_" + code;
    }

    @Transactional
    public boolean signIn(Integer courseId, String studentId) {
        Course course = courseMapper.selectByIdForShare(courseId);
        if (course == null) {
            return false;
        }
        SignCode signCode = signCodeMapper.selectActiveByCourseId(courseId);
        return signCode != null && markSigned(signCode, studentId);
    }

    @Transactional
    public boolean signInByCode(String signCodeStr, String studentId) {
        SignCode initialSignCode = signCodeMapper.selectActiveBySignCode(signCodeStr);
        if (initialSignCode == null) {
            return false;
        }
        Course course = courseMapper.selectByIdForShare(initialSignCode.getCourseId());
        if (course == null) {
            return false;
        }
        SignCode activeSignCode = signCodeMapper.selectActiveBySignCode(signCodeStr);
        return activeSignCode != null && markSigned(activeSignCode, studentId);
    }

    private boolean markSigned(SignCode signCode, String studentId) {
        return attendanceRecordMapper.markSignedBySession(
                signSessionKey(signCode.getId()), studentId, new Date()
        ) > 0;
    }

    private String signSessionKey(Integer signCodeId) {
        return "SIGN:" + signCodeId;
    }

    public Integer getCourseIdBySignCode(String signCodeStr) {
        SignCode signCode = signCodeMapper.selectActiveBySignCode(signCodeStr);
        if (signCode == null) {
            return null;
        }
        return signCode.getCourseId();
    }

    public List<AttendanceRecord> getAttendanceRecords(Integer courseId) {
        return attendanceRecordMapper.selectByCourseId(courseId);
    }

    public List<AttendanceRecord> getStudentAttendance(String studentId) {
        return attendanceRecordMapper.selectByStudentId(studentId);
    }

    @Transactional
    public void endAttendance(Integer courseId) {
        Course course = lockCourseForWrite(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        closePreviousSessions(courseId);
        attendanceRecordMapper.markUnsignedAsAbsent(courseId);
    }

    private void closePreviousSessions(Integer courseId) {
        signCodeMapper.expireByCourseId(courseId);
        digitalTokenMapper.expireByCourseId(courseId);
        LocationSign activeLocationSign = locationSignMapper.selectActiveByCourseId(courseId);
        if (activeLocationSign != null) {
            activeLocationSign.setStatus("已结束");
            activeLocationSign.setEndTime(new Date());
            locationSignMapper.updateById(activeLocationSign);
        }
    }

    private Course lockCourseForWrite(Integer courseId) {
        return courseMapper.selectByIdForUpdate(courseId);
    }

    @Transactional
    public LocationSignResult startLocationAttendance(Integer courseId, String className, Double latitude, Double longitude, Integer radius) {
        Course course = lockCourseForWrite(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        closePreviousSessions(courseId);
        attendanceRecordMapper.deleteByCourseId(courseId);

        Date now = new Date();
        SignCode signCode = createSignCode(courseId, now);
        String sessionKey = signSessionKey(signCode.getId());
        attendanceRecordMapper.insertRoster(courseId, course.getClassName(), sessionKey, now);

        LocationSign locationSign = new LocationSign();
        locationSign.setCourseId(courseId);
        locationSign.setSignCode(signCode.getSignCode());
        locationSign.setLatitude(latitude);
        locationSign.setLongitude(longitude);
        locationSign.setRadius(radius);
        locationSign.setStatus("进行中");
        locationSign.setCreateTime(new Date());
        locationSignMapper.insert(locationSign);

        LocationSignResult result = new LocationSignResult();
        result.signCode = signCode.getSignCode();
        result.latitude = latitude;
        result.longitude = longitude;
        result.radius = radius;
        return result;
    }

    @Transactional
    public LocationSignInResult signInByLocation(Integer courseId, String studentId, Double studentLat, Double studentLng) {
        LocationSign initialLocationSign = locationSignMapper.selectActiveByCourseId(courseId);
        if (initialLocationSign == null) {
            return locationFailure("该课程暂无进行中的定位签到");
        }
        Course course = courseMapper.selectByIdForShare(courseId);
        if (course == null) {
            return locationFailure("课程不存在");
        }

        LocationSign locationSign = locationSignMapper.selectActiveByCourseId(courseId);
        if (locationSign == null) {
            return locationFailure("该课程暂无进行中的定位签到");
        }

        double distance = calculateDistance(
            locationSign.getLatitude(), locationSign.getLongitude(),
            studentLat, studentLng
        );

        if (distance > locationSign.getRadius()) {
            LocationSignInResult result = new LocationSignInResult();
            result.success = false;
            result.message = "距离太远，无法签到（当前距离" + Math.round(distance) + "米，有效范围" + locationSign.getRadius() + "米）";
            result.distance = Math.round(distance * 100.0) / 100.0;
            return result;
        }

        SignCode signCode = signCodeMapper.selectActiveBySignCode(locationSign.getSignCode());
        if (signCode == null || !courseId.equals(signCode.getCourseId())) {
            return locationFailure("签到已结束");
        }

        boolean signed = markSigned(signCode, studentId);
        LocationSignInResult result = new LocationSignInResult();
        result.success = signed;
        result.message = signed ? "签到成功" : "签到失败，可能已经签到或不在签到名单中";
        result.distance = Math.round(distance * 100.0) / 100.0;
        return result;
    }

    private LocationSignInResult locationFailure(String message) {
        LocationSignInResult result = new LocationSignInResult();
        result.success = false;
        result.message = message;
        return result;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public LocationSign getActiveLocationSign(Integer courseId) {
        return locationSignMapper.selectActiveByCourseId(courseId);
    }

    public List<DailyAttendanceStats> getWeeklyAttendanceStats(String teacherId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        
        String endDate = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -6);
        String startDate = sdf.format(cal.getTime());
        
        List<Map<String, Object>> dailyStats = attendanceRecordMapper.selectDailyStatsByTeacherId(teacherId, startDate, endDate);
        
        Map<String, DailyAttendanceStats> statsMap = new java.util.HashMap<>();
        for (Map<String, Object> stat : dailyStats) {
            String date = stat.get("date").toString();
            int total = ((Number) stat.get("total")).intValue();
            int signed = ((Number) stat.get("signed")).intValue();
            double rate = total > 0 ? (signed * 100.0 / total) : 0;
            
            DailyAttendanceStats das = new DailyAttendanceStats();
            das.date = date;
            das.rate = Math.round(rate * 10) / 10.0;
            statsMap.put(date, das);
        }
        
        List<DailyAttendanceStats> result = new ArrayList<>();
        cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String date = sdf.format(cal.getTime());
            
            DailyAttendanceStats das = statsMap.get(date);
            if (das == null) {
                das = new DailyAttendanceStats();
                das.date = date;
                das.rate = 0.0;
            }
            result.add(das);
        }
        
        return result;
    }

    public static class DailyAttendanceStats {
        public String date;
        public Double rate;
    }

    public static class WeeklyDayStats {
        public int dayOfWeek;
        public String dayName;
        public int signedCount;
        public boolean hasClass;
    }

    public List<WeeklyDayStats> getWeeklyDayStats(String teacherId) {
        List<Integer> hasClassDays = scheduleMapper.selectDistinctDaysByTeacherId(teacherId);
        java.util.Set<Integer> hasClassSet = new java.util.HashSet<>(hasClassDays);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        int todayDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (todayDayOfWeek == Calendar.SUNDAY) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五"};
        int[] dayOfWeeks = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};
        
        List<WeeklyDayStats> result = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.set(Calendar.DAY_OF_WEEK, dayOfWeeks[i]);
            if (todayDayOfWeek == Calendar.SUNDAY) {
                dayCal.add(Calendar.WEEK_OF_YEAR, -1);
            } else if (todayDayOfWeek == Calendar.SATURDAY) {
                dayCal.add(Calendar.WEEK_OF_YEAR, 0);
            } else if (todayDayOfWeek < dayOfWeeks[i]) {
                dayCal.add(Calendar.WEEK_OF_YEAR, -1);
            }
            
            String date = sdf.format(dayCal.getTime());
            int dayOfWeekNum = i + 1;
            
            List<Map<String, Object>> dayStats = attendanceRecordMapper.selectDailyStatsByTeacherId(teacherId, date, date);
            int signedCount = 0;
            for (Map<String, Object> stat : dayStats) {
                Number signed = (Number) stat.get("signed");
                if (signed != null) {
                    signedCount += signed.intValue();
                }
            }
            
            WeeklyDayStats wds = new WeeklyDayStats();
            wds.dayOfWeek = dayOfWeekNum;
            wds.dayName = dayNames[i];
            wds.signedCount = signedCount;
            wds.hasClass = hasClassSet.contains(dayOfWeekNum);
            result.add(wds);
        }
        
        return result;
    }

    public static class LocationSignResult {
        public String signCode;
        public Double latitude;
        public Double longitude;
        public Integer radius;
    }

    public static class LocationSignInResult {
        public boolean success;
        public String message;
        public Double distance;
    }

    public List<Map<String, Object>> getRecordsByTeacherIdAndDate(String teacherId, String date) {
        return attendanceRecordMapper.selectRecordsByTeacherIdAndDate(teacherId, date);
    }

    public List<Map<String, Object>> getStudentRecordsWithCourse(String studentId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return attendanceRecordMapper.selectStudentRecordsWithCourse(studentId, offset, pageSize);
    }

    public Integer countStudentRecords(String studentId) {
        return attendanceRecordMapper.countStudentRecords(studentId);
    }

    public Map<String, Object> getTeacherOverallStats(String teacherId) {
        List<Map<String, Object>> allRecords = attendanceRecordMapper.selectRecordsByTeacherId(teacherId);
        
        int total = 0;
        int signed = 0;
        int absent = 0;
        int late = 0;
        
        for (Map<String, Object> record : allRecords) {
            total++;
            Number hasSigned = (Number) record.get("has_signed");
            Number hasAbsent = (Number) record.get("has_absent");
            Number hasLate = (Number) record.get("has_late");
            
            if (hasSigned != null && hasSigned.intValue() == 1) {
                signed++;
            } else if (hasAbsent != null && hasAbsent.intValue() == 1) {
                absent++;
            } else if (hasLate != null && hasLate.intValue() == 1) {
                late++;
            }
        }
        
        double rate = total > 0 ? (signed * 100.0 / total) : 0;
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", total);
        result.put("signed", signed);
        result.put("absent", absent);
        result.put("late", late);
        result.put("rate", Math.round(rate * 10) / 10.0);
        return result;
    }

    public Map<String, Object> getTeacherDashboardStats(String teacherId) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        int totalCourses = attendanceRecordMapper.countCoursesByTeacherId(teacherId);
        result.put("totalCourses", totalCourses);
        
        int totalStudents = attendanceRecordMapper.countStudentsByTeacherId(teacherId);
        result.put("totalStudents", totalStudents);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        int todaySign = attendanceRecordMapper.countTodaySignedByTeacherId(teacherId, today);
        result.put("todaySign", todaySign);
        
        return result;
    }
}

package com.attendance.service;

import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.DigitalToken;
import com.attendance.entity.Student;
import com.attendance.mapper.AttendanceRecordMapper;
import com.attendance.mapper.DigitalTokenMapper;
import com.attendance.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DigitalTokenService {
    @Autowired
    private DigitalTokenMapper digitalTokenMapper;
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired
    private StudentMapper studentMapper;

    private static final ConcurrentHashMap<String, Long> recentSubmissions = new ConcurrentHashMap<>();

    public DigitalToken generateToken(Integer courseId, String teacherId, int expireSeconds) {
        digitalTokenMapper.expireByCourseId(courseId);

        String token = generateRandomToken();
        
        DigitalToken digitalToken = new DigitalToken();
        digitalToken.setCourseId(courseId);
        digitalToken.setTeacherId(teacherId);
        digitalToken.setToken(token);
        digitalToken.setStatus("active");
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, expireSeconds);
        digitalToken.setExpireTime(cal.getTime());
        
        digitalTokenMapper.insert(digitalToken);
        return digitalToken;
    }

    private String generateRandomToken() {
        Random random = new Random();
        int num = 1000 + random.nextInt(9000);
        return String.valueOf(num);
    }

    public DigitalToken getActiveToken(Integer courseId) {
        return digitalTokenMapper.selectActiveByCourseId(courseId);
    }

    @Transactional
    public DigitalSignInResult signInByToken(String token, String studentId) {
        String key = studentId + "_" + token;
        long now = System.currentTimeMillis();
        Long lastTime = recentSubmissions.get(key);
        if (lastTime != null && now - lastTime < 60000) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "请勿重复提交";
            return result;
        }

        DigitalToken digitalToken = digitalTokenMapper.selectByToken(token);
        if (digitalToken == null) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "口令无效或已过期";
            return result;
        }

        Integer courseId = digitalToken.getCourseId();
        AttendanceRecord record = attendanceRecordMapper.selectByCourseIdAndStudentId(courseId, studentId);
        
        if (record == null) {
            Student student = studentMapper.selectByStudentId(studentId);
            if (student == null) {
                DigitalSignInResult result = new DigitalSignInResult();
                result.success = false;
                result.message = "学生信息不存在";
                return result;
            }
            record = new AttendanceRecord();
            record.setCourseId(courseId);
            record.setStudentId(studentId);
            record.setStatus("已签到");
            record.setSignTime(new Date());
            record.setCreateTime(new Date());
            attendanceRecordMapper.insert(record);
        } else if ("已签到".equals(record.getStatus())) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "你已经签到过了";
            result.courseId = courseId;
            return result;
        } else {
            record.setStatus("已签到");
            record.setSignTime(new Date());
            attendanceRecordMapper.updateById(record);
        }

        recentSubmissions.put(key, now);

        DigitalSignInResult result = new DigitalSignInResult();
        result.success = true;
        result.message = "签到成功";
        result.courseId = courseId;
        return result;
    }

    public void expireToken(Integer courseId) {
        digitalTokenMapper.expireByCourseId(courseId);
    }

    public static class DigitalSignInResult {
        public boolean success;
        public String message;
        public Integer courseId;
    }
}

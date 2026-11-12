package com.attendance.service;

import com.attendance.entity.Course;
import com.attendance.entity.DigitalToken;
import com.attendance.entity.LocationSign;
import com.attendance.mapper.AttendanceRecordMapper;
import com.attendance.mapper.CourseMapper;
import com.attendance.mapper.DigitalTokenMapper;
import com.attendance.mapper.LocationSignMapper;
import com.attendance.mapper.SignCodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

@Service
public class DigitalTokenService {
    @Autowired
    private DigitalTokenMapper digitalTokenMapper;
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private SignCodeMapper signCodeMapper;
    @Autowired
    private LocationSignMapper locationSignMapper;

    @Transactional
    public DigitalToken generateToken(Integer courseId, String teacherId, int expireSeconds) {
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }

        signCodeMapper.expireByCourseId(courseId);
        digitalTokenMapper.expireByCourseId(courseId);
        LocationSign activeLocationSign = locationSignMapper.selectActiveByCourseId(courseId);
        if (activeLocationSign != null) {
            activeLocationSign.setStatus("已结束");
            activeLocationSign.setEndTime(new Date());
            locationSignMapper.updateById(activeLocationSign);
        }
        attendanceRecordMapper.deleteByCourseId(courseId);

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
        attendanceRecordMapper.insertRoster(
                courseId,
                course.getClassName(),
                tokenSessionKey(digitalToken.getId()),
                new Date()
        );
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
        DigitalToken initialToken = digitalTokenMapper.selectByToken(token);
        if (initialToken == null) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "口令无效或已过期";
            return result;
        }

        Integer courseId = initialToken.getCourseId();
        Course course = courseMapper.selectByIdForShare(courseId);
        if (course == null) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "课程不存在";
            return result;
        }

        DigitalToken activeToken = digitalTokenMapper.selectByToken(token);
        if (activeToken == null || !courseId.equals(activeToken.getCourseId())) {
            DigitalSignInResult result = new DigitalSignInResult();
            result.success = false;
            result.message = "口令无效或已过期";
            return result;
        }

        boolean signed = attendanceRecordMapper.markSignedBySession(
                tokenSessionKey(activeToken.getId()), studentId, new Date()
        ) > 0;
        DigitalSignInResult result = new DigitalSignInResult();
        result.success = signed;
        result.message = signed ? "签到成功" : "签到失败，可能已经签到或不在签到名单中";
        result.courseId = courseId;
        return result;
    }

    @Transactional
    public void expireToken(Integer courseId) {
        if (courseMapper.selectByIdForUpdate(courseId) == null) {
            return;
        }
        digitalTokenMapper.expireByCourseId(courseId);
    }

    private String tokenSessionKey(Integer tokenId) {
        return "TOKEN:" + tokenId;
    }

    public static class DigitalSignInResult {
        public boolean success;
        public String message;
        public Integer courseId;
    }
}

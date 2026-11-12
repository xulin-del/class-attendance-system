package com.attendance.service;

import com.attendance.entity.AttendanceApplication;
import com.attendance.entity.AttendanceRecord;
import com.attendance.mapper.AttendanceApplicationMapper;
import com.attendance.mapper.AttendanceRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

@Service
public class ApplicationService {
    @Autowired
    private AttendanceApplicationMapper applicationMapper;
    @Autowired
    private AttendanceRecordMapper recordMapper;

    @Transactional
    public ApplicationResult submitApplication(Integer recordId, Integer courseId, String studentId, 
            String courseName, Date absenceDate, String reason, String images) {
        AttendanceRecord lockedRecord = recordMapper.selectByIdForUpdate(recordId);
        if (lockedRecord == null
                || !courseId.equals(lockedRecord.getCourseId())
                || !studentId.equals(lockedRecord.getStudentId())) {
            return result(false, "考勤记录不存在或不属于当前学生");
        }

        AttendanceApplication existing = applicationMapper.selectByRecordId(recordId);
        if (existing != null) {
            return result(false, "该缺勤记录已提交过申请");
        }
        
        AttendanceApplication app = new AttendanceApplication();
        app.setRecordId(recordId);
        app.setCourseId(courseId);
        app.setStudentId(studentId);
        app.setCourseName(courseName);
        app.setAbsenceDate(absenceDate);
        app.setReason(reason);
        app.setImages(images);
        app.setStatus("pending");
        app.setCreateTime(new Date());
        app.setUpdateTime(new Date());
        
        if (applicationMapper.insertIfAbsent(app) == 0) {
            return result(false, "该缺勤记录已提交过申请");
        }
        return result(true, "申请提交成功");
    }

    public List<AttendanceApplication> getPendingApplications(String teacherId) {
        return applicationMapper.selectPendingByTeacherId(teacherId);
    }

    public int countPendingApplications(String teacherId) {
        return applicationMapper.countPendingByTeacherId(teacherId);
    }

    @Transactional
    public ApplicationResult approveApplication(Integer applicationId) {
        AttendanceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            return result(false, "申请不存在");
        }

        int updated = applicationMapper.updateStatusIfPending(
                applicationId, "approved", app.getRejectReason(), new Date()
        );
        if (updated == 0) {
            return result(false, "该申请已处理");
        }
        
        AttendanceRecord record = recordMapper.selectById(app.getRecordId());
        if (record != null) {
            record.setStatus("已补签");
            record.setSignTime(new Date());
            recordMapper.updateById(record);
        }
        
        return result(true, "审批通过");
    }

    @Transactional
    public ApplicationResult rejectApplication(Integer applicationId, String rejectReason) {
        AttendanceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            return result(false, "申请不存在");
        }

        int updated = applicationMapper.updateStatusIfPending(
                applicationId, "rejected", rejectReason, new Date()
        );
        if (updated == 0) {
            return result(false, "该申请已处理");
        }
        return result(true, "已拒绝申请");
    }

    private ApplicationResult result(boolean success, String message) {
        ApplicationResult result = new ApplicationResult();
        result.success = success;
        result.message = message;
        return result;
    }

    public AttendanceApplication getApplicationByRecordId(Integer recordId) {
        return applicationMapper.selectByRecordId(recordId);
    }

    public List<AttendanceApplication> getApplicationsByStudentId(String studentId) {
        return applicationMapper.selectByStudentId(studentId);
    }

    public static class ApplicationResult {
        public boolean success;
        public String message;
    }
}

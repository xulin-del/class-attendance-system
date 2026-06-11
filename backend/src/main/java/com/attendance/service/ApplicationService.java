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

    public ApplicationResult submitApplication(Integer recordId, Integer courseId, String studentId, 
            String courseName, Date absenceDate, String reason, String images) {
        AttendanceApplication existing = applicationMapper.selectByRecordId(recordId);
        if (existing != null) {
            ApplicationResult result = new ApplicationResult();
            result.success = false;
            result.message = "该缺勤记录已提交过申请";
            return result;
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
        
        applicationMapper.insert(app);
        
        ApplicationResult result = new ApplicationResult();
        result.success = true;
        result.message = "申请提交成功";
        return result;
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
            ApplicationResult result = new ApplicationResult();
            result.success = false;
            result.message = "申请不存在";
            return result;
        }
        
        if (!"pending".equals(app.getStatus())) {
            ApplicationResult result = new ApplicationResult();
            result.success = false;
            result.message = "该申请已处理";
            return result;
        }
        
        app.setStatus("approved");
        app.setUpdateTime(new Date());
        applicationMapper.updateById(app);
        
        AttendanceRecord record = recordMapper.selectById(app.getRecordId());
        if (record != null) {
            record.setStatus("已补签");
            record.setSignTime(new Date());
            recordMapper.updateById(record);
        }
        
        ApplicationResult result = new ApplicationResult();
        result.success = true;
        result.message = "审批通过";
        return result;
    }

    @Transactional
    public ApplicationResult rejectApplication(Integer applicationId, String rejectReason) {
        AttendanceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            ApplicationResult result = new ApplicationResult();
            result.success = false;
            result.message = "申请不存在";
            return result;
        }
        
        if (!"pending".equals(app.getStatus())) {
            ApplicationResult result = new ApplicationResult();
            result.success = false;
            result.message = "该申请已处理";
            return result;
        }
        
        app.setStatus("rejected");
        app.setRejectReason(rejectReason);
        app.setUpdateTime(new Date());
        applicationMapper.updateById(app);
        
        ApplicationResult result = new ApplicationResult();
        result.success = true;
        result.message = "已拒绝申请";
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

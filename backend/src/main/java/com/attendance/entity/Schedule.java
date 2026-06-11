package com.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("schedules")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer courseId;
    private String teacherId;
    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private String location;
    private Integer weekStart;
    private Integer weekEnd;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String className;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getWeekStart() { return weekStart; }
    public void setWeekStart(Integer weekStart) { this.weekStart = weekStart; }
    public Integer getWeekEnd() { return weekEnd; }
    public void setWeekEnd(Integer weekEnd) { this.weekEnd = weekEnd; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}

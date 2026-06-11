package com.attendance.mapper;

import com.attendance.entity.SignCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;

public interface SignCodeMapper extends BaseMapper<SignCode> {
    SignCode selectActiveBySignCode(@Param("signCode") String signCode);
    void expireByCourseId(@Param("courseId") Integer courseId);
    void cleanExpired(@Param("now") Date now);
}

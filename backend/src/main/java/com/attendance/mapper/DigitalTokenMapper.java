package com.attendance.mapper;

import com.attendance.entity.DigitalToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface DigitalTokenMapper extends BaseMapper<DigitalToken> {
    DigitalToken selectActiveByCourseId(@Param("courseId") Integer courseId);
    DigitalToken selectByToken(@Param("token") String token);
    void expireByCourseId(@Param("courseId") Integer courseId);
}

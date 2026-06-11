package com.attendance.mapper;

import com.attendance.entity.LocationSign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface LocationSignMapper extends BaseMapper<LocationSign> {
    LocationSign selectActiveByCourseId(@Param("courseId") Integer courseId);
    LocationSign selectBySignCode(@Param("signCode") String signCode);
    List<LocationSign> selectByCourseId(@Param("courseId") Integer courseId);
}

package com.example.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.education.pojo.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<CourseEntity> {
}

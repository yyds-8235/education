package com.example.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.education.pojo.entity.CourseStudentEntity;
import com.example.education.pojo.model.CourseSelectableStudentRow;
import com.example.education.pojo.model.CourseStudentRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseStudentMapper extends BaseMapper<CourseStudentEntity> {

    List<CourseStudentRow> selectCourseStudents(@Param("courseId") String courseId);

    List<CourseSelectableStudentRow> selectCandidateStudents(@Param("courseId") String courseId,
                                                            @Param("keyword") String keyword);
}

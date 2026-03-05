package com.example.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.model.StudentProfileRow;
import com.example.education.pojo.query.StudentQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfileEntity> {

    IPage<StudentProfileRow> selectStudentPage(Page<StudentProfileRow> page, @Param("query") StudentQuery query);

    StudentProfileRow selectStudentDetail(@Param("studentId") String studentId);

    List<String> selectExistingStudentIds(@Param("studentIds") List<String> studentIds);
}


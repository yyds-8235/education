package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseStudentBatchResultVO {

    private String courseId;
    private List<CourseStudentVO> students = new ArrayList<>();
}

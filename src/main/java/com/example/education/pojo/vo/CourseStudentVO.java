package com.example.education.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseStudentVO {

    private String id;
    private String courseId;
    private String studentId;
    private String studentName;
    private String studentNo;
    private String joinedAt;
    private BigDecimal progress;
}

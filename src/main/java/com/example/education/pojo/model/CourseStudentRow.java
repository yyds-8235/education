package com.example.education.pojo.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseStudentRow {

    private String id;
    private String courseId;
    private String studentId;
    private String studentName;
    private String studentNo;
    private LocalDateTime joinedAt;
    private BigDecimal progress;
}

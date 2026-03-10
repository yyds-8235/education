package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course_students")
public class CourseStudentEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("course_id")
    private String courseId;

    @TableField("student_id")
    private String studentId;

    @TableField("student_no")
    private String studentNo;

    @TableField("joined_at")
    private LocalDateTime joinedAt;

    @TableField("progress")
    private BigDecimal progress;
}

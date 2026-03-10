package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("courses")
public class CourseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("grade")
    private String grade;

    @TableField("class_name")
    private String className;

    @TableField("subject")
    private String subject;

    @TableField("teacher_id")
    private String teacherId;

    @TableField("visibility")
    private String visibility;

    @TableField("cover_image")
    private String coverImage;

    @TableField("student_count")
    private Integer studentCount;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

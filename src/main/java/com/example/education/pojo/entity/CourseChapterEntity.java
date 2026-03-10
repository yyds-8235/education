package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_chapters")
public class CourseChapterEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("course_id")
    private String courseId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

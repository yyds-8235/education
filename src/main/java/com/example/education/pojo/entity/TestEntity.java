package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tests")
public class TestEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("course_id")
    private String courseId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("duration")
    private Integer duration;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("show_answer")
    private Boolean showAnswer;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

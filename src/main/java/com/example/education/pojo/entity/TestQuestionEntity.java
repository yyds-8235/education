package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_questions")
public class TestQuestionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("test_id")
    private String testId;

    @TableField("type")
    private String type;

    @TableField("content")
    private String content;

    @TableField("answer")
    private String answer;

    @TableField("score")
    private Integer score;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("analysis")
    private String analysis;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

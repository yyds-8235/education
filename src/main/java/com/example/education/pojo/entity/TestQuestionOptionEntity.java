package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_question_options")
public class TestQuestionOptionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("question_id")
    private String questionId;

    @TableField("label")
    private String label;

    @TableField("content")
    private String content;

    @TableField("sort_order")
    private Integer sortOrder;
}

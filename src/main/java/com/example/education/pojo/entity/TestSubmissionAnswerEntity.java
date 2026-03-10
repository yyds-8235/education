package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_submission_answers")
public class TestSubmissionAnswerEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("submission_id")
    private String submissionId;

    @TableField("question_id")
    private String questionId;

    @TableField("answer")
    private String answer;

    @TableField("score")
    private Integer score;

    @TableField("feedback")
    private String feedback;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

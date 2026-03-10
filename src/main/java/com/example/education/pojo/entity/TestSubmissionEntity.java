package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_submissions")
public class TestSubmissionEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("test_id")
    private String testId;

    @TableField("student_id")
    private String studentId;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("status")
    private String status;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("graded_at")
    private LocalDateTime gradedAt;

    @TableField("appeal_reason")
    private String appealReason;

    @TableField("appeal_status")
    private String appealStatus;

    @TableField("analysis_summary")
    private String analysisSummary;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

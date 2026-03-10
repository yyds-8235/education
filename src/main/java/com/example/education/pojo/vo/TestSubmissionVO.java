package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSubmissionVO {

    private String id;
    private String testId;
    private String studentId;
    private String studentName;
    private String studentNo;
    private List<SubmissionAnswerVO> answers = new ArrayList<>();
    private Integer totalScore;
    private String status;
    private String submittedAt;
    private String gradedAt;
    private String appealReason;
    private String appealStatus;
    private String analysisSummary;
    private String createdAt;
}

package com.example.education.pojo.vo;

import lombok.Data;

@Data
public class SubmissionAnswerVO {

    private String questionId;
    private String answer;
    private Integer score;
    private String feedback;
    private Boolean isCorrect;
}

package com.example.education.pojo.vo;

import lombok.Data;

@Data
public class TestQuestionStatVO {

    private String questionId;
    private Double correctRate;
    private Integer wrongCount;
    private Integer correctCount;
    private Double averageScore;
}

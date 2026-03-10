package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestQuestionVO {

    private String id;
    private String testId;
    private String type;
    private String content;
    private List<TestQuestionOptionVO> options = new ArrayList<>();
    private String answer;
    private Integer score;
    private Integer order;
    private String analysis;
}

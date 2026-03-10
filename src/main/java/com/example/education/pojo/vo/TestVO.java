package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestVO {

    private String id;
    private String courseId;
    private String courseName;
    private String title;
    private String description;
    private Integer duration;
    private Integer totalScore;
    private Boolean showAnswer;
    private String status;
    private List<TestQuestionVO> questions = new ArrayList<>();
    private List<TestSubmissionVO> submissions = new ArrayList<>();
    private String createdAt;
    private String updatedAt;
}

package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestStatisticsVO {

    private String testId;
    private Integer totalSubmissions;
    private Double averageScore;
    private Integer highestScore;
    private Integer lowestScore;
    private Double passRate;
    private List<TestQuestionStatVO> questionStats = new ArrayList<>();
    private List<TestWrongDistributionVO> wrongDistribution = new ArrayList<>();
    private String learningBrief;
    private List<String> adaptiveRecommendations = new ArrayList<>();
}

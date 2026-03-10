package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestBatchGradeResultVO {

    private String testId;
    private List<TestSubmissionVO> submissions = new ArrayList<>();
}

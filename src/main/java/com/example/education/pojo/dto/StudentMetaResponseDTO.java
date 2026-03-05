package com.example.education.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentMetaResponseDTO {

    private List<String> grades;
    private List<String> classes;
    private List<String> povertyLevels;
    private List<String> householdTypes;
}


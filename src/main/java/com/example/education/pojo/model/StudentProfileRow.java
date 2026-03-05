package com.example.education.pojo.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentProfileRow {

    private String id;
    private String studentNo;
    private String name;
    private String username;
    private String grade;
    private String className;
    private String guardian;
    private LocalDateTime syncedAt;
    private String povertyLevel;
    private Boolean isSponsored;
    private String householdType;
    private Boolean isLeftBehind;
    private Boolean isDisabled;
    private Boolean isSingleParent;
    private Boolean isKeyConcern;
    private Boolean canView;
    private Boolean canEdit;
}


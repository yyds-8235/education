package com.example.education.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StudentProfileVO {

    private String id;
    private String studentNo;
    private String name;
    private String username;
    private String grade;

    @JsonProperty("class")
    private String clazz;

    private String guardian;
    private String syncedAt;
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


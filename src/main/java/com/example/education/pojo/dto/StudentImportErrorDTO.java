package com.example.education.pojo.dto;

import lombok.Data;

@Data
public class StudentImportErrorDTO {

    private Integer row;
    private String studentNo;
    private String error;
}

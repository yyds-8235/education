package com.example.education.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentImportResultDTO {

    private Integer total = 0;
    private Integer success = 0;
    private Integer failed = 0;
    private List<StudentImportErrorDTO> errors = new ArrayList<>();
}

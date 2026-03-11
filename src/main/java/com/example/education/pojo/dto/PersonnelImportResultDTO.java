package com.example.education.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonnelImportResultDTO {

    private Integer importedCount = 0;
    private Integer skippedCount = 0;
    private List<PersonnelImportFailureDTO> failedRows = new ArrayList<>();
}

package com.example.education.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelImportFailureDTO {

    private Integer rowNumber;
    private String reason;
}

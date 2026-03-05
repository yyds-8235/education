package com.example.education.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class SyncStudentsResponseDTO {

    private Integer syncedCount;
    private String syncedAt;
    private List<String> failed;
}


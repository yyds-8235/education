package com.example.education.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class SyncStudentsRequestDTO {

    private List<String> studentIds;
}


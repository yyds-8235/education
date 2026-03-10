package com.example.education.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class TestPageDataDTO<T> {

    private List<T> list;
    private Long total;
    private Long page;
    private Long pageSize;
    private Long totalPages;
}

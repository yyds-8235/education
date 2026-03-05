package com.example.education.pojo.dto;

import com.example.education.pojo.vo.StudentProfileVO;
import lombok.Data;

import java.util.List;

@Data
public class StudentPageDataDTO {

    private List<StudentProfileVO> list;
    private Long total;
    private Long page;
    private Long pageSize;
    private Long totalPages;
}


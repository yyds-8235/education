package com.example.education.pojo.dto;

import com.example.education.pojo.vo.CourseVO;
import lombok.Data;

import java.util.List;

@Data
public class CoursePageDataDTO {

    private List<CourseVO> list;
    private Long total;
    private Long page;
    private Long pageSize;
    private Long totalPages;
}

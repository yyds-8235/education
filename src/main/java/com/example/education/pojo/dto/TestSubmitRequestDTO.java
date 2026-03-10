package com.example.education.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSubmitRequestDTO {

    @Valid
    @NotEmpty(message = "answers不能为空")
    private List<TestSubmitAnswerRequestDTO> answers = new ArrayList<>();
}

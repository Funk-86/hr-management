package org.example.hrmanagement.module.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FaceEnrollDTO {
    @NotNull
    @Size(min = 128, max = 128)
    private List<@NotNull Double> descriptor;
    private Integer sampleCount = 1;  // 前端采样帧数，便于审计
}

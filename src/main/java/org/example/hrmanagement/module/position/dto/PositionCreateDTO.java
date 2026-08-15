package org.example.hrmanagement.module.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PositionCreateDTO {
    @NotBlank
    private String positionName;
    @NotBlank
    private String positionCode;
    @NotNull
    private Long deptId;
    private Integer level;   // 可选，默认 1
    private Integer status;  // 可选，默认 1
}

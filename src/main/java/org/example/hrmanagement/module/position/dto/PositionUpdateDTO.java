package org.example.hrmanagement.module.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PositionUpdateDTO {
    @NotBlank
    private String positionName;
    @NotNull
    private Long deptId;
    private Integer level;
    private Integer status;
}

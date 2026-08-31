package org.example.hrmanagement.module.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FieldWorkCreateDTO {
    @NotNull
    private LocalDate workDate;
    @NotBlank
    private String location;
    @NotBlank
    private String reason;
}

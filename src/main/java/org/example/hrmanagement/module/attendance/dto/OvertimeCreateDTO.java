package org.example.hrmanagement.module.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class OvertimeCreateDTO {
    @NotNull
    private LocalDate workDate;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotNull
    private BigDecimal hours;
    @NotBlank
    private String reason;
}

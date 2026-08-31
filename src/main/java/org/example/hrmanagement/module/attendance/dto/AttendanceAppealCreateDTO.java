package org.example.hrmanagement.module.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceAppealCreateDTO {
    @NotNull
    private LocalDate attendDate;
    private Long attendanceId;
    private Integer fromStatus;
    @NotNull
    private Integer toStatus;
    private LocalTime checkIn;
    private LocalTime checkOut;
    @NotBlank
    private String reason;
}

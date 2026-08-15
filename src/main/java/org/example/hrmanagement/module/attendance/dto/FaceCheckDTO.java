package org.example.hrmanagement.module.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FaceCheckDTO {
    @NotNull
    @Size(min = 128, max = 128)
    private List<@NotNull Double> descriptor;
    /** 仅 SUPER_ADMIN 代打卡时必传；普通员工不传 */
    private Long employeeId;
}

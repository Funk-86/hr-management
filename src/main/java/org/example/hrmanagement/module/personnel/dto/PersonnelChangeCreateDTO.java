package org.example.hrmanagement.module.personnel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PersonnelChangeCreateDTO {

    /** 1调岗 2调薪 3离职 4入职完善 */
    @NotNull(message = "请选择异动类型")
    private Integer changeType;

    @NotNull(message = "请选择员工")
    private Long employeeId;

    private Long toDeptId;
    private Long toPositionId;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String reason;
}

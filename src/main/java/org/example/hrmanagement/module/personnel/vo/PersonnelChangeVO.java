package org.example.hrmanagement.module.personnel.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PersonnelChangeVO {

    private Long id;
    private Integer changeType;
    private String changeTypeLabel;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private Long fromDeptId;
    private String fromDeptName;
    private Long toDeptId;
    private String toDeptName;
    private Long fromPositionId;
    private String fromPositionName;
    private Long toPositionId;
    private String toPositionName;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String reason;
    private Integer status;
    private String statusLabel;
    private Long applicantId;
    private String applicantName;
    private Long approverId;
    private String approverName;
    private String approveRemark;
    private LocalDateTime approvedAt;
    private LocalDateTime effectedAt;
    private LocalDateTime createdAt;
    /** 入职完善：已关联合同类文档数 */
    private Integer contractDocCount;
}

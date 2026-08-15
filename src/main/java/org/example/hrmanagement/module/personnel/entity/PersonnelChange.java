package org.example.hrmanagement.module.personnel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_personnel_change")
public class PersonnelChange extends BaseEntity {

    /** 1调岗 2调薪 3离职 4入职完善 */
    private Integer changeType;
    private Long employeeId;
    private Long fromDeptId;
    private Long toDeptId;
    private Long fromPositionId;
    private Long toPositionId;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String reason;
    /** 0待审批 1已通过 2已拒绝 3已撤销 4已生效 */
    private Integer status;
    private Long applicantId;
    private Long approverId;
    private String approveRemark;
    private LocalDateTime approvedAt;
    private LocalDateTime effectedAt;
    private Long effectedBy;
}

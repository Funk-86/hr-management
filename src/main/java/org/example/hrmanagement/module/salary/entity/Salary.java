package org.example.hrmanagement.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary")
public class Salary extends AuditableEntity {

    /** 员工ID */
    private Long employeeId;

    /** 薪资月份，如 2026-06 */
    private String salaryMonth;

    /** 岗位快照 */
    private Long positionId;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 任务评分奖金汇总 */
    private BigDecimal taskBonus;

    /** 最终奖金 */
    private BigDecimal bonus;

    /** 扣款 */
    private BigDecimal deduction;

    /** 实发工资 */
    private BigDecimal actualSalary;

    /** 状态：0-待发放 1-已发放 */
    private Integer status;

    /** 发放日期 */
    private LocalDate payDate;

    /** 备注 */
    private String remark;
}

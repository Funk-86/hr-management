package org.example.hrmanagement.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary_base_dict")
public class SalaryBaseDict extends BaseEntity {

    private Long positionId;
    private BigDecimal baseSalary;
    /** 1-启用 0-停用 */
    private Integer status;
    private String remark;
}

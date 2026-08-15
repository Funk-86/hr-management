package org.example.hrmanagement.module.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeCreateDTO {
    @NotBlank
    private String empNo;
    @NotBlank
    private String name;
    @NotNull
    private Long deptId;
    @NotNull
    private Long positionId;
    @NotNull
    private LocalDate hireDate;
    private Integer gender;
    private Integer employmentType;
    private Integer status;
    private String phone;
    private String email;
    private String idCard;
    private String avatar;
    /** 试用期结束日期 */
    private LocalDate probationEnd;
    /** 备注 */
    private String remark;
    /** 登录用户名（不可用工号替代） */
    @NotBlank(message = "登录用户名不能为空")
    private String username;
    @NotBlank(message = "初始密码不能为空")
    private String password;
    private String roleCode;
}

package org.example.hrmanagement.module.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeUpdateDTO {
    @Size(min = 1, max = 50, message = "姓名长度必须在1-50之间")
    private String name;
    private Long deptId;
    private Long positionId;
    private Integer gender;
    private Integer employmentType;
    private Integer status;
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @Email(message = "邮箱格式不正确")
    private String email;
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;
    private String avatar;
    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /**
     * 重置登录密码（可选）。仅超级管理员可传；不传则不修改密码。
     */
    @Size(min = 6, max = 64, message = "密码长度必须在6-64之间")
    private String password;
}

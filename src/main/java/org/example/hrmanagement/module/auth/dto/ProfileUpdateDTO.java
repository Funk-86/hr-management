package org.example.hrmanagement.module.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateDTO {
    @Size(max = 64, message = "用户名过长")
    private String username;

    @Size(max = 64, message = "姓名过长")
    private String realName;

    @Size(max = 20, message = "手机号过长")
    private String phone;

    @Size(max = 128, message = "邮箱过长")
    private String email;

    /** 性别：1-男 2-女 */
    private Integer gender;

    @Size(max = 500, message = "个人简介过长")
    private String introduction;
}

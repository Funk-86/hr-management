package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProfileVO {
    private String username;
    private String realName;
    private String avatar;
    private List<String> roles;
    private String phone;
    private String email;
    private Integer gender;
    /** 个人简介（对应员工备注） */
    private String introduction;
    /** 是否已关联员工档案（未关联时不可改基本信息） */
    private Boolean boundEmployee;
}

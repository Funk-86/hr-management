package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private String username;
    /** 展示名：优先关联员工姓名，否则回退登录名 */
    private String realName;
    /** 头像访问 URL */
    private String avatar;
    private List<String> roles;             // 角色编码
    private List<String> permissions;       // 权限编码
    private List<MenuVO> menus;             // 菜单树
}

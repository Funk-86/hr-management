package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private String name;      // perm_name
    private String code;      // perm_code
    private String path;      // 前端路由
    private Integer sortOrder;
    private List<MenuVO> children;
}

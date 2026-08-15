package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionNodeVO {
    private Long id;
    private Long parentId;
    private String permName;
    private String permCode;
    private Integer permType;
    private String path;
    private Integer sortOrder;
    private List<PermissionNodeVO> children = new ArrayList<>();
}

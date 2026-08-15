package org.example.hrmanagement.module.position.vo;

import lombok.Data;

@Data
public class PositionVO {
    private Long id;
    private String positionName;
    private String positionCode;
    private Long deptId;
    private String deptName;
    private Integer level;
    private Integer status;
}

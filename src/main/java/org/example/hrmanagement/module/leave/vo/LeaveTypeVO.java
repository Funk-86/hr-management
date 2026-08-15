package org.example.hrmanagement.module.leave.vo;

import lombok.Data;

@Data
public class LeaveTypeVO {

    private Long id;

    /** 类型名称（年假/病假/事假等） */
    private String typeName;

    /** 类型编码 */
    private String typeCode;

    /** 每年最大天数，NULL表示不限 */
    private Integer maxDays;

    /** 状态：0-禁用 1-启用 */
    private Integer status;
}

package org.example.hrmanagement.module.performance.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.dto.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class PerformanceQueryDTO extends PageQuery {

    private Long employeeId;
    private Long deptId;
    /** 1月度 2季度 */
    private Integer periodType;
    private String periodKey;
    /** 0草稿 1已提交 2已确认 */
    private Integer status;
}

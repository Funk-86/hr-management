package org.example.hrmanagement.module.document.dto;

import lombok.Data;
import org.example.hrmanagement.common.dto.PageQuery;

@Data
public class DocumentQueryDTO extends PageQuery {
    private Long employeeId;
    private Long deptId;
    private Long positionId;
    private Integer docType;
    private String keyword;
}

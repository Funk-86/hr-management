package org.example.hrmanagement.module.personnel.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.dto.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class PersonnelChangeQueryDTO extends PageQuery {

    private Long employeeId;
    private Integer changeType;
    private Integer status;
}

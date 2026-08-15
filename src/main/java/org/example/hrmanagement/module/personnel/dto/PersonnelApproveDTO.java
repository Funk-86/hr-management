package org.example.hrmanagement.module.personnel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonnelApproveDTO {

    /** true通过 false拒绝 */
    @NotNull(message = "请选择审批结果")
    private Boolean approved;

    private String approveRemark;
}

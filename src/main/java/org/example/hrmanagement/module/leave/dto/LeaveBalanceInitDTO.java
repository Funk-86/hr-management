package org.example.hrmanagement.module.leave.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveBalanceInitDTO {
    /** 年度，默认当年 */
    private Integer year;

    /** 是否覆盖已有额度（used/pending 保留，仅重置 quota） */
    @NotNull
    private Boolean overwriteQuota;
}

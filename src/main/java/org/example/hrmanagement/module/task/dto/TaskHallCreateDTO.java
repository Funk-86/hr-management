package org.example.hrmanagement.module.task.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskHallCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    @Size(max = 2000, message = "内容过长")
    private String content;

    private LocalDateTime dueTime;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度至少为1")
    @Max(value = 5, message = "难度最多为5")
    private Integer difficulty;

    @NotNull(message = "协作名额不能为空")
    @Min(value = 1, message = "协作人数至少为1")
    @Max(value = 20, message = "协作人数最多为20")
    private Integer claimQuota;

    @DecimalMin(value = "0.00", message = "建议奖金不能为负")
    private BigDecimal suggestBonus;

    @NotBlank(message = "逾期策略不能为空")
    private String overduePolicy;

    @DecimalMin(value = "0.01", message = "扣款金额须大于0")
    private BigDecimal deductAmount;

    /** HR 可指定部门；经理忽略，强制本部门 */
    private Long deptId;
}

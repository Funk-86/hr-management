package org.example.hrmanagement.module.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PerformanceSaveDTO {

    @NotNull(message = "请选择员工")
    private Long employeeId;

    /** 1月度 2季度 */
    @NotNull(message = "请选择周期类型")
    @Min(1)
    @Max(2)
    private Integer periodType;

    @NotBlank(message = "请填写考核周期")
    private String periodKey;

    @NotNull(message = "请评分")
    @Min(value = 1, message = "评分等级无效")
    @Max(value = 5, message = "评分等级无效")
    private Integer scoreGrade;

    private String comment;

    /** true=直接提交；false/空=存草稿 */
    private Boolean submit;
}

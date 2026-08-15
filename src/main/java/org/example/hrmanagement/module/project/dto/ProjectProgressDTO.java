package org.example.hrmanagement.module.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectProgressDTO {

    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;

    /** true=锁定后不再被任务自动覆盖；false=解锁并立即按任务重算 */
    private Boolean locked;
}

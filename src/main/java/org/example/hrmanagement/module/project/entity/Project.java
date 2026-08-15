package org.example.hrmanagement.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_project")
public class Project extends BaseEntity {

    private String name;
    private String description;
    /** 负责人（员工ID） */
    private Long ownerId;
    private Long deptId;
    /** 0规划 1进行中 2已完成 3已关闭 */
    private Integer status;
    /** 进度 0-100 */
    private Integer progress;
    /** 1=承接人已锁定/手调进度，不再自动覆盖 */
    private Integer progressLocked;
    private LocalDate startDate;
    private LocalDate endDate;
}

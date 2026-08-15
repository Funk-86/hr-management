package org.example.hrmanagement.module.project.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectVO {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Long deptId;
    private String deptName;
    /** 0规划 1进行中 2已完成 3已关闭 */
    private Integer status;
    private Integer progress;
    private Boolean progressLocked;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer taskCount;
    private LocalDateTime createdAt;
    private List<Long> memberIds;
    private List<String> memberNames;
}

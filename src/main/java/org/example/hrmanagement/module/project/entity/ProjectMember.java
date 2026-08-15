package org.example.hrmanagement.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_project_member")
public class ProjectMember extends BaseEntity {

    private Long projectId;
    private Long employeeId;
}

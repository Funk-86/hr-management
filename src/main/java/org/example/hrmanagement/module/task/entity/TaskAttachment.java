package org.example.hrmanagement.module.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_task_attachment")
public class TaskAttachment extends BaseEntity {

    private Long taskId;
    private String objectKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    /** 上传人（员工ID） */
    private Long uploaderId;
}

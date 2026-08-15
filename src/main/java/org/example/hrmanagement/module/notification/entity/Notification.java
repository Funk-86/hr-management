package org.example.hrmanagement.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class Notification extends BaseEntity {

    /** 接收人 sys_user.id */
    private Long userId;

    private String title;

    private String content;

    /** TASK_ASSIGN / TASK_URGE / TASK_REJECT */
    private String bizType;

    /** 业务主键，如任务 ID */
    private Long bizId;

    /** 前端跳转路径 */
    private String link;

    /** 0-未读 1-已读 */
    private Integer isRead;
}

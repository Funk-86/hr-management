package org.example.hrmanagement.module.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务操作日志（追加写入，无逻辑删除）。
 * action：CREATE / ASSIGN / ACCEPT / PROGRESS / FINISH / REJECT / URGE / CLOSE
 */
@Data
@TableName("hr_task_log")
public class TaskLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 操作人（员工ID） */
    private Long operatorId;

    /** 操作类型 */
    private String action;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

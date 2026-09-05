package org.example.hrmanagement.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long userId;

    /** 模块名称 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 请求参数（短摘要，兼容旧数据） */
    private String params;

    /** 请求信息（JSON，已脱敏/截断） */
    private String requestInfo;

    /** 响应信息（JSON，已脱敏/截断） */
    private String responseInfo;

    /** IP地址 */
    private String ip;

    /** 0-失败 1-成功 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

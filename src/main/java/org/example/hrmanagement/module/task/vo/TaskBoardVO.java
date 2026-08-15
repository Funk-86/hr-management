package org.example.hrmanagement.module.task.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务看板：按主任务状态分列。
 */
@Data
public class TaskBoardVO {

    /** 待接收 */
    private List<TaskVO> pending = new ArrayList<>();
    /** 进行中 */
    private List<TaskVO> inProgress = new ArrayList<>();
    /** 已完成 */
    private List<TaskVO> done = new ArrayList<>();
    /** 已关闭 */
    private List<TaskVO> closed = new ArrayList<>();
}

package org.example.hrmanagement.module.task.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.task.dto.TaskCreateDto;
import org.example.hrmanagement.module.task.dto.TaskProgressDTO;
import org.example.hrmanagement.module.task.dto.TaskRejectDTO;
import org.example.hrmanagement.module.task.dto.TaskScoreDTO;
import org.example.hrmanagement.module.task.vo.TaskBoardVO;
import org.example.hrmanagement.module.task.vo.TaskDetailVO;
import org.example.hrmanagement.module.task.vo.TaskTodoStatsVO;
import org.example.hrmanagement.module.task.vo.TaskVO;

import java.util.List;

/**
 * 任务管理：上级下发、下级接收与进度更新。
 */
public interface TaskService {

    /**
     * 创建并下发任务。
     * 校验执行人后写入任务主表、执行人表，并记录 CREATE 日志。
     */
    void create(TaskCreateDto taskCreateDto);

    /**
     * 分页查询任务列表。
     *
     * @param scope  mine=我负责的；created=我创建的
     * @param status 主任务状态，可选
     */
    PageResult<TaskVO> listTasks(String scope, Integer status, PageQuery pageQuery);

    /**
     * 当前用户接收任务（执行人状态 0→1，必要时主任务同步为进行中）。
     */
    void accept(Long taskId);

    /**
     * 更新当前用户在该任务上的进度；progress=100 时自动完成。
     *
     * @return 更新后的任务详情
     */
    TaskDetailVO progress(Long taskId, TaskProgressDTO dto);

    /**
     * 查询任务详情（含执行人列表与操作时间线）。
     * 仅创建人、执行人或 HR/超管可查看。
     */
    TaskDetailVO getDetail(Long taskId);

    /**
     * 驳回任务：仅待接收可驳回，写入驳回原因。
     */
    void reject(Long taskId, TaskRejectDTO dto);

    /**
     * 关闭任务：创建人或 HR/超管；主任务关闭，未完成执行人标为已关闭。
     */
    void close(Long taskId);

    /**
     * 催办：创建人或上级角色写 URGE 日志（通知后续对接）。
     */
    void urge(Long taskId);

    /**
     * 我的待办列表（待接收 + 进行中）。
     */
    List<TaskVO> listMyTodo();

    /**
     * 我的逾期任务列表。
     */
    List<TaskVO> listMyOverdue();

    /**
     * 工作台待办/逾期数量统计。
     */
    TaskTodoStatsVO myTodoStats();

    /**
     * 看板数据：按主任务状态分列（每列最多 50 条）。
     *
     * @param scope mine=我负责的；created=我创建的
     */
    TaskBoardVO board(String scope);

    /**
     * 对已完成执行人评分（写入等级与奖金）。
     */
    void scoreAssignee(Long taskId, Long employeeId, TaskScoreDTO dto);
}

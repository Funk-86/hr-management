package org.example.hrmanagement.module.task.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.task.dto.TaskHallAbandonDTO;
import org.example.hrmanagement.module.task.dto.TaskHallCreateDTO;
import org.example.hrmanagement.module.task.dto.TaskHallReclaimDTO;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.vo.TaskHallClaimResultVO;
import org.example.hrmanagement.module.task.vo.TaskVO;

public interface TaskHallService {

    void publish(TaskHallCreateDTO dto);

    PageResult<TaskVO> listOpen(PageQuery page);

    TaskHallClaimResultVO claim(Long taskId);

    void abandon(Long taskId, TaskHallAbandonDTO dto);

    void reclaim(Long taskId, TaskHallReclaimDTO dto);

    /** 关闭大厅任务时按逾期策略处理未完成执行人 */
    void applyClosePolicy(Task task, Long operatorEmpId, String reason);
}

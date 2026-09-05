package org.example.hrmanagement.module.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.task.dto.TaskCreateDto;
import org.example.hrmanagement.module.task.dto.TaskHallAbandonDTO;
import org.example.hrmanagement.module.task.dto.TaskHallCreateDTO;
import org.example.hrmanagement.module.task.dto.TaskHallReclaimDTO;
import org.example.hrmanagement.module.task.dto.TaskProgressDTO;
import org.example.hrmanagement.module.task.dto.TaskRejectDTO;
import org.example.hrmanagement.module.task.dto.TaskScoreDTO;
import org.example.hrmanagement.module.task.service.TaskAttachmentService;
import org.example.hrmanagement.module.task.service.TaskHallService;
import org.example.hrmanagement.module.task.service.TaskOverdueReminderService;
import org.example.hrmanagement.module.task.service.TaskService;
import org.example.hrmanagement.module.task.vo.TaskAttachmentVO;
import org.example.hrmanagement.module.task.vo.TaskBoardVO;
import org.example.hrmanagement.module.task.vo.TaskDetailVO;
import org.example.hrmanagement.module.task.vo.TaskHallClaimResultVO;
import org.example.hrmanagement.module.task.vo.TaskTodoStatsVO;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskHallService taskHallService;
    private final TaskOverdueReminderService taskOverdueReminderService;
    private final TaskAttachmentService taskAttachmentService;

    @Operation(summary = "创建并下发任务")
    @OperationLog(module = "任务管理", value = "创建任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TaskCreateDto dto) {
        taskService.create(dto);
        return Result.success();
    }

    @Operation(summary = "发布任务大厅任务")
    @OperationLog(module = "任务大厅", value = "发布大厅任务")
    @PreAuthorize("hasAuthority('feat.task.hall.publish')")
    @PostMapping("/hall")
    public Result<Void> publishHall(@Valid @RequestBody TaskHallCreateDTO dto) {
        taskHallService.publish(dto);
        return Result.success();
    }

    @Operation(summary = "任务大厅列表（本部门未满员）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/hall")
    public Result<PageResult<TaskVO>> listHall(@Valid PageQuery page) {
        return Result.success(taskHallService.listOpen(page));
    }

    @Operation(summary = "接取大厅任务")
    @OperationLog(module = "任务大厅", value = "接取任务")
    @PreAuthorize("hasAuthority('feat.task.hall.claim')")
    @PostMapping("/{id}/claim")
    public Result<TaskHallClaimResultVO> claim(@PathVariable Long id) {
        return Result.success(taskHallService.claim(id));
    }

    @Operation(summary = "放弃大厅任务名额")
    @OperationLog(module = "任务大厅", value = "放弃任务")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/{id}/abandon")
    public Result<Void> abandon(
            @PathVariable Long id,
            @Valid @RequestBody TaskHallAbandonDTO dto) {
        taskHallService.abandon(id, dto);
        return Result.success();
    }

    @Operation(summary = "强制收回大厅任务")
    @OperationLog(module = "任务大厅", value = "强制收回")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/reclaim")
    public Result<Void> reclaim(
            @PathVariable Long id,
            @Valid @RequestBody TaskHallReclaimDTO dto) {
        taskHallService.reclaim(id, dto);
        return Result.success();
    }

    @Operation(summary = "任务列表（mine=我负责的，created=我创建的）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<TaskVO>> list(
            @RequestParam(defaultValue = "mine") String scope,
            @RequestParam(required = false) Integer status,
            @Valid PageQuery page) {
        return Result.success(taskService.listTasks(scope, status, page));
    }

    @Operation(summary = "我的待办任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/my/todo")
    public Result<List<TaskVO>> myTodo() {
        return Result.success(taskService.listMyTodo());
    }

    @Operation(summary = "我的逾期任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/my/overdue")
    public Result<List<TaskVO>> myOverdue() {
        return Result.success(taskService.listMyOverdue());
    }

    @Operation(summary = "工作台待办/逾期统计")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/my/stats")
    public Result<TaskTodoStatsVO> myStats() {
        return Result.success(taskService.myTodoStats());
    }

    @Operation(summary = "任务看板（按状态分列）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/board")
    public Result<TaskBoardVO> board(@RequestParam(defaultValue = "mine") String scope) {
        return Result.success(taskService.board(scope));
    }

    @Operation(summary = "手动触发任务逾期提醒（演示/补跑）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping("/overdue-remind/run")
    public Result<Map<String, Integer>> runOverdueRemind() {
        int sent = taskOverdueReminderService.runReminder();
        return Result.success(Map.of("sent", sent));
    }

    @Operation(summary = "任务详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable Long id) {
        return Result.success(taskService.getDetail(id));
    }

    @Operation(summary = "接收任务")
    @OperationLog(module = "任务管理", value = "接收任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        taskService.accept(id);
        return Result.success();
    }

    @Operation(summary = "更新进度（100 自动完成，返回详情）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/{id}/progress")
    public Result<TaskDetailVO> progress(
            @PathVariable Long id,
            @Valid @RequestBody TaskProgressDTO dto) {
        return Result.success(taskService.progress(id, dto));
    }

    @Operation(summary = "驳回任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @Valid @RequestBody TaskRejectDTO dto) {
        taskService.reject(id, dto);
        return Result.success();
    }

    @Operation(summary = "关闭任务")
    @OperationLog(module = "任务管理", value = "关闭任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        taskService.close(id);
        return Result.success();
    }

    @Operation(summary = "催办任务")
    @OperationLog(module = "任务管理", value = "催办任务")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/urge")
    public Result<Void> urge(@PathVariable Long id) {
        taskService.urge(id);
        return Result.success();
    }

    @Operation(summary = "对已完成执行人评分")
    @OperationLog(module = "任务管理", value = "任务评分")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/assignees/{employeeId}/score")
    public Result<Void> scoreAssignee(
            @PathVariable Long id,
            @PathVariable Long employeeId,
            @Valid @RequestBody TaskScoreDTO dto) {
        taskService.scoreAssignee(id, employeeId, dto);
        return Result.success();
    }

    @Operation(summary = "任务附件列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}/attachments")
    public Result<List<TaskAttachmentVO>> listAttachments(@PathVariable Long id) {
        return Result.success(taskAttachmentService.listByTaskId(id));
    }

    @Operation(summary = "上传任务附件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/{id}/attachments")
    public Result<TaskAttachmentVO> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return Result.success(taskAttachmentService.upload(id, file));
    }

    @Operation(summary = "删除任务附件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public Result<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) {
        taskAttachmentService.delete(id, attachmentId);
        return Result.success();
    }
}

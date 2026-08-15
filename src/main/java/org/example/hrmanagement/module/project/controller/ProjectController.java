package org.example.hrmanagement.module.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.project.dto.ProjectCreateDTO;
import org.example.hrmanagement.module.project.dto.ProjectProgressDTO;
import org.example.hrmanagement.module.project.dto.ProjectUpdateDTO;
import org.example.hrmanagement.module.project.service.ProjectService;
import org.example.hrmanagement.module.project.vo.ProjectVO;
import org.example.hrmanagement.module.task.vo.TaskVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "项目管理")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询项目")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<ProjectVO>> page(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Integer status,
            PageQuery pageQuery) {
        return Result.success(projectService.page(scope, status, pageQuery));
    }

    @Operation(summary = "项目详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}")
    public Result<ProjectVO> detail(@PathVariable Long id) {
        return Result.success(projectService.getDetail(id));
    }

    @Operation(summary = "项目下根任务列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}/tasks")
    public Result<List<TaskVO>> tasks(@PathVariable Long id) {
        return Result.success(projectService.listProjectTasks(id));
    }

    @Operation(summary = "创建项目")
    @OperationLog(module = "项目管理", value = "创建项目")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProjectCreateDTO dto) {
        return Result.success(projectService.create(dto));
    }

    @Operation(summary = "更新项目")
    @OperationLog(module = "项目管理", value = "更新项目")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateDTO dto) {
        projectService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "确认/锁定项目进度")
    @OperationLog(module = "项目管理", value = "更新项目进度")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/{id}/progress")
    public Result<Void> progress(@PathVariable Long id, @Valid @RequestBody ProjectProgressDTO dto) {
        projectService.updateProgress(id, dto);
        return Result.success();
    }

    @Operation(summary = "关闭项目")
    @OperationLog(module = "项目管理", value = "关闭项目")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        projectService.close(id);
        return Result.success();
    }
}

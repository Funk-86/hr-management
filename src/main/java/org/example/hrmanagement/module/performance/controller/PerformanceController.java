package org.example.hrmanagement.module.performance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.performance.dto.PerformanceQueryDTO;
import org.example.hrmanagement.module.performance.dto.PerformanceSaveDTO;
import org.example.hrmanagement.module.performance.service.PerformanceService;
import org.example.hrmanagement.module.performance.vo.PerformanceReviewVO;
import org.example.hrmanagement.module.performance.vo.PerformanceTaskHintVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "绩效考核")
@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @Operation(summary = "分页查询考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<PerformanceReviewVO>> page(PerformanceQueryDTO query) {
        return Result.success(performanceService.page(query));
    }

    @Operation(summary = "考核单详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}")
    public Result<PerformanceReviewVO> detail(@PathVariable Long id) {
        return Result.success(performanceService.getDetail(id));
    }

    @Operation(summary = "员工档案-考核记录")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/employee/{employeeId}")
    public Result<List<PerformanceReviewVO>> byEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer limit) {
        return Result.success(performanceService.listByEmployee(employeeId, limit));
    }

    @Operation(summary = "周期内任务表现提示")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping("/task-hint")
    public Result<PerformanceTaskHintVO> taskHint(
            @RequestParam Long employeeId,
            @RequestParam Integer periodType,
            @RequestParam String periodKey) {
        return Result.success(performanceService.taskHint(employeeId, periodType, periodKey));
    }

    @Operation(summary = "创建考核单")
    @OperationLog(module = "绩效考核", value = "创建考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PerformanceSaveDTO dto) {
        return Result.success(performanceService.create(dto));
    }

    @Operation(summary = "更新考核单")
    @OperationLog(module = "绩效考核", value = "更新考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PerformanceSaveDTO dto) {
        performanceService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "提交考核单")
    @OperationLog(module = "绩效考核", value = "提交考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        performanceService.submit(id);
        return Result.success();
    }

    @Operation(summary = "确认考核单（归档）")
    @OperationLog(module = "绩效考核", value = "确认考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        performanceService.confirm(id);
        return Result.success();
    }

    @Operation(summary = "删除考核单")
    @OperationLog(module = "绩效考核", value = "删除考核单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        performanceService.delete(id);
        return Result.success();
    }
}

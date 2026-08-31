package org.example.hrmanagement.module.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.attendance.dto.AttendanceAppealCreateDTO;
import org.example.hrmanagement.module.attendance.dto.AttendanceApproveDTO;
import org.example.hrmanagement.module.attendance.dto.FieldWorkCreateDTO;
import org.example.hrmanagement.module.attendance.dto.OvertimeCreateDTO;
import org.example.hrmanagement.module.attendance.service.AttendanceAdvanceService;
import org.example.hrmanagement.module.attendance.vo.AttendanceAppealVO;
import org.example.hrmanagement.module.attendance.vo.FieldWorkRequestVO;
import org.example.hrmanagement.module.attendance.vo.OvertimeRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "考勤进阶（加班/申诉/外勤）")
@RestController
@RequestMapping("/attendance")
public class AttendanceAdvanceController {

    @Autowired
    private AttendanceAdvanceService advanceService;

    @Operation(summary = "提交加班申请")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @PostMapping("/overtime/requests")
    public Result<Void> createOvertime(@Valid @RequestBody OvertimeCreateDTO dto) {
        advanceService.createOvertime(dto);
        return Result.success();
    }

    @GetMapping("/overtime/requests")
    @PreAuthorize("hasAuthority('feat.attendance.self') or hasAuthority('feat.attendance.approve')")
    public Result<PageResult<OvertimeRequestVO>> listOvertime(@Valid PageQuery page) {
        return Result.success(advanceService.listOvertime(page));
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/overtime/requests/{id}/approve")
    public Result<Void> approveOvertime(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.approveOvertime(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/overtime/requests/{id}/reject")
    public Result<Void> rejectOvertime(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.rejectOvertime(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PutMapping("/overtime/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    public Result<Void> cancelOvertime(@PathVariable Long id) {
        advanceService.cancelOvertime(id);
        return Result.success();
    }

    @PostMapping("/appeals")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    public Result<Void> createAppeal(@Valid @RequestBody AttendanceAppealCreateDTO dto) {
        advanceService.createAppeal(dto);
        return Result.success();
    }

    @GetMapping("/appeals")
    @PreAuthorize("hasAuthority('feat.attendance.self') or hasAuthority('feat.attendance.approve')")
    public Result<PageResult<AttendanceAppealVO>> listAppeals(@Valid PageQuery page) {
        return Result.success(advanceService.listAppeals(page));
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/appeals/{id}/approve")
    public Result<Void> approveAppeal(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.approveAppeal(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/appeals/{id}/reject")
    public Result<Void> rejectAppeal(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.rejectAppeal(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PutMapping("/appeals/{id}/cancel")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    public Result<Void> cancelAppeal(@PathVariable Long id) {
        advanceService.cancelAppeal(id);
        return Result.success();
    }

    @PostMapping("/field-work/requests")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    public Result<Void> createFieldWork(@Valid @RequestBody FieldWorkCreateDTO dto) {
        advanceService.createFieldWork(dto);
        return Result.success();
    }

    @GetMapping("/field-work/requests")
    @PreAuthorize("hasAuthority('feat.attendance.self') or hasAuthority('feat.attendance.approve')")
    public Result<PageResult<FieldWorkRequestVO>> listFieldWork(@Valid PageQuery page) {
        return Result.success(advanceService.listFieldWork(page));
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/field-work/requests/{id}/approve")
    public Result<Void> approveFieldWork(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.approveFieldWork(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.approve')")
    @PutMapping("/field-work/requests/{id}/reject")
    public Result<Void> rejectFieldWork(@PathVariable Long id, @RequestBody(required = false) AttendanceApproveDTO dto) {
        advanceService.rejectFieldWork(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PutMapping("/field-work/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    public Result<Void> cancelFieldWork(@PathVariable Long id) {
        advanceService.cancelFieldWork(id);
        return Result.success();
    }
}

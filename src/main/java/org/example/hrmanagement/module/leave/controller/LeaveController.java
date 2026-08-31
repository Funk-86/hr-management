package org.example.hrmanagement.module.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.leave.dto.*;
import org.example.hrmanagement.module.leave.service.LeaveService;
import org.example.hrmanagement.module.leave.vo.LeaveBalanceVO;
import org.example.hrmanagement.module.leave.vo.LeaveRequestVO;
import org.example.hrmanagement.module.leave.vo.LeaveTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "请假管理")
@RestController()
@RequestMapping("/leave")
public class LeaveController {
    @Autowired
    private LeaveService leaveService;

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public Result<List<LeaveTypeVO>> getLeaveType(){
        return Result.success(leaveService.getLeaveType());
    }

    @Operation(summary = "我的假期余额")
    @PreAuthorize("hasAuthority('feat.leave.apply')")
    @GetMapping("/balances/mine")
    public Result<List<LeaveBalanceVO>> getMyBalances(@RequestParam(required = false) Integer year) {
        return Result.success(leaveService.getMyBalances(year));
    }

    @Operation(summary = "假期余额列表（按数据范围）")
    @PreAuthorize("hasAuthority('feat.leave.balance.manage') or hasAuthority('feat.leave.approve')")
    @GetMapping("/balances")
    public Result<List<LeaveBalanceVO>> listBalances(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year) {
        return Result.success(leaveService.listBalances(employeeId, year));
    }

    @Operation(summary = "初始化当年假期余额")
    @PreAuthorize("hasAuthority('feat.leave.balance.manage')")
    @PostMapping("/balances/init")
    public Result<Void> initBalances(@Valid @RequestBody LeaveBalanceInitDTO dto) {
        leaveService.initBalances(dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @PostMapping("/types")
    public Result<Void> insertLeaveType(@Valid @RequestBody LeaveTypeCreateDTO dto){
        leaveService.insertLeaveType(dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @PutMapping("/types/{id}")
    public Result<Void> updateLeaveType(@PathVariable Long id,@Valid @RequestBody LeaveTypeUpdateDTO dto){
        leaveService.updateLeaveType(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteLeaveType(@PathVariable Long id){
        leaveService.deleteLeaveType(id);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.leave.apply')")
    @PostMapping("/requests")
    public Result<Void> requestLeave(@Valid @RequestBody LeaveRequestCreateDTO dto){
        leaveService.requestLeave(dto);
        return Result.success();
    }

    @Operation(summary = "请假申请列表（分页）")
    @PreAuthorize("hasAuthority('feat.leave.apply') or hasAuthority('feat.leave.approve')")
    @GetMapping("/requests")
    public Result<PageResult<LeaveRequestVO>> getLeaveRequest(@Valid PageQuery page){
        return Result.success(leaveService.getLeaveRequest(page));
    }

    @PreAuthorize("hasAuthority('feat.leave.apply') or hasAuthority('feat.leave.approve')")
    @GetMapping("/requests/{id}")
    public Result<LeaveRequestVO> getLeaveRequest(@PathVariable Long id){
        return  Result.success(leaveService.getLeaveRequestById(id));
    }

    @PreAuthorize("hasAuthority('feat.leave.approve')")
    @PutMapping("/requests/{id}/approve")
    public Result<Void> approveLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.approveLeave(id,dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.leave.approve')")
    @PutMapping("/requests/{id}/reject")
    public Result<Void> rejectLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.rejectLeave(id,dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.leave.apply')")
    @PutMapping("/requests/{id}/cancel")
    public Result<Void> cancelLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.cancelLeave(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }
}

package org.example.hrmanagement.module.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.leave.dto.*;
import org.example.hrmanagement.module.leave.service.LeaveService;
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
    public Result<List<LeaveTypeVO>> getLeaveType(){
        return Result.success(leaveService.getLeaveType());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping("/types")
    public Result<Void> insertLeaveType(@Valid @RequestBody LeaveTypeCreateDTO dto){
        leaveService.insertLeaveType(dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PutMapping("/types/{id}")
    public Result<Void> updateLeaveType(@PathVariable Long id,@Valid @RequestBody LeaveTypeUpdateDTO dto){
        leaveService.updateLeaveType(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteLeaveType(@PathVariable Long id){
        leaveService.deleteLeaveType(id);
        return Result.success();
    }

    @PostMapping("/requests")
    public Result<Void> requestLeave(@Valid @RequestBody LeaveRequestCreateDTO dto){
        leaveService.requestLeave(dto);
        return Result.success();
    }

    @Operation(summary = "请假申请列表（分页）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/requests")
    public Result<PageResult<LeaveRequestVO>> getLeaveRequest(@Valid PageQuery page){
        return Result.success(leaveService.getLeaveRequest(page));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/requests/{id}")
    public Result<LeaveRequestVO> getLeaveRequest(@PathVariable Long id){
        return  Result.success(leaveService.getLeaveRequestById(id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/requests/{id}/approve")
    public Result<Void> approveLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.approveLeave(id,dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/requests/{id}/reject")
    public Result<Void> rejectLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.rejectLeave(id,dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }

    @PutMapping("/requests/{id}/cancel")
    public Result<Void> cancelLeaveRequest(@PathVariable Long id, @RequestBody LeaveApproveDTO dto){
        leaveService.cancelLeave(id, dto != null ? dto.getApproveRemark() : null);
        return Result.success();
    }
}

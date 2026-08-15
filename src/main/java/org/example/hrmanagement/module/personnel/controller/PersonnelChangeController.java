package org.example.hrmanagement.module.personnel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.personnel.dto.PersonnelApproveDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeCreateDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeQueryDTO;
import org.example.hrmanagement.module.personnel.service.PersonnelChangeService;
import org.example.hrmanagement.module.personnel.vo.PersonnelChangeVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "入转调离")
@RestController
@RequestMapping("/personnel-changes")
@RequiredArgsConstructor
public class PersonnelChangeController {

    private final PersonnelChangeService personnelChangeService;

    @Operation(summary = "分页查询异动单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<PersonnelChangeVO>> page(PersonnelChangeQueryDTO query) {
        return Result.success(personnelChangeService.page(query));
    }

    @Operation(summary = "异动单详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}")
    public Result<PersonnelChangeVO> detail(@PathVariable Long id) {
        return Result.success(personnelChangeService.getDetail(id));
    }

    @Operation(summary = "发起异动申请")
    @OperationLog(module = "入转调离", value = "发起异动申请")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PersonnelChangeCreateDTO dto) {
        return Result.success(personnelChangeService.create(dto));
    }

    @Operation(summary = "审批异动")
    @OperationLog(module = "入转调离", value = "审批异动")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody PersonnelApproveDTO dto) {
        personnelChangeService.approve(id, dto);
        return Result.success();
    }

    @Operation(summary = "撤销异动")
    @OperationLog(module = "入转调离", value = "撤销异动")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        personnelChangeService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "异动生效")
    @OperationLog(module = "入转调离", value = "异动生效")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping("/{id}/effect")
    public Result<Void> effect(@PathVariable Long id) {
        personnelChangeService.effect(id);
        return Result.success();
    }
}

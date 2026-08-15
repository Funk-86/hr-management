package org.example.hrmanagement.module.department.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.department.dto.DepartmentCreateDTO;
import org.example.hrmanagement.module.department.dto.DepartmentUpdateDTO;
import org.example.hrmanagement.module.department.service.DepartmentService;
import org.example.hrmanagement.module.department.vo.DepartmentTreeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/departments")
public class DepartmentController {
    @Autowired
    private DepartmentService departmentService;

    @Operation(summary = "部门树形列表")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping
    public Result<List<DepartmentTreeVO>> list(){
        return Result.success(departmentService.listTree());
    }

    @Operation(summary = "查询单个部门")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping ("/{id}")
    public Result<DepartmentTreeVO> getById(@PathVariable Long id){
        return Result.success(departmentService.getById(id));
    }

    @Operation(summary = "新增部门")
    @OperationLog(module = "部门管理", value = "新增部门")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DepartmentCreateDTO dto){
        departmentService.createDepartment(dto);
        return Result.success();
    }

    @Operation(summary = "修改部门")
    @OperationLog(module = "部门管理", value = "修改部门")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody DepartmentUpdateDTO dto){
        departmentService.updateDepartment(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @OperationLog(module = "部门管理", value = "删除部门")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        departmentService.deleteDepartment(id);
        return Result.success();
    }
}

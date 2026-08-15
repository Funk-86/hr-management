package org.example.hrmanagement.module.salary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.module.salary.dto.SalaryCreateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryGenerateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryUpdateDTO;
import org.example.hrmanagement.module.salary.service.SalaryService;
import org.example.hrmanagement.module.salary.vo.SalaryPreviewVO;
import org.example.hrmanagement.module.salary.vo.SalaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "薪资管理")
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired
    private SalaryService salaryService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @GetMapping()
    public Result<PageResult<SalaryVO>> getSalaryAll(@Valid PageQuery page){
        return Result.success(salaryService.getSalaryAll(page));
    }

    @Operation(summary = "按员工+月份预计算底薪与任务奖金")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping("/preview")
    public Result<SalaryPreviewVO> preview(@Valid @RequestBody SalaryGenerateDTO dto) {
        return Result.success(salaryService.previewGenerate(dto));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @GetMapping("/{id}")
    public Result<SalaryVO> getSalaryById(@PathVariable Long id){
        return Result.success(salaryService.getSalaryById(id));
    }

    @OperationLog(module = "薪资管理", value = "创建薪资")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping()
    public Result<Void> createSalary(@Valid @RequestBody SalaryCreateDTO dto){
        salaryService.createSalary(dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> updateSalary(@PathVariable Long id,@Valid @RequestBody SalaryUpdateDTO dto){
        salaryService.updateSalary(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PutMapping("/{id}/pay")
    public Result<Void> paySalary(@PathVariable Long id,@Valid @RequestParam(required = false) LocalDate payDate){
        salaryService.paySalary(id, payDate);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSalary(@PathVariable Long id){
        salaryService.deleteSalary(id);
        return Result.success();
    }
}

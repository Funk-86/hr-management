package org.example.hrmanagement.module.salary.controller;



import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.example.hrmanagement.common.annotation.OperationLog;

import org.example.hrmanagement.common.result.Result;

import org.example.hrmanagement.module.salary.dto.SalaryBaseDictSaveDTO;

import org.example.hrmanagement.module.salary.dto.TaskScoreBonusDictSaveDTO;

import org.example.hrmanagement.module.salary.service.SalaryDictService;

import org.example.hrmanagement.module.salary.vo.SalaryBaseDictVO;

import org.example.hrmanagement.module.salary.vo.TaskScoreBonusDictVO;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;



import java.util.List;



@Tag(name = "薪资字典")

@RestController

@RequestMapping("/salary")

@RequiredArgsConstructor

public class SalaryDictController {



    private final SalaryDictService salaryDictService;



    @Operation(summary = "岗位底薪字典列表")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @GetMapping("/base-dict")

    public Result<List<SalaryBaseDictVO>> listBaseDict() {

        return Result.success(salaryDictService.listBaseDict());

    }



    @Operation(summary = "新增岗位底薪")

    @OperationLog(module = "字典管理", value = "新增底薪字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @PostMapping("/base-dict")

    public Result<Void> saveBaseDict(@Valid @RequestBody SalaryBaseDictSaveDTO dto) {

        salaryDictService.saveBaseDict(dto);

        return Result.success();

    }



    @Operation(summary = "修改岗位底薪")

    @OperationLog(module = "字典管理", value = "修改底薪字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @PutMapping("/base-dict/{id}")

    public Result<Void> updateBaseDict(@PathVariable Long id, @Valid @RequestBody SalaryBaseDictSaveDTO dto) {

        salaryDictService.updateBaseDict(id, dto);

        return Result.success();

    }



    @Operation(summary = "删除岗位底薪")

    @OperationLog(module = "字典管理", value = "删除底薪字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @DeleteMapping("/base-dict/{id}")

    public Result<Void> deleteBaseDict(@PathVariable Long id) {

        salaryDictService.deleteBaseDict(id);

        return Result.success();

    }



    @Operation(summary = "任务评分奖金字典列表（评分下拉可读）")

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")

    @GetMapping("/score-bonus-dict")

    public Result<List<TaskScoreBonusDictVO>> listScoreBonusDict() {

        return Result.success(salaryDictService.listScoreBonusDict());

    }



    @Operation(summary = "新增评分奖金档")

    @OperationLog(module = "字典管理", value = "新增评分奖金字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @PostMapping("/score-bonus-dict")

    public Result<Void> saveScoreBonusDict(@Valid @RequestBody TaskScoreBonusDictSaveDTO dto) {

        salaryDictService.saveScoreBonusDict(dto);

        return Result.success();

    }



    @Operation(summary = "修改评分奖金档")

    @OperationLog(module = "字典管理", value = "修改评分奖金字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @PutMapping("/score-bonus-dict/{id}")

    public Result<Void> updateScoreBonusDict(

            @PathVariable Long id,

            @Valid @RequestBody TaskScoreBonusDictSaveDTO dto) {

        salaryDictService.updateScoreBonusDict(id, dto);

        return Result.success();

    }



    @Operation(summary = "删除评分奖金档")

    @OperationLog(module = "字典管理", value = "删除评分奖金字典")

    @PreAuthorize("hasRole('SUPER_ADMIN')")

    @DeleteMapping("/score-bonus-dict/{id}")

    public Result<Void> deleteScoreBonusDict(@PathVariable Long id) {

        salaryDictService.deleteScoreBonusDict(id);

        return Result.success();

    }

}


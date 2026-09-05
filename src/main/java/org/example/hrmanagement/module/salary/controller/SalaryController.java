package org.example.hrmanagement.module.salary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.excel.ExcelExportHelper;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.salary.dto.AttendanceDeductRuleUpdateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryCreateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryGenerateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryUpdateDTO;
import org.example.hrmanagement.module.salary.excel.SalaryMineExportRow;
import org.example.hrmanagement.module.salary.service.SalaryService;
import org.example.hrmanagement.module.salary.vo.AttendanceDeductRuleVO;
import org.example.hrmanagement.module.salary.vo.SalaryPreviewVO;
import org.example.hrmanagement.module.salary.vo.SalaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "薪资管理")
@RestController
@RequestMapping("/salary")
public class SalaryController {
    @Autowired
    private SalaryService salaryService;

    @Operation(summary = "我的已发放薪资条")
    @PreAuthorize("hasAuthority('feat.salary.self')")
    @GetMapping("/mine")
    public Result<PageResult<SalaryVO>> getMySalaries(
            @Valid PageQuery page,
            @RequestParam(required = false) String salaryMonth) {
        return Result.success(salaryService.getMyPaidSalaries(page, salaryMonth));
    }

    @Operation(summary = "我的薪资条详情")
    @PreAuthorize("hasAuthority('feat.salary.self')")
    @GetMapping("/mine/{id}")
    public Result<SalaryVO> getMySalaryById(@PathVariable Long id) {
        return Result.success(salaryService.getMySalaryById(id));
    }

    @Operation(summary = "导出我的薪资条 Excel")
    @OperationLog(module = "薪资", value = "导出我的薪资条")
    @PreAuthorize("hasAuthority('feat.salary.self')")
    @GetMapping("/mine/export")
    public void exportMySalaries(
            HttpServletResponse response,
            @RequestParam(required = false) String salaryMonth) {
        List<SalaryVO> list = salaryService.listMyPaidForExport(salaryMonth);
        List<SalaryMineExportRow> rows = list.stream().map(this::toMineExportRow).toList();
        ExcelExportHelper.write(response, "我的薪资条.xlsx", "薪资", SalaryMineExportRow.class, rows);
    }

    private SalaryMineExportRow toMineExportRow(SalaryVO vo) {
        SalaryMineExportRow row = new SalaryMineExportRow();
        row.setSalaryMonth(vo.getSalaryMonth());
        row.setEmpNo(vo.getEmpNo());
        row.setEmployeeName(vo.getEmployeeName());
        row.setPositionName(vo.getPositionName());
        row.setBaseSalary(vo.getBaseSalary());
        row.setTaskBonus(vo.getTaskBonus());
        row.setBonus(vo.getBonus());
        row.setDeduction(vo.getDeduction());
        row.setActualSalary(vo.getActualSalary());
        row.setPayDate(vo.getPayDate() == null ? null : vo.getPayDate().toString());
        row.setRemark(vo.getRemark());
        return row;
    }

    @Operation(summary = "考勤扣款规则列表")
    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @GetMapping("/deduct-rules")
    public Result<List<AttendanceDeductRuleVO>> listDeductRules() {
        return Result.success(salaryService.listDeductRules());
    }

    @Operation(summary = "更新考勤扣款规则")
    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @PutMapping("/deduct-rules/{id}")
    public Result<Void> updateDeductRule(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceDeductRuleUpdateDTO dto) {
        salaryService.updateDeductRule(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @GetMapping()
    public Result<PageResult<SalaryVO>> getSalaryAll(@Valid PageQuery page){
        return Result.success(salaryService.getSalaryAll(page));
    }

    @Operation(summary = "按员工+月份预计算底薪与任务奖金")
    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @PostMapping("/preview")
    public Result<SalaryPreviewVO> preview(@Valid @RequestBody SalaryGenerateDTO dto) {
        return Result.success(salaryService.previewGenerate(dto));
    }

    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @GetMapping("/{id}")
    public Result<SalaryVO> getSalaryById(@PathVariable Long id){
        return Result.success(salaryService.getSalaryById(id));
    }

    @OperationLog(module = "薪资管理", value = "创建薪资")
    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @PostMapping()
    public Result<Void> createSalary(@Valid @RequestBody SalaryCreateDTO dto){
        salaryService.createSalary(dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @PutMapping("/{id}")
    public Result<Void> updateSalary(@PathVariable Long id,@Valid @RequestBody SalaryUpdateDTO dto){
        salaryService.updateSalary(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @PutMapping("/{id}/pay")
    public Result<Void> paySalary(@PathVariable Long id,@Valid @RequestParam(required = false) LocalDate payDate){
        salaryService.paySalary(id, payDate);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.salary.manage')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSalary(@PathVariable Long id){
        salaryService.deleteSalary(id);
        return Result.success();
    }
}

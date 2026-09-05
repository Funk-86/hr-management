package org.example.hrmanagement.module.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.excel.ExcelExportHelper;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.employee.dto.EmployeeCreateDTO;
import org.example.hrmanagement.module.employee.dto.EmployeeUpdateDTO;
import org.example.hrmanagement.module.employee.excel.EmployeeExportRow;
import org.example.hrmanagement.module.employee.service.EmployeeService;
import org.example.hrmanagement.module.employee.service.ProbationReminderService;
import org.example.hrmanagement.module.employee.vo.EmployeeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "员工管理")
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private ProbationReminderService probationReminderService;

    @Operation(summary = "员工列表（分页）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<EmployeeVO>> getAllEmployees(
            @Valid PageQuery page,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(employeeService.list(page, deptId, status, keyword));
    }

    @Operation(summary = "导出员工 Excel")
    @OperationLog(module = "员工管理", value = "导出员工")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping("/export")
    public void exportEmployees(
            HttpServletResponse response,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        List<EmployeeVO> list = employeeService.listForExport(deptId, status, keyword);
        List<EmployeeExportRow> rows = list.stream().map(this::toExportRow).toList();
        ExcelExportHelper.write(response, "员工花名册.xlsx", "员工", EmployeeExportRow.class, rows);
    }

    @Operation(summary = "立即执行试用期到期提醒（演示/补跑）")
    @OperationLog(module = "员工管理", value = "试用期提醒")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping("/probation-remind/run")
    public Result<Map<String, Integer>> runProbationRemind() {
        int sent = probationReminderService.runReminder();
        return Result.success(Map.of("sent", sent));
    }

    @Operation(summary = "查询员工详情")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}")
    public Result<EmployeeVO> getEmployee(@PathVariable Long id) {
        EmployeeVO employeeVO=employeeService.findById(id);
        return Result.success(employeeVO);
    }

    private EmployeeExportRow toExportRow(EmployeeVO vo) {
        EmployeeExportRow row = new EmployeeExportRow();
        row.setEmpNo(vo.getEmpNo());
        row.setName(vo.getName());
        row.setDeptName(vo.getDeptName());
        row.setPositionName(vo.getPositionName());
        row.setGender(vo.getGender() == null ? "" : (vo.getGender() == 1 ? "男" : "女"));
        row.setPhone(vo.getPhone());
        row.setEmail(vo.getEmail());
        row.setStatus(switch (vo.getStatus() == null ? 0 : vo.getStatus()) {
            case 1 -> "在职";
            case 2 -> "试用期";
            case 3 -> "离职";
            default -> "";
        });
        row.setHireDate(vo.getHireDate() == null ? "" : vo.getHireDate().toString());
        return row;
    }

    @Operation(summary = "新增员工")
    @OperationLog(module = "员工管理", value = "新增员工")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PostMapping
    public Result<Void> createEmployee(@Valid @RequestBody EmployeeCreateDTO dto) {
        employeeService.insert(dto);
        return Result.success();
    }

    @Operation(summary = "修改员工")
    @OperationLog(module = "员工管理", value = "修改员工")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @PutMapping("/{id}")
    public Result<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateDTO dto) {
        employeeService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除员工")
    @OperationLog(module = "员工管理", value = "删除员工")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.success(null);
    }

}

package org.example.hrmanagement.module.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.excel.ExcelExportHelper;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.attendance.dto.AttendanceCheckDTO;
import org.example.hrmanagement.module.attendance.dto.FaceCheckDTO;
import org.example.hrmanagement.module.attendance.excel.AttendanceExportRow;
import org.example.hrmanagement.module.attendance.service.AttendanceService;
import org.example.hrmanagement.module.attendance.vo.AttendanceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "考勤管理")
@RestController
@RequestMapping("/attendance")
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @PostMapping("/check-in")
    public Result<Void> checkIn(@Valid @RequestBody AttendanceCheckDTO dto){
        attendanceService.checkIn(dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @PostMapping("/check-out")
    public Result<Void> checkOut(@Valid @RequestBody AttendanceCheckDTO dto){
        attendanceService.checkOut(dto);
        return Result.success();
    }

    @Operation(summary = "考勤列表（分页）")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @GetMapping
    public Result<PageResult<AttendanceVO>> getAll(@Valid PageQuery page){
        return Result.success(attendanceService.getAll(page));
    }

    @Operation(summary = "导出考勤 Excel")
    @OperationLog(module = "考勤管理", value = "导出考勤")
    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<AttendanceVO> list = attendanceService.listForExport();
        List<AttendanceExportRow> rows = list.stream().map(this::toExportRow).toList();
        ExcelExportHelper.write(response, "考勤台账.xlsx", "考勤", AttendanceExportRow.class, rows);
    }

    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @GetMapping("/{id}")
    public Result<AttendanceVO> getOne(@PathVariable Long id){
        return Result.success(attendanceService.getOne(id));
    }

    private AttendanceExportRow toExportRow(AttendanceVO vo) {
        AttendanceExportRow row = new AttendanceExportRow();
        row.setAttendDate(vo.getAttendDate() == null ? "" : vo.getAttendDate().toString());
        row.setEmpNo(vo.getEmpNo());
        row.setEmployeeName(vo.getEmployeeName());
        row.setCheckIn(vo.getCheckIn() == null ? "" : vo.getCheckIn().toString());
        row.setCheckOut(vo.getCheckOut() == null ? "" : vo.getCheckOut().toString());
        row.setStatus(switch (vo.getStatus() == null ? 0 : vo.getStatus()) {
            case 1 -> "正常";
            case 2 -> "迟到";
            case 3 -> "早退";
            case 4 -> "缺勤";
            case 5 -> "请假";
            case 6 -> "外勤";
            default -> "";
        });
        row.setWorkHours(vo.getWorkHours() == null ? "" : vo.getWorkHours().toPlainString());
        row.setRemark(vo.getRemark());
        return row;
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @PostMapping
    public Result<Void> create(@RequestBody AttendanceVO vo){
        attendanceService.create(vo);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AttendanceVO vo){
        attendanceService.update(id, vo);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.org.manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        attendanceService.delete(id);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @PostMapping("/check-in/face")
    public Result<Void> checkInByFace(@Valid @RequestBody FaceCheckDTO dto) {
        attendanceService.checkInByFace(dto);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('feat.attendance.self')")
    @PostMapping("/check-out/face")
    public Result<Void> checkOutByFace(@Valid @RequestBody FaceCheckDTO dto) {
        attendanceService.checkOutByFace(dto);
        return Result.success();
    }

}

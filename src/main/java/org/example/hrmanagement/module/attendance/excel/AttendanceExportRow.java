package org.example.hrmanagement.module.attendance.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class AttendanceExportRow {

    @ExcelProperty("日期")
    private String attendDate;

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String employeeName;

    @ExcelProperty("上班")
    private String checkIn;

    @ExcelProperty("下班")
    private String checkOut;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("工时")
    private String workHours;

    @ExcelProperty("备注")
    private String remark;
}

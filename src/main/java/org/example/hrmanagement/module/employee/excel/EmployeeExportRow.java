package org.example.hrmanagement.module.employee.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class EmployeeExportRow {

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("岗位")
    private String positionName;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("手机")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("入职日期")
    private String hireDate;
}

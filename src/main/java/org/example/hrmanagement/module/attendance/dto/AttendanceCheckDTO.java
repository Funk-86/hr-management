package org.example.hrmanagement.module.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceCheckDTO {
    /** 员工ID；普通员工可不传，由后端从登录上下文取 */
    private Long employeeId;

    private String employeeName;

    /** 考勤日期 */
    private LocalDate attendDate;

    /** 上班打卡时间 */
    private LocalTime checkIn;

    /** 下班打卡时间 */
    private LocalTime checkOut;

    /** 状态：1-正常 2-迟到 3-早退 4-缺勤 5-请假 */
    private Integer status;

    /** 工作时长（小时） */
    private BigDecimal workHours;

    /** 备注 */
    private String remark;
}

package org.example.hrmanagement.module.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance")
public class Attendance extends AuditableEntity {

    /** 员工ID */
    private Long employeeId;

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

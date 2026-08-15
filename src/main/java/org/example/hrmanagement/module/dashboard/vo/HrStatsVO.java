package org.example.hrmanagement.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class HrStatsVO {

    private String yearMonth;

    /** 在职人数 */
    private Long totalEmployees;

    /** 人力分布（一级部门） */
    private Map<String, Long> departmentDistribution = new LinkedHashMap<>();

    /** 本月考勤异常总数（迟到+早退+缺勤） */
    private Long attendanceAbnormalCount;

    /** 本月考勤异常明细：迟到/早退/缺勤 */
    private Map<String, Long> attendanceAbnormal = new LinkedHashMap<>();

    /** 本月应发合计 */
    private BigDecimal salaryTotalAmount;

    /** 本月已发放笔数 */
    private Long salaryPaidCount;

    /** 本月待发放笔数 */
    private Long salaryPendingCount;

    /** 任务完成率 0-100（根任务 status=2 / 非关闭根任务） */
    private Integer taskCompletionRate;

    private Long taskDoneCount;
    private Long taskTotalCount;

    /** 项目完成率 0-100（status=2 / 非关闭项目） */
    private Integer projectCompletionRate;

    private Long projectDoneCount;
    private Long projectTotalCount;
}

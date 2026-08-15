package org.example.hrmanagement.module.dashboard.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    /** 年份，如 2026 */
    private Integer year;
    /** 月份，1-12 */
    private Integer month;
    /** 当月天数，dayStatus 的长度 */
    private Integer totalDays;
    /**
     * 下标 0 = 1 号，下标 i = i+1 号。
     * 0=无记录，1-5 与 hr_attendance.status 一致。
     */
    private List<Integer> dayStatus;
    /** 有打卡记录的天数 */
    private Integer punchDays;
    /** status=1 的天数 */
    private Integer normalCount;
    /** status=2 的天数 */
    private Integer lateCount;

    // === 仪表盘汇总指标 ===
    /** 在职员工总数 */
    private Long totalEmployees;
    /** 本月新入职员工数 */
    private Long newHiresThisMonth;
    /** 待审批事项数（请假申请） */
    private Long pendingApprovals;
    /** 部门人数分布：key=部门名称, value=人数 */
    private Map<String, Long> departmentDistribution;
    /** 当前用户所在部门在职人数（工作台「团队」） */
    private Long teamCount;
    /** 项目数（预留，后续项目模块接入） */
    private Long projectCount;
}

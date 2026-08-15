package org.example.hrmanagement.module.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.dashboard.dto.DashboardDTO;
import org.example.hrmanagement.module.dashboard.service.DashboardService;
import org.example.hrmanagement.module.dashboard.vo.DashboardVO;
import org.example.hrmanagement.module.dashboard.vo.HrStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "获取仪表盘数据（含考勤日历）")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/calendar")
    public Result<DashboardVO> getCalendar(
            @RequestParam(required = false) String yearMonth) {
        DashboardDTO dto = new DashboardDTO();
        dto.setYearMonth(yearMonth);
        return Result.success(dashboardService.getDashboard(dto));
    }

    @Operation(summary = "HR 统计看板")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @GetMapping("/hr-stats")
    public Result<HrStatsVO> hrStats(@RequestParam(required = false) String yearMonth) {
        return Result.success(dashboardService.getHrStats(yearMonth));
    }
}

package org.example.hrmanagement.module.dashboard.service;

import org.example.hrmanagement.module.dashboard.dto.DashboardDTO;
import org.example.hrmanagement.module.dashboard.vo.DashboardVO;
import org.example.hrmanagement.module.dashboard.vo.HrStatsVO;

public interface DashboardService {
    DashboardVO getDashboard(DashboardDTO dto);

    /** HR 统计看板 */
    HrStatsVO getHrStats(String yearMonth);
}

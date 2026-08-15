package org.example.hrmanagement.module.dashboard.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    /** 格式 yyyy-MM，如 2026-06；不传则由 Service 默认当前月 */
    private String yearMonth;
}

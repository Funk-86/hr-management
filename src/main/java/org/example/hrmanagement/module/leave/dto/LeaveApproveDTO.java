package org.example.hrmanagement.module.leave.dto;

import lombok.Data;

@Data
public class LeaveApproveDTO {
    private String approveRemark;  // 可选，不传就是 null
}

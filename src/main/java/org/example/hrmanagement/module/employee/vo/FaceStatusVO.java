package org.example.hrmanagement.module.employee.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FaceStatusVO {
    private boolean enrolled;
    private LocalDateTime enrolledAt;
    private String modelVersion;
}

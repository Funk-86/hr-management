package org.example.hrmanagement.module.document.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeDocumentVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    private Integer docType;
    private String docTypeLabel;
    private String title;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    /** 是否 30 天内到期 */
    private Boolean expiringSoon;
    private String remark;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime createdAt;
}

package org.example.hrmanagement.module.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_document")
public class EmployeeDocument extends BaseEntity {

    private Long employeeId;
    /** 1劳动合同 2保密协议 3薪资确认单 4其他 */
    private Integer docType;
    private String title;
    private String objectKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String remark;
    private Long uploaderId;
}

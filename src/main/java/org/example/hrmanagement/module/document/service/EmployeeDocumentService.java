package org.example.hrmanagement.module.document.service;

import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.document.dto.DocumentQueryDTO;
import org.example.hrmanagement.module.document.vo.DocumentFilePayload;
import org.example.hrmanagement.module.document.vo.EmployeeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface EmployeeDocumentService {

    PageResult<EmployeeDocumentVO> page(DocumentQueryDTO query);

    EmployeeDocumentVO upload(
            Long employeeId,
            Integer docType,
            String title,
            LocalDate effectiveDate,
            LocalDate expireDate,
            String remark,
            MultipartFile file);

    void delete(Long id);

    /** 按权限读取文件流，供预览/下载 */
    DocumentFilePayload openFile(Long id);
}

package org.example.hrmanagement.module.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.document.dto.DocumentQueryDTO;
import org.example.hrmanagement.module.document.service.EmployeeDocumentService;
import org.example.hrmanagement.module.document.vo.DocumentFilePayload;
import org.example.hrmanagement.module.document.vo.EmployeeDocumentVO;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@Tag(name = "员工文档")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class EmployeeDocumentController {

    private final EmployeeDocumentService employeeDocumentService;

    @Operation(summary = "分页查询员工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping
    public Result<PageResult<EmployeeDocumentVO>> page(DocumentQueryDTO query) {
        return Result.success(employeeDocumentService.page(query));
    }

    @Operation(summary = "上传员工文档")
    @OperationLog(module = "文档管理", value = "上传员工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping
    public Result<EmployeeDocumentVO> upload(
            @RequestParam Long employeeId,
            @RequestParam(required = false) Integer docType,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expireDate,
            @RequestParam(required = false) String remark,
            @RequestParam("file") MultipartFile file) {
        return Result.success(employeeDocumentService.upload(
                employeeId, docType, title, effectiveDate, expireDate, remark, file));
    }

    @Operation(summary = "删除员工文档")
    @OperationLog(module = "文档管理", value = "删除员工文档")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeDocumentService.delete(id);
        return Result.success();
    }

    @Operation(summary = "预览/下载员工文档文件")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @GetMapping("/{id}/file")
    public ResponseEntity<InputStreamResource> file(
            @PathVariable Long id,
            @RequestParam(defaultValue = "inline") String disposition) {
        DocumentFilePayload payload = employeeDocumentService.openFile(id);
        boolean attachment = "attachment".equalsIgnoreCase(disposition);
        String fileName = StringUtils.hasText(payload.getFileName()) ? payload.getFileName() : "document";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        // 预览时尽量给浏览器可识别的类型
        if (!attachment && mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        if (payload.getContentLength() >= 0) {
            headers.setContentLength(payload.getContentLength());
        }
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                (attachment ? "attachment" : "inline") + "; filename*=UTF-8''" + encoded);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(payload.getInputStream()));
    }
}

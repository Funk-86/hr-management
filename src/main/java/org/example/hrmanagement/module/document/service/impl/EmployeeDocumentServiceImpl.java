package org.example.hrmanagement.module.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.oss.OssProperties;
import org.example.hrmanagement.common.oss.OssService;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.document.constant.DocumentType;
import org.example.hrmanagement.module.document.dto.DocumentQueryDTO;
import org.example.hrmanagement.module.document.entity.EmployeeDocument;
import org.example.hrmanagement.module.document.mapper.EmployeeDocumentMapper;
import org.example.hrmanagement.module.document.service.EmployeeDocumentService;
import org.example.hrmanagement.module.document.vo.DocumentFilePayload;
import org.example.hrmanagement.module.document.vo.EmployeeDocumentVO;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private static final int EXPIRING_DAYS = 30;

    private final EmployeeDocumentMapper documentMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final OssService ossService;
    private final OssProperties ossProperties;

    @Override
    public PageResult<EmployeeDocumentVO> page(DocumentQueryDTO query) {
        Set<Long> scopedEmployeeIds = resolveScopedEmployeeIds(query);
        if (scopedEmployeeIds != null && scopedEmployeeIds.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<EmployeeDocument> wrapper = new LambdaQueryWrapper<EmployeeDocument>()
                .in(scopedEmployeeIds != null, EmployeeDocument::getEmployeeId, scopedEmployeeIds)
                .eq(query.getEmployeeId() != null, EmployeeDocument::getEmployeeId, query.getEmployeeId())
                .eq(query.getDocType() != null, EmployeeDocument::getDocType, query.getDocType())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(EmployeeDocument::getTitle, query.getKeyword())
                        .or()
                        .like(EmployeeDocument::getFileName, query.getKeyword())
                        .or()
                        .like(EmployeeDocument::getRemark, query.getKeyword()))
                .orderByDesc(EmployeeDocument::getCreatedAt);

        IPage<EmployeeDocument> iPage = documentMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<EmployeeDocument> records = iPage.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.empty();
        }

        List<EmployeeDocumentVO> vos = toVos(records);
        PageResult<EmployeeDocumentVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeDocumentVO upload(
            Long employeeId,
            Integer docType,
            String title,
            LocalDate effectiveDate,
            LocalDate expireDate,
            String remark,
            MultipartFile file) {
        if (employeeId == null) {
            throw new BusinessException("请选择员工");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManage(employee);

        int type = docType == null ? DocumentType.OTHER : docType;
        if (!DocumentType.valid(type)) {
            throw new BusinessException("文档类型不正确");
        }
        validateFile(file);

        String extension = resolveExtension(file);
        String objectKey = ossService.buildObjectKey(
                ossProperties.getDocument().getPrefix(), employeeId, extension);
        try {
            ossService.upload(file.getInputStream(), file.getSize(), file.getContentType(), objectKey);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployeeId(employeeId);
        doc.setDocType(type);
        doc.setTitle(StringUtils.hasText(title) ? title.trim() : file.getOriginalFilename());
        doc.setObjectKey(objectKey);
        doc.setFileName(resolveFileName(file));
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setEffectiveDate(effectiveDate);
        doc.setExpireDate(expireDate);
        doc.setRemark(remark);
        doc.setUploaderId(SecurityUtil.getEmployeeId());
        documentMapper.insert(doc);

        return toVos(List.of(doc)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        EmployeeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        Employee employee = employeeMapper.selectById(doc.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManage(employee);
        String objectKey = doc.getObjectKey();
        documentMapper.deleteById(id);
        ossService.deleteIfExists(objectKey);
    }

    @Override
    public DocumentFilePayload openFile(Long id) {
        EmployeeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        Employee employee = employeeMapper.selectById(doc.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManage(employee);

        // 先完整读入内存再关闭 OSS 连接，避免预览/下载第二次出现 Connection reset
        try (OSSObject ossObject = ossService.getObject(doc.getObjectKey())) {
            ObjectMetadata meta = ossObject.getObjectMetadata();
            String contentType = StringUtils.hasText(doc.getContentType())
                    ? doc.getContentType()
                    : (meta.getContentType() == null ? "application/octet-stream" : meta.getContentType());
            String fileName = StringUtils.hasText(doc.getFileName()) ? doc.getFileName() : "document";
            byte[] bytes = ossObject.getObjectContent().readAllBytes();
            return new DocumentFilePayload(
                    fileName,
                    contentType,
                    bytes.length,
                    new java.io.ByteArrayInputStream(bytes));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败，请稍后重试");
        }
    }

    /**
     * null = 不限制员工集合（HR 看全部）；空集合 = 无权限数据。
     */
    private Set<Long> resolveScopedEmployeeIds(DocumentQueryDTO query) {
        if (SecurityUtil.isHrStaff()) {
            return filterEmployees(query, null);
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER")) {
            Long deptId = SecurityUtil.requireDeptId();
            Long filterDept = query.getDeptId() != null ? query.getDeptId() : deptId;
            if (!Objects.equals(filterDept, deptId)) {
                return Set.of();
            }
            return filterEmployees(query, deptId);
        }
        Long myId = SecurityUtil.getEmployeeId();
        if (myId == null) {
            return Set.of();
        }
        if (query.getEmployeeId() != null && !Objects.equals(query.getEmployeeId(), myId)) {
            return Set.of();
        }
        return Set.of(myId);
    }

    private Set<Long> filterEmployees(DocumentQueryDTO query, Long forceDeptId) {
        if (forceDeptId == null
                && query.getDeptId() == null
                && query.getPositionId() == null
                && query.getEmployeeId() == null) {
            return null;
        }
        LambdaQueryWrapper<Employee> ew = new LambdaQueryWrapper<Employee>()
                .eq(forceDeptId != null, Employee::getDeptId, forceDeptId)
                .eq(forceDeptId == null && query.getDeptId() != null, Employee::getDeptId, query.getDeptId())
                .eq(query.getPositionId() != null, Employee::getPositionId, query.getPositionId())
                .eq(query.getEmployeeId() != null, Employee::getId, query.getEmployeeId())
                .select(Employee::getId);
        List<Employee> list = employeeMapper.selectList(ew);
        return list.stream().map(Employee::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertCanManage(Employee employee) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER")) {
            Long deptId = SecurityUtil.requireDeptId();
            if (employee.getDeptId() != null && employee.getDeptId().equals(deptId)) {
                return;
            }
        }
        Long myId = SecurityUtil.getEmployeeId();
        if (myId != null && myId.equals(employee.getId())) {
            return;
        }
        throw new BusinessException("无权管理该员工文档");
    }

    private List<EmployeeDocumentVO> toVos(List<EmployeeDocument> docs) {
        Set<Long> empIds = docs.stream().map(EmployeeDocument::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> uploaderIds = docs.stream().map(EmployeeDocument::getUploaderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> allEmpIds = new HashSet<>(empIds);
        allEmpIds.addAll(uploaderIds);

        Map<Long, Employee> empMap = allEmpIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(allEmpIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        Set<Long> deptIds = empMap.values().stream().map(Employee::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> positionIds = empMap.values().stream().map(Employee::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> deptNames = deptIds.isEmpty()
                ? Map.of()
                : departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        Map<Long, String> positionNames = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));

        LocalDate soon = LocalDate.now().plusDays(EXPIRING_DAYS);
        List<EmployeeDocumentVO> list = new ArrayList<>();
        for (EmployeeDocument doc : docs) {
            Employee emp = empMap.get(doc.getEmployeeId());
            EmployeeDocumentVO vo = new EmployeeDocumentVO();
            vo.setId(doc.getId());
            vo.setEmployeeId(doc.getEmployeeId());
            if (emp != null) {
                vo.setEmployeeName(emp.getName());
                vo.setEmpNo(emp.getEmpNo());
                vo.setDeptId(emp.getDeptId());
                vo.setDeptName(emp.getDeptId() == null ? null : deptNames.get(emp.getDeptId()));
                vo.setPositionId(emp.getPositionId());
                vo.setPositionName(emp.getPositionId() == null ? null : positionNames.get(emp.getPositionId()));
            }
            vo.setDocType(doc.getDocType());
            vo.setDocTypeLabel(DocumentType.label(doc.getDocType()));
            vo.setTitle(doc.getTitle());
            vo.setFileName(doc.getFileName());
            vo.setContentType(doc.getContentType());
            vo.setFileSize(doc.getFileSize());
            vo.setUrl(ossService.toPublicUrl(doc.getObjectKey()));
            vo.setEffectiveDate(doc.getEffectiveDate());
            vo.setExpireDate(doc.getExpireDate());
            vo.setExpiringSoon(doc.getExpireDate() != null
                    && !doc.getExpireDate().isBefore(LocalDate.now())
                    && !doc.getExpireDate().isAfter(soon));
            vo.setRemark(doc.getRemark());
            vo.setUploaderId(doc.getUploaderId());
            Employee uploader = doc.getUploaderId() == null ? null : empMap.get(doc.getUploaderId());
            vo.setUploaderName(uploader == null ? null : uploader.getName());
            vo.setCreatedAt(doc.getCreatedAt());
            list.add(vo);
        }
        return list;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        OssProperties.Document conf = ossProperties.getDocument();
        if (file.getSize() > conf.getMaxSize()) {
            throw new BusinessException("文件不能超过 " + (conf.getMaxSize() / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> allowed = Arrays.stream(conf.getAllowedTypes().split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean okType = allowed.contains(contentType)
                || name.endsWith(".pdf")
                || name.endsWith(".doc")
                || name.endsWith(".docx");
        if (!okType) {
            throw new BusinessException("仅支持 PDF / Word 文档");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (StringUtils.hasText(name) && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (".pdf".equals(ext) || ".doc".equals(ext) || ".docx".equals(ext)) {
                return ext;
            }
        }
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (ct.contains("pdf")) {
            return ".pdf";
        }
        if (ct.contains("wordprocessingml")) {
            return ".docx";
        }
        if (ct.contains("msword")) {
            return ".doc";
        }
        throw new BusinessException("无法识别文件格式");
    }

    private String resolveFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return StringUtils.hasText(name) ? name : "document";
    }
}

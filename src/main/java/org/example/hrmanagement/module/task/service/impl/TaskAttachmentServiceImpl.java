package org.example.hrmanagement.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.oss.OssProperties;
import org.example.hrmanagement.common.oss.OssService;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.task.entity.Task;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.entity.TaskAttachment;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskAttachmentMapper;
import org.example.hrmanagement.module.task.mapper.TaskMapper;
import org.example.hrmanagement.module.task.service.TaskAttachmentService;
import org.example.hrmanagement.module.task.vo.TaskAttachmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final EmployeeMapper employeeMapper;
    private final OssService ossService;
    private final OssProperties ossProperties;

    @Override
    public List<TaskAttachmentVO> listByTaskId(Long taskId) {
        assertCanAccess(taskId);
        return listAttachmentsUnchecked(taskId);
    }

    @Override
    public List<TaskAttachmentVO> listAttachmentsUnchecked(Long taskId) {
        return loadVos(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskAttachmentVO upload(Long taskId, MultipartFile file) {
        assertCanAccess(taskId);
        validateFile(file);

        Long uploaderId = SecurityUtil.requireEmployeeId();
        String extension = resolveExtension(file);
        String prefix = ossProperties.getAttachment().getPrefix();
        String objectKey = ossService.buildObjectKey(prefix, taskId, extension);

        try {
            ossService.upload(file.getInputStream(), file.getSize(), file.getContentType(), objectKey);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }

        TaskAttachment entity = new TaskAttachment();
        entity.setTaskId(taskId);
        entity.setObjectKey(objectKey);
        entity.setFileName(resolveFileName(file));
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploaderId(uploaderId);
        taskAttachmentMapper.insert(entity);

        TaskAttachmentVO vo = toVo(entity);
        Employee uploader = employeeMapper.selectById(uploaderId);
        if (uploader != null) {
            vo.setUploaderName(uploader.getName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long taskId, Long attachmentId) {
        assertCanAccess(taskId);
        TaskAttachment attachment = taskAttachmentMapper.selectById(attachmentId);
        if (attachment == null || !Objects.equals(attachment.getTaskId(), taskId)) {
            throw new BusinessException("附件不存在");
        }
        assertCanDelete(attachment);
        String objectKey = attachment.getObjectKey();
        taskAttachmentMapper.deleteById(attachmentId);
        ossService.deleteIfExists(objectKey);
    }

    /**
     * 经理/HR/超管/任务创建人可删任意附件；普通执行人仅可删除自己刚上传的附件。
     */
    private void assertCanDelete(TaskAttachment attachment) {
        if (SecurityUtil.isHrStaff() || SecurityUtil.isManagerUp()) {
            return;
        }
        Long myId = SecurityUtil.requireEmployeeId();
        Task task = taskMapper.selectById(attachment.getTaskId());
        if (task != null && Objects.equals(task.getCreatorId(), myId)) {
            return;
        }
        if (Objects.equals(attachment.getUploaderId(), myId)) {
            return;
        }
        throw new BusinessException("无权删除该附件，请联系部门经理处理");
    }

    List<TaskAttachmentVO> loadVos(Long taskId) {
        List<TaskAttachment> list = taskAttachmentMapper.selectList(
                new LambdaQueryWrapper<TaskAttachment>()
                        .eq(TaskAttachment::getTaskId, taskId)
                        .orderByDesc(TaskAttachment::getCreatedAt));
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> uploaderIds = list.stream()
                .map(TaskAttachment::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = uploaderIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(uploaderIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));
        return list.stream().map(a -> {
            TaskAttachmentVO vo = toVo(a);
            vo.setUploaderName(names.get(a.getUploaderId()));
            return vo;
        }).toList();
    }

    private TaskAttachmentVO toVo(TaskAttachment a) {
        TaskAttachmentVO vo = new TaskAttachmentVO();
        vo.setId(a.getId());
        vo.setTaskId(a.getTaskId());
        vo.setFileName(a.getFileName());
        vo.setContentType(a.getContentType());
        vo.setFileSize(a.getFileSize());
        vo.setUploaderId(a.getUploaderId());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUrl(ossService.toPublicUrl(a.getObjectKey()));
        return vo;
    }

    private void assertCanAccess(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("该任务不存在");
        }
        Long myId = SecurityUtil.requireEmployeeId();
        if (SecurityUtil.isHrStaff() || Objects.equals(task.getCreatorId(), myId)) {
            return;
        }
        Long count = taskAssigneeMapper.selectCount(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getTaskId, taskId)
                        .eq(TaskAssignee::getEmployeeId, myId));
        if (count == null || count == 0) {
            throw new BusinessException("无权操作该任务附件");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        OssProperties.Attachment cfg = ossProperties.getAttachment();
        if (file.getSize() > cfg.getMaxSize()) {
            throw new BusinessException("附件大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        Set<String> allowed = Arrays.stream(cfg.getAllowedTypes().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!StringUtils.hasText(contentType) || !allowed.contains(contentType)) {
            throw new BusinessException("仅支持 JPG、PNG、WEBP、PDF、DOC、DOCX 格式");
        }
        resolveExtension(file);
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        if ("application/pdf".equals(contentType)) {
            return ".pdf";
        }
        if ("application/msword".equals(contentType)) {
            return ".doc";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return ".docx";
        }
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx").contains(ext)) {
                return ".jpeg".equals(ext) ? ".jpg" : ext;
            }
        }
        throw new BusinessException("无法识别附件文件格式");
    }

    private String resolveFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name)) {
            return "attachment" + resolveExtension(file);
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}

package org.example.hrmanagement.module.task.service;

import org.example.hrmanagement.module.task.vo.TaskAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskAttachmentService {

    List<TaskAttachmentVO> listByTaskId(Long taskId);

    /** 详情组装用：调用方需已完成权限校验 */
    List<TaskAttachmentVO> listAttachmentsUnchecked(Long taskId);

    TaskAttachmentVO upload(Long taskId, MultipartFile file);

    void delete(Long taskId, Long attachmentId);
}

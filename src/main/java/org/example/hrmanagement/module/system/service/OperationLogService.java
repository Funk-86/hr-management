package org.example.hrmanagement.module.system.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.system.vo.OperationLogVO;

import java.time.LocalDateTime;

public interface OperationLogService {

    PageResult<OperationLogVO> page(
            String module,
            Integer status,
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            PageQuery pageQuery);

    OperationLogVO getById(Long id);
}

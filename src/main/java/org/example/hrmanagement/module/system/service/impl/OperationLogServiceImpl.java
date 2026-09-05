package org.example.hrmanagement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.ResultCode;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.system.entity.OperationLog;
import org.example.hrmanagement.module.system.mapper.OperationLogMapper;
import org.example.hrmanagement.module.system.service.OperationLogService;
import org.example.hrmanagement.module.system.vo.OperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<OperationLogVO> page(
            String module,
            Integer status,
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            PageQuery pageQuery) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        // 列表不查大字段，避免分页膨胀
        wrapper.select(
                OperationLog::getId,
                OperationLog::getUserId,
                OperationLog::getModule,
                OperationLog::getOperation,
                OperationLog::getMethod,
                OperationLog::getIp,
                OperationLog::getStatus,
                OperationLog::getErrorMsg,
                OperationLog::getDuration,
                OperationLog::getCreatedAt
        );
        if (StringUtils.hasText(module)) {
            wrapper.like(OperationLog::getModule, module.trim());
        }
        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);

        IPage<OperationLog> iPage = operationLogMapper.selectPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), wrapper);
        List<OperationLog> records = iPage.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, String> usernameMap = loadUsernameMap(records);

        List<OperationLogVO> vos = records.stream()
                .map(log -> toVo(log, usernameMap, false))
                .toList();

        PageResult<OperationLogVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public OperationLogVO getById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<Long, String> usernameMap = log.getUserId() == null
                ? Map.of()
                : loadUsernameMap(List.of(log));
        return toVo(log, usernameMap, true);
    }

    private Map<Long, String> loadUsernameMap(List<OperationLog> records) {
        Set<Long> userIds = records.stream()
                .map(OperationLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private OperationLogVO toVo(OperationLog log, Map<Long, String> usernameMap, boolean detail) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(usernameMap.get(log.getUserId()));
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setMethod(log.getMethod());
        vo.setIp(log.getIp());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setDuration(log.getDuration());
        vo.setCreatedAt(log.getCreatedAt());
        if (detail) {
            vo.setParams(log.getParams());
            String requestInfo = log.getRequestInfo();
            if (!StringUtils.hasText(requestInfo) && StringUtils.hasText(log.getParams())) {
                requestInfo = log.getParams();
            }
            vo.setRequestInfo(requestInfo);
            vo.setResponseInfo(log.getResponseInfo());
        }
        return vo;
    }
}

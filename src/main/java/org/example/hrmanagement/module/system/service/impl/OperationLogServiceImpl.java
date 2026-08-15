package org.example.hrmanagement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
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

        Set<Long> userIds = records.stream()
                .map(OperationLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<OperationLogVO> vos = records.stream().map(log -> {
            OperationLogVO vo = new OperationLogVO();
            vo.setId(log.getId());
            vo.setUserId(log.getUserId());
            vo.setUsername(usernameMap.get(log.getUserId()));
            vo.setModule(log.getModule());
            vo.setOperation(log.getOperation());
            vo.setMethod(log.getMethod());
            vo.setParams(log.getParams());
            vo.setIp(log.getIp());
            vo.setStatus(log.getStatus());
            vo.setErrorMsg(log.getErrorMsg());
            vo.setDuration(log.getDuration());
            vo.setCreatedAt(log.getCreatedAt());
            return vo;
        }).toList();

        PageResult<OperationLogVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }
}

package org.example.hrmanagement.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.service.UserSettingService;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.example.hrmanagement.module.notification.entity.Notification;
import org.example.hrmanagement.module.notification.mapper.NotificationMapper;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.notification.sse.NotificationSseHub;
import org.example.hrmanagement.module.notification.vo.NotificationVO;
import org.example.hrmanagement.module.notification.vo.UnreadPushVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final NotificationSseHub notificationSseHub;
    private final UserSettingService userSettingService;

    @Override
    public void sendToUsers(Collection<Long> userIds, String title, String content,
                            String bizType, Long bizId, String link) {
        if (CollectionUtils.isEmpty(userIds) || !StringUtils.hasText(title) || !StringUtils.hasText(bizType)) {
            return;
        }
        Set<Long> uniqueIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return;
        }
        String resolvedLink = StringUtils.hasText(link) ? link : NotificationBizType.DEFAULT_TASK_LINK;
        Long senderId = null;
        try {
            senderId = SecurityUtil.getUserId();
        } catch (Exception ignored) {
            // 无登录上下文时仍可发送系统消息
        }
        for (Long userId : uniqueIds) {
            if (!userSettingService.allowsNotification(userId, bizType)) {
                log.debug("用户 {} 已关闭业务类型 {} 的消息提醒，跳过", userId, bizType);
                continue;
            }
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setBizType(bizType);
            n.setBizId(bizId);
            n.setLink(resolvedLink);
            n.setIsRead(0);
            if (senderId != null) {
                n.setCreatedBy(senderId);
            }
            notificationMapper.insert(n);
            pushUnread(userId, toVO(n));
        }
    }

    @Override
    public void sendToEmployees(Collection<Long> employeeIds, String title, String content,
                                String bizType, Long bizId, String link) {
        if (CollectionUtils.isEmpty(employeeIds)) {
            return;
        }
        Set<Long> empIds = employeeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (empIds.isEmpty()) {
            return;
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .in(User::getEmployeeId, empIds)
                        .eq(User::getStatus, 1));
        List<Long> userIds = users.stream().map(User::getId).filter(Objects::nonNull).toList();
        if (userIds.isEmpty()) {
            log.warn("站内消息跳过：员工 {} 均无关联登录账号", empIds);
            return;
        }
        Set<Long> foundEmpIds = users.stream()
                .map(User::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long empId : empIds) {
            if (!foundEmpIds.contains(empId)) {
                log.warn("站内消息跳过：员工ID {} 未关联登录账号", empId);
            }
        }
        sendToUsers(userIds, title, content, bizType, bizId, link);
    }

    @Override
    public PageResult<NotificationVO> listMine(Boolean onlyUnread, PageQuery pageQuery) {
        Long userId = SecurityUtil.getUserId();
        int pageNum = pageQuery.getPageNum() == null ? 1 : pageQuery.getPageNum();
        int pageSize = pageQuery.getPageSize() == null ? 20 : pageQuery.getPageSize();

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Boolean.TRUE.equals(onlyUnread), Notification::getIsRead, 0)
                .orderByDesc(Notification::getCreatedAt);

        IPage<Notification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        IPage<NotificationVO> voPage = page.convert(this::toVO);
        return PageResult.of(voPage);
    }

    @Override
    public long unreadCount() {
        Long userId = SecurityUtil.getUserId();
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Long id) {
        if (id == null) {
            throw new BusinessException("消息ID不能为空");
        }
        Long userId = SecurityUtil.getUserId();
        Notification n = notificationMapper.selectById(id);
        if (n == null || !Objects.equals(n.getUserId(), userId)) {
            throw new BusinessException("消息不存在");
        }
        if (n.getIsRead() != null && n.getIsRead() == 1) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    @Override
    public void markAllRead() {
        Long userId = SecurityUtil.getUserId();
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    @Override
    public SseEmitter subscribeStream() {
        Long userId = SecurityUtil.getUserId();
        return notificationSseHub.subscribe(userId);
    }

    private void pushUnread(Long userId, NotificationVO latest) {
        try {
            Long count = notificationMapper.selectCount(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, userId)
                            .eq(Notification::getIsRead, 0));
            UnreadPushVO push = new UnreadPushVO();
            push.setUnreadCount(count == null ? 0L : count);
            push.setLatest(latest);
            notificationSseHub.pushUnread(userId, push);
        } catch (Exception e) {
            log.warn("SSE 推送失败 userId={}", userId, e);
        }
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setBizType(n.getBizType());
        vo.setBizId(n.getBizId());
        vo.setLink(n.getLink());
        vo.setIsRead(n.getIsRead());
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}

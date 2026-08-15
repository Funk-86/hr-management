package org.example.hrmanagement.module.notification.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.notification.vo.NotificationVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;

public interface NotificationService {

    void sendToUsers(Collection<Long> userIds, String title, String content,
                     String bizType, Long bizId, String link);

    void sendToEmployees(Collection<Long> employeeIds, String title, String content,
                         String bizType, Long bizId, String link);

    PageResult<NotificationVO> listMine(Boolean onlyUnread, PageQuery pageQuery);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();

    /** 建立当前登录用户的 SSE 连接 */
    SseEmitter subscribeStream();
}

package org.example.hrmanagement.module.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.notification.vo.NotificationVO;
import org.example.hrmanagement.module.notification.vo.StreamTicketVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "站内消息")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的消息列表")
    @GetMapping
    public Result<PageResult<NotificationVO>> list(
            @RequestParam(required = false) Boolean onlyUnread,
            @Valid PageQuery page) {
        return Result.success(notificationService.listMine(onlyUnread, page));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.success(Map.of("count", notificationService.unreadCount()));
    }

    @Operation(summary = "SSE 连接 ticket（需 Authorization Bearer）")
    @PostMapping("/stream-ticket")
    public Result<StreamTicketVO> streamTicket() {
        return Result.success(notificationService.createStreamTicket());
    }

    @Operation(summary = "SSE 未读推送（EventSource 使用 ?ticket= 一次性凭证）")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        return notificationService.subscribeStream(ticket);
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }
}

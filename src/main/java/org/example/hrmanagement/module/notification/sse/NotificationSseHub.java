package org.example.hrmanagement.module.notification.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.module.notification.vo.UnreadPushVO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单机内存维护 userId -> SSE 连接；发送站内消息后向在线用户推送 unread 事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseHub {

    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000;
    private static final long TICKET_TTL_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, TicketEntry> tickets = new ConcurrentHashMap<>();

    private record TicketEntry(Long userId, long expiresAtMs) {}

    /** 签发一次性 SSE 连接 ticket */
    public String createTicket(Long userId) {
        purgeExpiredTickets();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        tickets.put(ticket, new TicketEntry(userId, System.currentTimeMillis() + TICKET_TTL_MS));
        return ticket;
    }

    /** 消费 ticket 并返回 userId；无效或过期返回 null */
    public Long consumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        purgeExpiredTickets();
        TicketEntry entry = tickets.remove(ticket.trim());
        if (entry == null || entry.expiresAtMs() < System.currentTimeMillis()) {
            return null;
        }
        return entry.userId();
    }

    public int ticketTtlSeconds() {
        return (int) (TICKET_TTL_MS / 1000);
    }

    private void purgeExpiredTickets() {
        long now = System.currentTimeMillis();
        tickets.entrySet().removeIf(e -> e.getValue().expiresAtMs() < now);
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    public void pushUnread(Long userId, UnreadPushVO payload) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("SSE 序列化失败 userId={}", userId, e);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unread")
                        .data(json));
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    /** 心跳，防止代理/浏览器空闲断连 */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId, list);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // ignore
        }
    }
}

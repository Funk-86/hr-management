package org.example.hrmanagement.module.notification.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationStreamTicketTest {

    private NotificationSseHub hub;

    @BeforeEach
    void setUp() {
        hub = new NotificationSseHub(new ObjectMapper());
    }

    @Test
    void createAndConsumeTicketOnce() {
        String ticket = hub.createTicket(42L);
        assertEquals(42L, hub.consumeTicket(ticket));
        assertNull(hub.consumeTicket(ticket));
    }

    @Test
    void invalidTicketReturnsNull() {
        assertNull(hub.consumeTicket("not-a-valid-ticket"));
        assertNull(hub.consumeTicket(null));
    }
}

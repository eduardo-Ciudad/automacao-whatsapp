package com.eduar.automacaozap.domain.model;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OutboundMessage {

    private final Long id;
    private final UUID conversationId;
    private final Long messageId;
    private final String toWhatsappNumber;
    private final OutboundMessageType type;
    private final JsonNode payload;
    private final OutboundStatus status;
    private final int attempts;
    private final String lastError;
    private final String metaMessageId;
    private final Instant createdAt;
    private final Instant sentAt;
}

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
public class Message {

    private final Long id;
    private final UUID conversationId;
    private final Direction direction;
    private final String metaMessageId;
    private final String contentType;
    private final String body;
    private final JsonNode rawPayload;
    private final Instant createdAt;
}

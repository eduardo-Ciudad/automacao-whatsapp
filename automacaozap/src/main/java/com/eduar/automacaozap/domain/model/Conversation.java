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
public class Conversation {

    private final UUID id;
    private final UUID contactId;
    private final UUID flowId;
    private final String currentStepId;
    private final JsonNode context;
    private final ConversationStatus status;
    private final Instant lastInboundAt;
    private final String lastMetaMessageId;
    private final Long companyId;
    private final Instant createdAt;
    private final Instant updatedAt;
}

package com.eduar.automacaozap.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Lead {

    private final UUID id;
    private final UUID contactId;
    private final UUID conversationId;
    private final String interest;
    private final String description;
    private final LeadStatus status;
    private final Long companyId;
    private final Instant createdAt;
    private final Instant updatedAt;
}

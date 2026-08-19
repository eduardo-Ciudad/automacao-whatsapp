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
public class Flow {

    private final UUID id;
    private final String name;
    private final int version;
    private final JsonNode definition;
    private final boolean active;
    private final Long companyId;
    private final Instant createdAt;
    private final Instant updatedAt;
}

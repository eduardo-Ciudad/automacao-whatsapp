package com.eduar.automacaozap.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Contact {

    private final UUID id;
    private final String whatsappNumber;
    private final String name;
    private final Long companyId;
    private final Instant createdAt;
    private final Instant updatedAt;
}

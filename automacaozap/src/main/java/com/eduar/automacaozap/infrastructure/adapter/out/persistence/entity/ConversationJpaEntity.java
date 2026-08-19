package com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.eduar.automacaozap.domain.model.ConversationStatus;
import tools.jackson.databind.JsonNode;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversation")
public class ConversationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "flow_id")
    private UUID flowId;

    @Column(name = "current_step_id")
    private String currentStepId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", nullable = false)
    private JsonNode context;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConversationStatus status;

    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "last_meta_message_id")
    private String lastMetaMessageId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

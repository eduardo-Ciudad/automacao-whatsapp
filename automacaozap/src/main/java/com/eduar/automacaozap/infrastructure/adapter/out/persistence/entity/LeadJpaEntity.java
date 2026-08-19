package com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.eduar.automacaozap.domain.model.LeadStatus;

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
@Table(name = "lead")
public class LeadJpaEntity {

    @Id
    private UUID id;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "interest")
    private String interest;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeadStatus status;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

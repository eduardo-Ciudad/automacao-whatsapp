package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.LeadRepositoryPort;
import com.eduar.automacaozap.domain.model.Lead;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.LeadJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.LeadJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeadRepositoryAdapter implements LeadRepositoryPort {

    private final LeadJpaRepository leadJpaRepository;

    @Override
    public Lead save(Lead lead) {
        LeadJpaEntity saved = leadJpaRepository.save(toEntity(lead));
        return toDomain(saved);
    }

    private Lead toDomain(LeadJpaEntity entity) {
        return Lead.builder()
                .id(entity.getId())
                .contactId(entity.getContactId())
                .conversationId(entity.getConversationId())
                .interest(entity.getInterest())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .companyId(entity.getCompanyId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private LeadJpaEntity toEntity(Lead lead) {
        return LeadJpaEntity.builder()
                .id(lead.getId())
                .contactId(lead.getContactId())
                .conversationId(lead.getConversationId())
                .interest(lead.getInterest())
                .description(lead.getDescription())
                .status(lead.getStatus())
                .companyId(lead.getCompanyId())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}

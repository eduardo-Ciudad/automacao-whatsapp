package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.ConversationRepositoryPort;
import com.eduar.automacaozap.domain.model.Conversation;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.ConversationJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.ConversationJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConversationRepositoryAdapter implements ConversationRepositoryPort {

    private final ConversationJpaRepository conversationJpaRepository;

    @Override
    public Optional<Conversation> findByIdForUpdate(UUID id) {
        return conversationJpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<Conversation> findByContactId(UUID contactId) {
        return conversationJpaRepository.findByContactId(contactId).map(this::toDomain);
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity saved = conversationJpaRepository.save(toEntity(conversation));
        return toDomain(saved);
    }

    private Conversation toDomain(ConversationJpaEntity entity) {
        return Conversation.builder()
                .id(entity.getId())
                .contactId(entity.getContactId())
                .flowId(entity.getFlowId())
                .currentStepId(entity.getCurrentStepId())
                .context(entity.getContext())
                .status(entity.getStatus())
                .lastInboundAt(entity.getLastInboundAt())
                .lastMetaMessageId(entity.getLastMetaMessageId())
                .companyId(entity.getCompanyId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ConversationJpaEntity toEntity(Conversation conversation) {
        return ConversationJpaEntity.builder()
                .id(conversation.getId())
                .contactId(conversation.getContactId())
                .flowId(conversation.getFlowId())
                .currentStepId(conversation.getCurrentStepId())
                .context(conversation.getContext())
                .status(conversation.getStatus())
                .lastInboundAt(conversation.getLastInboundAt())
                .lastMetaMessageId(conversation.getLastMetaMessageId())
                .companyId(conversation.getCompanyId())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}

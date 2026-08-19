package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.MessageRepositoryPort;
import com.eduar.automacaozap.domain.model.Message;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.MessageJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepositoryPort {

    private final MessageJpaRepository messageJpaRepository;

    @Override
    public boolean existsByMetaMessageId(String metaMessageId) {
        return messageJpaRepository.existsByMetaMessageId(metaMessageId);
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity saved = messageJpaRepository.save(toEntity(message));
        return toDomain(saved);
    }

    private Message toDomain(MessageJpaEntity entity) {
        return Message.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .direction(entity.getDirection())
                .metaMessageId(entity.getMetaMessageId())
                .contentType(entity.getContentType())
                .body(entity.getBody())
                .rawPayload(entity.getRawPayload())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private MessageJpaEntity toEntity(Message message) {
        return MessageJpaEntity.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .direction(message.getDirection())
                .metaMessageId(message.getMetaMessageId())
                .contentType(message.getContentType())
                .body(message.getBody())
                .rawPayload(message.getRawPayload())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

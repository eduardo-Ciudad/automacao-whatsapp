package com.eduar.automacaozap.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.eduar.automacaozap.application.port.out.OutboundMessageRepositoryPort;
import com.eduar.automacaozap.domain.model.OutboundMessage;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.OutboundMessageJpaEntity;
import com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository.OutboundMessageJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboundMessageRepositoryAdapter implements OutboundMessageRepositoryPort {

    private final OutboundMessageJpaRepository outboundMessageJpaRepository;

    @Override
    public OutboundMessage save(OutboundMessage outboundMessage) {
        OutboundMessageJpaEntity saved = outboundMessageJpaRepository.save(toEntity(outboundMessage));
        return toDomain(saved);
    }

    @Override
    public List<OutboundMessage> findPendingBatch(int limit) {
        return outboundMessageJpaRepository.findPendingBatch(PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OutboundMessage toDomain(OutboundMessageJpaEntity entity) {
        return OutboundMessage.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .messageId(entity.getMessageId())
                .toWhatsappNumber(entity.getToWhatsappNumber())
                .type(entity.getType())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .metaMessageId(entity.getMetaMessageId())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }

    private OutboundMessageJpaEntity toEntity(OutboundMessage outboundMessage) {
        return OutboundMessageJpaEntity.builder()
                .id(outboundMessage.getId())
                .conversationId(outboundMessage.getConversationId())
                .messageId(outboundMessage.getMessageId())
                .toWhatsappNumber(outboundMessage.getToWhatsappNumber())
                .type(outboundMessage.getType())
                .payload(outboundMessage.getPayload())
                .status(outboundMessage.getStatus())
                .attempts(outboundMessage.getAttempts())
                .lastError(outboundMessage.getLastError())
                .metaMessageId(outboundMessage.getMetaMessageId())
                .createdAt(outboundMessage.getCreatedAt())
                .sentAt(outboundMessage.getSentAt())
                .build();
    }
}

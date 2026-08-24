package com.eduar.automacaozap.infrastructure.adapter.out.whatsapp;


import com.eduar.automacaozap.application.port.out.OutboundMessageRepositoryPort;
import com.eduar.automacaozap.application.port.out.WhatsAppSenderPort;
import com.eduar.automacaozap.domain.model.OutboundMessage;
import com.eduar.automacaozap.domain.model.OutboundStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OutboundMessageSender {


    private static final int MAX_ATTEMPTS = 3;

    private final OutboundMessageRepositoryPort outboundMessageRepositoryPort;
    private final WhatsAppSenderPort whatsAppSenderPort;

    public OutboundMessageSender(
            OutboundMessageRepositoryPort outboundMessageRepositoryPort,
            WhatsAppSenderPort whatsAppSenderPort
    ) {
        this.outboundMessageRepositoryPort = outboundMessageRepositoryPort;
        this.whatsAppSenderPort = whatsAppSenderPort;
    }

    @Transactional
    public void sendOne(OutboundMessage message) {
        try {
            String metaMessageId = whatsAppSenderPort.send(message);

            OutboundMessage sent = OutboundMessage.builder()
                    .id(message.getId())
                    .conversationId(message.getConversationId())
                    .messageId(message.getMessageId())
                    .toWhatsappNumber(message.getToWhatsappNumber())
                    .type(message.getType())
                    .payload(message.getPayload())
                    .status(OutboundStatus.SENT)
                    .attempts(message.getAttempts())
                    .lastError(null)
                    .metaMessageId(metaMessageId)
                    .createdAt(message.getCreatedAt())
                    .sentAt(Instant.now())
                    .build();

            outboundMessageRepositoryPort.save(sent);

        } catch (Exception e) {
            int attempts = message.getAttempts() + 1;
            boolean exhausted = attempts >= MAX_ATTEMPTS;

            OutboundMessage failed = OutboundMessage.builder()
                    .id(message.getId())
                    .conversationId(message.getConversationId())
                    .messageId(message.getMessageId())
                    .toWhatsappNumber(message.getToWhatsappNumber())
                    .type(message.getType())
                    .payload(message.getPayload())
                    .status(exhausted ? OutboundStatus.FAILED : OutboundStatus.PENDING)
                    .attempts(attempts)
                    .lastError(e.getMessage())
                    .metaMessageId(message.getMetaMessageId())
                    .createdAt(message.getCreatedAt())
                    .sentAt(message.getSentAt())
                    .build();

            outboundMessageRepositoryPort.save(failed);
        }
    }
}

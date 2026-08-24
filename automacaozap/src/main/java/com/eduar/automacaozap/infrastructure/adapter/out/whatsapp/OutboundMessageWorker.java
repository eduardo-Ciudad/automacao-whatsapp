package com.eduar.automacaozap.infrastructure.adapter.out.whatsapp;


import com.eduar.automacaozap.application.port.out.OutboundMessageRepositoryPort;
import com.eduar.automacaozap.application.port.out.WhatsAppSenderPort;
import com.eduar.automacaozap.domain.model.OutboundMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboundMessageWorker {

    private final OutboundMessageRepositoryPort outboundMessageRepositoryPort;
    private final WhatsAppSenderPort whatsAppSenderPort;

    public OutboundMessageWorker(
            OutboundMessageRepositoryPort outboundMessageRepositoryPort,
            WhatsAppSenderPort whatsAppSenderPort
    ) {
        this.outboundMessageRepositoryPort = outboundMessageRepositoryPort;
        this.whatsAppSenderPort = whatsAppSenderPort;
    }

    @Scheduled(fixedDelay = 5000)
    void processPendingMessages() {
        List<OutboundMessage> batch = outboundMessageRepositoryPort.findPendingBatch(20);

        for (OutboundMessage message : batch) {
            try {
                String metaMessageId = whatsAppSenderPort.send(message);

            } catch (Exception e) {

            }
        }
    }
}

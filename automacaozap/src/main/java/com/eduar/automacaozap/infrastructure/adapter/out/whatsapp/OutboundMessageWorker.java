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
    private final OutboundMessageSender outboundMessageSender;

    public OutboundMessageWorker(
            OutboundMessageRepositoryPort outboundMessageRepositoryPort,
            OutboundMessageSender outboundMessageSender
    ) {
        this.outboundMessageRepositoryPort = outboundMessageRepositoryPort;
        this.outboundMessageSender = outboundMessageSender;
    }

    @Scheduled(fixedDelay = 5000)
    void processPendingMessages() {
        List<OutboundMessage> batch = outboundMessageRepositoryPort.findPendingBatch(20);

        for (OutboundMessage message : batch) {
            try {
                outboundMessageSender.sendOne(message);
            } catch (Exception e) {
                System.err.println("Falha inesperada ao processar OutboundMessage id=" + message.getId() + ": " + e.getMessage());
            }
        }
    }
}

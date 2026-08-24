package com.eduar.automacaozap.infrastructure.adapter.out.whatsapp;


import com.eduar.automacaozap.application.port.out.OutboundMessageRepositoryPort;
import com.eduar.automacaozap.application.port.out.WhatsAppSenderPort;
import org.springframework.stereotype.Component;

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
}

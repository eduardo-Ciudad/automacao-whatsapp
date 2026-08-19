package com.eduar.automacaozap.application.port.out;

import com.eduar.automacaozap.domain.model.OutboundMessage;

public interface WhatsAppSenderPort {

    String send(OutboundMessage message);
}

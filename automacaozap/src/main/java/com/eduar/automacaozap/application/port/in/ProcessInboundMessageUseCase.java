package com.eduar.automacaozap.application.port.in;

public interface ProcessInboundMessageUseCase {

    void handle(InboundMessageCommand command);
}

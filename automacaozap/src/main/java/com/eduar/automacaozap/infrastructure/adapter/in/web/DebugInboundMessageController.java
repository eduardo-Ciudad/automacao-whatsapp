package com.eduar.automacaozap.infrastructure.adapter.in.web;

import com.eduar.automacaozap.application.port.in.InboundMessageCommand;
import com.eduar.automacaozap.application.port.in.ProcessInboundMessageUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.UUID;

/**
 * TODO: controller temporário para testar o fluxo de processamento sem
 * depender do webhook real da Meta. Remover (ou proteger atrás de um
 * profile "debug") antes de qualquer deploy.
 */
@RestController
@RequestMapping("/debug")
public class DebugInboundMessageController {

    private final ProcessInboundMessageUseCase processInboundMessageUseCase;

    public DebugInboundMessageController(ProcessInboundMessageUseCase processInboundMessageUseCase) {
        this.processInboundMessageUseCase = processInboundMessageUseCase;
    }

    @PostMapping("/inbound-message")
    public ResponseEntity<Void> simulate(@RequestBody DebugInboundMessageRequest request) {
        InboundMessageCommand command = new InboundMessageCommand(
                request.whatsappNumber(),
                UUID.randomUUID().toString(),
                request.text(),
                JsonNodeFactory.instance.objectNode()
        );
        processInboundMessageUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    record DebugInboundMessageRequest(String whatsappNumber, String text) {}
}

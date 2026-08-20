package com.eduar.automacaozap.domain.service;

import java.util.List;
import java.util.Optional;

import tools.jackson.databind.JsonNode;

public record FlowEngineResult(
        String nextStepId,
        JsonNode updatedContext,
        List<String> messagesToSend,
        boolean requiresHandoff,
        boolean invalidInput,
        Optional<LeadDraft> leadToCreate
) {
}

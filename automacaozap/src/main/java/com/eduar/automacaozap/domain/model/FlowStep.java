package com.eduar.automacaozap.domain.model;

import java.util.Map;

public record FlowStep(
        String id,
        StepType type,
        String text,
        Map<String, String> options,
        String nextStepId
) {
}

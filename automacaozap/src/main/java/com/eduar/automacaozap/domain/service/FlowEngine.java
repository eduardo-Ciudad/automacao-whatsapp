package com.eduar.automacaozap.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.eduar.automacaozap.domain.model.Conversation;
import com.eduar.automacaozap.domain.model.Flow;
import com.eduar.automacaozap.domain.model.FlowStep;
import com.eduar.automacaozap.domain.model.StepType;

/**
 * Motor de execução de fluxos de automação.
 *
 * <p>Classe pura de domínio: não injeta nenhum repository, port, bean Spring ou
 * cliente HTTP. Recebe {@link Flow}, {@link Conversation} e o texto recebido do
 * usuário como parâmetros, e devolve um {@link FlowEngineResult} descrevendo o que
 * deveria acontecer a seguir — quem efetivamente persiste a conversa, cria o lead ou
 * envia as mensagens é responsabilidade de uma camada de aplicação, não desta
 * classe.
 *
 * <p>Assume-se que {@link Flow#getDefinition()} segue este formato:
 * <pre>{@code
 * {
 *   "initialStepId": "step1",
 *   "steps": [
 *     {
 *       "id": "step1",
 *       "type": "MESSAGE" | "MENU" | "INPUT" | "ACTION" | "HANDOFF",
 *       "text": "...",
 *       "options": { "1": "stepX", "2": "stepY" },   // apenas em MENU
 *       "contextKey": "nome",                        // apenas em INPUT
 *       "actionType": "CREATE_LEAD",                  // apenas em ACTION
 *       "nextStepId": "stepZ"
 *     }
 *   ]
 * }
 * }</pre>
 */
public class FlowEngine {

    /**
     * Limite defensivo de avanços automáticos (MESSAGE/ACTION) em uma única
     * chamada a {@link #process}. Um fluxo mal configurado (ex: MESSAGE apontando
     * para si mesmo) não deveria travar o processamento em um loop infinito — é
     * preferível falhar de forma explícita.
     */
    private static final int MAX_CASCADE_STEPS = 100;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    public FlowEngineResult process(Flow flow, Conversation conversation, String inboundText) {
        JsonNode definition = flow.getDefinition();

        String currentStepId = conversation.getCurrentStepId() != null
                ? conversation.getCurrentStepId()
                : definition.path("initialStepId").asText();

        FlowStep currentStep = toFlowStep(parseStep(definition, currentStepId));
        JsonNode context = defaultIfMissing(conversation.getContext());

        String resolvedNextStepId;
        JsonNode updatedContext;

        switch (currentStep.type()) {
            case MENU -> {
                String matchedTarget = matchMenuOption(currentStep, inboundText);
                if (matchedTarget == null) {
                    // Entrada inválida em um MENU não é um erro de sistema, é uma
                    // interação normal de usuário (ele digitou algo fora das
                    // opções) — por isso não lança exception, apenas repete o
                    // mesmo step com uma mensagem de reforço, mantendo a conversa
                    // no mesmo lugar.
                    List<String> messages = new ArrayList<>();
                    messages.add("Não entendi, escolha uma das opções abaixo:");
                    messages.add(buildMenuPrompt(currentStep));
                    return new FlowEngineResult(currentStep.id(), context, messages, false, true, Optional.empty());
                }
                resolvedNextStepId = matchedTarget;
                updatedContext = context;
            }
            case INPUT -> {
                String contextKey = textOrNull(parseStep(definition, currentStepId), "contextKey");
                updatedContext = mergeInput(context, contextKey, inboundText);
                resolvedNextStepId = currentStep.nextStepId();
            }
            default -> {
                // TODO: sem classificação de intenção (IA) implementada ainda.
                // Uma mensagem chegando enquanto a conversa está parada em um
                // step que não espera input (MESSAGE/ACTION/HANDOFF já
                // processados, ou HANDOFF aguardando atendente humano) é um
                // evento fora do fluxo esperado. Por ora isso só é sinalizado
                // como entrada inválida, sem alterar o estado da conversa;
                // quando a classificação de intenção existir, este branch deve
                // decidir se reinicia o fluxo, aciona um fluxo diferente, etc.
                return new FlowEngineResult(currentStepId, context, List.of(), false, true, Optional.empty());
            }
        }

        return cascade(definition, resolvedNextStepId, updatedContext);
    }

    /**
     * Avança automaticamente por steps que não esperam input do usuário
     * (MESSAGE, ACTION), acumulando mensagens e efeitos, até encontrar um step
     * que precise parar e esperar (MENU, INPUT) ou que encerre o atendimento
     * automático (HANDOFF).
     */
    private FlowEngineResult cascade(JsonNode definition, String startStepId, JsonNode initialContext) {
        List<String> messages = new ArrayList<>();
        JsonNode context = initialContext;
        Optional<LeadDraft> leadToCreate = Optional.empty();
        String cursor = startStepId;

        for (int i = 0; i < MAX_CASCADE_STEPS; i++) {
            JsonNode stepNode = parseStep(definition, cursor);
            FlowStep step = toFlowStep(stepNode);

            switch (step.type()) {
                case MESSAGE -> {
                    messages.add(interpolate(step.text(), context));
                    cursor = step.nextStepId();
                }
                case MENU -> {
                    messages.add(interpolate(step.text(), context));
                    messages.add(buildMenuPrompt(step));
                    return new FlowEngineResult(cursor, context, messages, false, false, leadToCreate);
                }
                case INPUT -> {
                    messages.add(interpolate(step.text(), context));
                    return new FlowEngineResult(cursor, context, messages, false, false, leadToCreate);
                }
                case ACTION -> {
                    // ACTION não gera mensagem para o usuário — é um efeito
                    // interno (ex: registrar um lead) — por isso continua em
                    // cascata automaticamente, assim como MESSAGE. A criação do
                    // lead em si (I/O) não acontece aqui: só é montado o
                    // LeadDraft, que quem chamar o FlowEngine decide persistir.
                    if ("CREATE_LEAD".equals(textOrNull(stepNode, "actionType"))) {
                        leadToCreate = Optional.of(buildLeadDraft(context));
                    }
                    cursor = step.nextStepId();
                }
                case HANDOFF -> {
                    // HANDOFF sempre para o processamento automático: a partir
                    // daqui um humano assume a conversa, então o motor de fluxo
                    // não tem mais nada a decidir sozinho.
                    if (step.text() != null && !step.text().isBlank()) {
                        messages.add(interpolate(step.text(), context));
                    }
                    return new FlowEngineResult(cursor, context, messages, true, false, leadToCreate);
                }
            }
        }

        throw new IllegalStateException(
                "Fluxo excedeu " + MAX_CASCADE_STEPS + " avanços automáticos a partir do step '"
                        + startStepId + "' — possível ciclo na definição do fluxo.");
    }

    /**
     * Localiza, dentro de {@code definition.steps}, o nó JSON do step com o id
     * informado.
     */
    private JsonNode parseStep(JsonNode definition, String stepId) {
        for (JsonNode stepNode : definition.path("steps")) {
            if (stepId.equals(stepNode.path("id").asText())) {
                return stepNode;
            }
        }
        throw new IllegalStateException("Step '" + stepId + "' não encontrado na definição do fluxo.");
    }

    private FlowStep toFlowStep(JsonNode stepNode) {
        String id = stepNode.path("id").asText();
        StepType type = StepType.valueOf(stepNode.path("type").asText());
        String text = textOrNull(stepNode, "text");
        Map<String, String> options = parseOptions(stepNode.path("options"));
        String nextStepId = textOrNull(stepNode, "nextStepId");
        return new FlowStep(id, type, text, options, nextStepId);
    }

    private Map<String, String> parseOptions(JsonNode optionsNode) {
        Map<String, String> options = new LinkedHashMap<>();
        if (optionsNode.isObject()) {
            for (Map.Entry<String, JsonNode> entry : optionsNode.properties()) {
                options.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return options;
    }

    private String matchMenuOption(FlowStep menuStep, String inboundText) {
        if (inboundText == null) {
            return null;
        }
        String normalized = inboundText.trim();
        for (Map.Entry<String, String> option : menuStep.options().entrySet()) {
            if (option.getKey().equalsIgnoreCase(normalized)) {
                return option.getValue();
            }
        }
        return null;
    }

    private String buildMenuPrompt(FlowStep menuStep) {
        StringBuilder prompt = new StringBuilder();
        if (menuStep.text() != null) {
            prompt.append(menuStep.text());
        }
        for (String key : menuStep.options().keySet()) {
            prompt.append('\n').append("- ").append(key);
        }
        return prompt.toString();
    }

    private JsonNode mergeInput(JsonNode context, String contextKey, String inboundText) {
        if (contextKey == null || contextKey.isBlank() || inboundText == null) {
            return context;
        }
        ObjectNode updated = context.isObject()
                ? ((ObjectNode) context).deepCopy()
                : JsonNodeFactory.instance.objectNode();
        updated.put(contextKey, inboundText.trim());
        return updated;
    }

    /**
     * Monta o rascunho de lead a partir do context acumulado da conversa.
     *
     * <p>Assume que os steps de INPUT anteriores no fluxo preencheram o context
     * com as chaves {@code interest} e {@code description} — os mesmos nomes de
     * campo usados em {@link com.eduar.automacaozap.domain.model.Lead}. Quando
     * ausentes, o campo correspondente do draft fica {@code null}.
     */
    private LeadDraft buildLeadDraft(JsonNode context) {
        return new LeadDraft(textOrNull(context, "interest"), textOrNull(context, "description"));
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private JsonNode defaultIfMissing(JsonNode context) {
        return context == null || context.isMissingNode() || context.isNull()
                ? JsonNodeFactory.instance.objectNode()
                : context;
    }

    /**
     * Substitui placeholders {@code {{chave}}} pelo valor correspondente em
     * {@code context}. Chaves ausentes no context não geram erro — o placeholder
     * é mantido como está no texto, já que uma variável faltando é uma situação
     * de configuração do fluxo, não algo que deva interromper o atendimento.
     */
    private String interpolate(String text, JsonNode context) {
        if (text == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = textOrNull(context, key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}

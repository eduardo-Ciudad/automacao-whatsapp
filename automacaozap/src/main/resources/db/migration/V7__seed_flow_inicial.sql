INSERT INTO flow (id, name, version, definition, active, company_id, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Fluxo Inicial - Atendimento',
    1,
    '{
      "initialStepId": "boas_vindas",
      "steps": [
        {
          "id": "boas_vindas",
          "type": "MESSAGE",
          "text": "Olá! Seja bem-vindo à CiudadLab 👋",
          "nextStepId": "menu_inicial"
        },
        {
          "id": "menu_inicial",
          "type": "MENU",
          "text": "Como posso te ajudar?\n1 - Quero um orçamento\n2 - Falar com atendente",
          "options": {"1": "pergunta_nome", "2": "step_suporte"}
        },
        {
          "id": "pergunta_nome",
          "type": "INPUT",
          "text": "Qual o seu nome?",
          "contextKey": "nome",
          "nextStepId": "pergunta_interesse"
        },
        {
          "id": "pergunta_interesse",
          "type": "INPUT",
          "text": "Legal, {{nome}}! Qual o seu interesse (ex: site, e-commerce, sistema)?",
          "contextKey": "interest",
          "nextStepId": "pergunta_descricao"
        },
        {
          "id": "pergunta_descricao",
          "type": "INPUT",
          "text": "Me conta rapidamente o que você precisa.",
          "contextKey": "description",
          "nextStepId": "cria_lead"
        },
        {
          "id": "cria_lead",
          "type": "ACTION",
          "actionType": "CREATE_LEAD",
          "nextStepId": "confirmacao"
        },
        {
          "id": "confirmacao",
          "type": "INPUT",
          "text": "Show, {{nome}}! Registrei seu pedido e nossa equipe vai te retornar em breve.",
          "contextKey": "observacao_final",
          "nextStepId": "confirmacao"
        },
        {
          "id": "step_suporte",
          "type": "HANDOFF",
          "text": "Combinado! Já vou te transferir para um de nossos atendentes."
        }
      ]
    }'::jsonb,
    true,
    1,
    now(),
    now()
)
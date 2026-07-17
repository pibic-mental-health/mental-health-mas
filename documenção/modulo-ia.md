# Módulo Conversacional com Modelo de Linguagem

## 1. Finalidade do módulo

O módulo conversacional tem como objetivo gerar respostas textuais de apoio inicial a partir das informações processadas pelo sistema multiagente.

No contexto deste projeto, o modelo de linguagem não é utilizado para realizar diagnóstico, tratamento psicológico ou acompanhamento clínico. Sua função é apoiar a comunicação inicial com o usuário simulado, organizar informações e reforçar a recomendação de busca por apoio profissional quando apropriado.

O sistema deve ser compreendido como uma prova de conceito acadêmica de triagem emocional apoiada por agentes inteligentes.

---

## 2. Papel da inteligência artificial no sistema

A inteligência artificial atua como um componente auxiliar dentro da arquitetura multiagente.

Ela não opera de forma isolada. Antes de gerar uma resposta, o `AgenteConversacional` recebe informações de outros agentes, como:

- `AgenteTriagemFormulario`: identifica um perfil emocional inicial a partir do formulário simulado;
- `AgenteMemoria`: fornece contexto textual e histórico da interação;
- `AgenteSeguranca`: classifica o nível de risco textual;
- `AgenteConteudo`: sugere conteúdos de apoio, quando solicitado pelo usuário simulado;
- `AgentePsicologo`: sugere profissionais cadastrados, quando solicitado pelo usuário simulado;
- `AgenteMonitoramento`: realiza apenas uma simulação acadêmica de acompanhamento;
- `AgenteRelatorio`: gera um relatório simulado e demonstrativo.

Dessa forma, a IA conversacional é apenas uma parte do sistema e não o elemento responsável por decisões clínicas.

---

## 3. Camada de abstração da IA

Para evitar dependência direta de um único provedor de inteligência artificial, foi criada uma camada intermediária chamada `ClienteLLM`.

O `AgenteConversacional` não chama diretamente uma classe específica de provedor. Em vez disso, ele envia o prompt para o `ClienteLLM`, que seleciona o provedor configurado.

No estágio atual do protótipo, a execução confirma o uso da camada genérica:

```text
[CONVERSACIONAL] Provedor de IA configurado: NVIDIA_NIM
[LLM] Provedor configurado: NVIDIA_NIM
```

Essa decisão permite substituir o provedor de IA no futuro sem alterar a arquitetura principal dos agentes.

---

## 4. Provedor utilizado no protótipo

No protótipo atual, o provedor configurado é:

- Provedor: NVIDIA NIM;
- Classe concreta: `ClienteNvidia`;
- Classe de abstração: `ClienteLLM`;
- Variável de ambiente prevista: `LLM_PROVIDER`;
- Valor padrão: `NVIDIA_NIM`.

A documentação oficial da NVIDIA NIM informa que a plataforma disponibiliza endpoints para modelos de linguagem, incluindo o endpoint `/v1/chat/completions`, voltado a conversas com histórico de mensagens.

---

## 5. Possíveis provedores futuros

A arquitetura permite adaptação para outros provedores de IA, desde que implementados atrás da camada `ClienteLLM`.

### 5.1 DeepSeek

O DeepSeek possui documentação oficial para criação de respostas conversacionais por meio do endpoint `/chat/completions`.

Esse provedor pode ser avaliado em uma etapa futura do projeto, especialmente por possuir documentação pública da API e por permitir integração em formato semelhante a APIs de chat.

### 5.2 Ollama local

O Ollama pode ser considerado em uma etapa futura para execução local de modelos de linguagem.

Essa opção é relevante academicamente porque pode reduzir a dependência de serviços externos e facilitar experimentos controlados em ambiente local. A documentação do Ollama indica que, quando executado localmente, sua API pode ser acessada em `http://localhost:11434`.

---

## 6. Entrada do módulo conversacional

O módulo conversacional recebe um prompt textual contendo:

- nome do usuário simulado;
- perfil emocional identificado pela triagem;
- nível de risco identificado pelo agente de segurança;
- histórico armazenado pelo agente de memória;
- mensagem atual enviada pelo usuário simulado;
- regras de segurança e limitação de atuação.

Exemplo de informações enviadas ao modelo:

```text
Nome: Maria
Perfil: MISTO
Risco: BAIXO_RISCO
Histórico:
- Usuário: Hoje estou muito preocupado e com dificuldade para relaxar.
```

---

## 7. Saída do módulo conversacional

A saída esperada é uma resposta textual em português do Brasil, com linguagem acolhedora, breve e não clínica.

A resposta deve:

- acolher a mensagem do usuário;
- evitar diagnóstico;
- não afirmar tratamento;
- não prometer acompanhamento clínico;
- reforçar que a plataforma não substitui psicólogos;
- incentivar busca por apoio profissional quando apropriado;
- oferecer opções controladas de conteúdo, indicação profissional e simulação acadêmica.

---

## 8. Restrições aplicadas ao prompt

O prompt utilizado pelo `AgenteConversacional` contém regras explícitas para limitar a atuação do modelo.

Entre essas regras estão:

```text
- Não afirme diagnósticos.
- Não diga que está tratando o usuário.
- Não prometa acompanhamento clínico.
- Reforce que a plataforma não substitui psicólogo.
- Quando fizer sentido, incentive a busca por apoio profissional.
```

Essas restrições foram adicionadas para reduzir o risco de o sistema ser interpretado como uma ferramenta clínica.

---

## 9. Tratamento de falhas

Caso o provedor de IA não retorne uma resposta válida, o sistema utiliza respostas alternativas previamente definidas no próprio `AgenteConversacional`.

Esse mecanismo é chamado de fallback.

O fallback evita que a execução dependa exclusivamente da resposta do modelo externo e garante que o sistema ainda consiga responder de maneira controlada.

Exemplos de situações em que o fallback pode ser utilizado:

- indisponibilidade da API externa;
- erro de autenticação;
- resposta vazia;
- retorno inválido;
- erro de comunicação com o provedor.

---

## 10. Limitações do uso de LLM

Modelos de linguagem podem apresentar limitações, como:

- geração de respostas imprecisas;
- excesso de confiança no texto gerado;
- dificuldade em reconhecer todos os contextos sensíveis;
- variação de resposta para uma mesma entrada;
- dependência de disponibilidade do provedor externo;
- risco de inadequação em contextos de saúde mental.

Por isso, o modelo é utilizado apenas como componente auxiliar de uma prova de conceito, sempre acompanhado de agentes de controle, mensagens de limitação e fluxos simulados.

---

## 11. Dados utilizados

Nesta etapa do projeto, os dados utilizados são simulados.

O usuário `Maria`, o formulário emocional, as respostas, os dados semanais e o relatório são utilizados apenas para demonstração técnica da arquitetura.

Não há, nesta etapa, coleta real de dados sensíveis de usuários.

---

## 12. Considerações éticas

Como o projeto envolve o domínio de saúde mental, qualquer uso com pessoas reais exigirá cuidados adicionais.

A aplicação com usuários reais, entrevistas, coleta de dados sensíveis, validação de campo ou acompanhamento emocional dependerá de aprovação prévia por Comitê de Ética em Pesquisa.

Nesta fase, o sistema permanece como uma prova de conceito acadêmica com dados simulados.

O projeto não deve ser apresentado como ferramenta de atendimento psicológico, diagnóstico, tratamento ou substituição de profissionais da saúde.

---

## 13. Justificativa arquitetural

A separação entre `AgenteConversacional`, `ClienteLLM` e provedores concretos de IA fortalece a arquitetura do projeto.

Essa separação permite:

- trocar o provedor de IA sem alterar os agentes;
- documentar melhor o uso da IA;
- testar diferentes modelos futuramente;
- reduzir dependência tecnológica;
- melhorar a reprodutibilidade acadêmica;
- manter o foco do projeto no sistema multiagente.

Assim, o núcleo da pesquisa permanece sendo a arquitetura multiagente de triagem emocional, e não um provedor específico de inteligência artificial.

---

## 14. Estado atual da implementação

O fluxo atual do sistema é:

```text
AgentePaciente
→ AgenteTriagemFormulario
→ AgenteMemoria
→ AgenteSeguranca
→ AgenteConversacional
→ ClienteLLM
→ ClienteNvidia
→ AgenteConteudo
→ AgentePsicologo
→ AgenteMonitoramentoSimulado
→ AgenteRelatorio
→ AgenteConversacional
→ AgentePaciente
→ AgenteMemoria
```

A camada `ClienteLLM` permite que, futuramente, o sistema seja adaptado para:

```text
ClienteLLM
├── ClienteNvidia
├── ClienteDeepSeek
└── ClienteOllamaLocal
```

---

## 15. Evidência de execução no protótipo

A execução do protótipo demonstrou que o `AgenteConversacional` utiliza a camada `ClienteLLM` para selecionar o provedor configurado:

```text
[CONVERSACIONAL] Provedor de IA configurado: NVIDIA_NIM
[LLM] Provedor configurado: NVIDIA_NIM
```

Também foi confirmado que o relatório simulado é gerado pelo `AgenteRelatorio`, enviado ao `AgenteConversacional` e entregue ao `AgentePaciente`.

O relatório gerado contém aviso explícito de limitação:

```text
Este relatorio possui finalidade academica e demonstrativa.
Nao representa avaliacao clinica, diagnostico ou acompanhamento psicologico real.
```

---

## 16. Referências técnicas consultadas

- NVIDIA NIM for Large Language Models — API Reference: https://docs.nvidia.com/nim/large-language-models/latest/api-reference.html
- DeepSeek API Docs — Create Chat Completion: https://api-docs.deepseek.com/api/create-chat-completion
- DeepSeek API Docs — Your First API Call: https://api-docs.deepseek.com/
- Ollama Docs — API Introduction: https://docs.ollama.com/api/introduction
- Ollama Docs — API Authentication: https://docs.ollama.com/api/authentication
- Ollama Docs — OpenAI Compatibility: https://docs.ollama.com/api/openai-compatibility

---

## 17. Conclusão

O módulo conversacional foi projetado como uma camada auxiliar de interação textual dentro de um sistema multiagente.

A principal decisão técnica foi isolar o provedor de IA em uma camada abstrata, permitindo maior flexibilidade, documentação e controle acadêmico.

A IA não realiza avaliação clínica, não substitui profissionais e não executa acompanhamento real. Sua função é apoiar a demonstração de uma arquitetura multiagente voltada à triagem emocional inicial, utilizando dados simulados e respeitando as limitações éticas do domínio.

# Documentação Técnica — Sistema Multiagente de Triagem Emocional Inteligente

## 1. Visão geral

Este projeto apresenta um protótipo acadêmico de um **Sistema Multiagente de Triagem Emocional Inteligente**, desenvolvido em Java com o framework JADE. O objetivo do sistema é demonstrar como agentes inteligentes podem atuar de forma coordenada para realizar uma triagem emocional inicial, organizar informações do usuário, oferecer respostas empáticas, sugerir conteúdos educativos, indicar profissionais cadastrados de forma demonstrativa, listar locais de atendimento e gerar uma simulação de acompanhamento semanal.

O sistema não realiza diagnóstico psicológico, tratamento, prescrição, acompanhamento clínico real ou substituição de profissionais da saúde. Sua finalidade é exclusivamente acadêmica, técnica e demonstrativa.

---

## 2. Objetivo do sistema

O objetivo principal do protótipo é demonstrar uma arquitetura multiagente capaz de:

1. Coletar respostas de um formulário emocional simulado.
2. Classificar um perfil inicial de triagem emocional.
3. Realizar uma conversa empática com apoio de IA generativa.
4. Avaliar mensagens com um agente de segurança.
5. Registrar interações em memória.
6. Sugerir conteúdos educativos baseados em evidências cadastradas.
7. Apresentar indicações demonstrativas de psicólogos.
8. Apresentar locais demonstrativos de atendimento, como CAPS, UBS e serviços públicos.
9. Simular acompanhamento semanal com dados artificiais.
10. Gerar um relatório simulado com finalidade acadêmica.

---

## 3. Tecnologias utilizadas

- **Linguagem:** Java
- **Framework multiagente:** JADE 4.6.0
- **Gerenciador de dependências:** Maven
- **IA generativa:** camada abstrata `ClienteLLM`, atualmente configurada para NVIDIA NIM
- **Formato das bases locais:** JSON
- **Biblioteca JSON:** Gson

---

## 4. Arquitetura geral

A arquitetura é baseada em múltiplos agentes especializados, cada um com uma responsabilidade bem definida. O sistema segue uma organização modular, em que o `AgenteConversacional` atua como o principal orquestrador das interações após a triagem inicial.

Fluxo geral:

```text
AgentePaciente
→ AgenteTriagemFormulario
→ AgenteConversacional
→ AgenteSeguranca
→ AgenteMemoria
→ ClienteLLM / NVIDIA_NIM
→ AgenteConteudo
→ AgentePsicologo
→ AgenteLocalAtendimento
→ AgenteMonitoramento
→ AgenteRelatorio
→ AgenteMemoria
```

---

## 5. Agentes do sistema

### 5.1 AgentePaciente

O `AgentePaciente` simula a interação inicial de um usuário com o sistema. Ele envia um formulário emocional ao agente de triagem e, em seguida, envia uma mensagem ao agente conversacional.

Responsabilidades:

- Simular o preenchimento do formulário.
- Enviar dados ao `AgenteTriagemFormulario`.
- Receber o perfil identificado.
- Enviar mensagem ao `AgenteConversacional`.
- Responder automaticamente aos convites do sistema.
- Receber conteúdos, psicólogos, locais de atendimento e relatório simulado.

Exemplo de formulário enviado:

```text
nome=Maria;preocupacao=4;nervosismo=4;relaxamento=3;sono=2;tristeza=2;energia=1;interesse=1;isolamento=2;
```

---

### 5.2 AgenteTriagemFormulario

O `AgenteTriagemFormulario` recebe as respostas do formulário, calcula pontuações iniciais e identifica um perfil de triagem.

Responsabilidades:

- Receber dados do formulário.
- Calcular escore de ansiedade.
- Calcular escore de depressão.
- Classificar o perfil como `ANSIEDADE`, `DEPRESSAO`, `MISTO` ou `GERAL`.
- Enviar o perfil identificado ao `AgentePaciente`.
- Registrar nome e perfil no `AgenteMemoria`.

Critério usado no protótipo:

```text
ansiedade = preocupacao + nervosismo + relaxamento + sono
depressao = tristeza + (4 - energia) + (4 - interesse) + isolamento
```

Observação: essa regra é apenas demonstrativa e não corresponde a instrumento clínico validado.

---

### 5.3 AgenteConversacional

O `AgenteConversacional` é o principal orquestrador do sistema após a etapa de triagem. Ele recebe a mensagem do usuário, consulta segurança, memória e IA generativa, além de coordenar os demais agentes conforme as respostas do usuário.

Responsabilidades:

- Receber a mensagem do paciente.
- Consultar o `AgenteSeguranca`.
- Consultar o `AgenteMemoria`.
- Construir o prompt para IA generativa.
- Acionar `ClienteLLM`.
- Gerar ou recuperar resposta de fallback.
- Convidar o usuário para acessar conteúdos educativos.
- Convidar o usuário para visualizar psicólogos cadastrados.
- Convidar o usuário para visualizar locais demonstrativos de atendimento.
- Convidar o usuário para visualizar simulação de acompanhamento semanal.
- Encaminhar relatório simulado ao paciente.

O agente reforça em suas respostas que a plataforma não substitui psicólogo e não realiza diagnóstico.

---

### 5.4 AgenteSeguranca

O `AgenteSeguranca` analisa a mensagem textual recebida e classifica o nível de risco inicial.

Responsabilidades:

- Receber texto do usuário.
- Identificar sinais de baixo risco, atenção ou risco.
- Retornar classificação ao `AgenteConversacional`.

Classificações usadas:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Em situações classificadas como risco, o sistema deve responder de forma segura, recomendando busca imediata de apoio profissional, serviço de emergência ou pessoa de confiança.

---

### 5.5 AgenteMemoria

O `AgenteMemoria` armazena o histórico básico da interação e os estados de decisão do usuário.

Responsabilidades:

- Salvar nome.
- Salvar perfil de triagem.
- Salvar nível de risco.
- Salvar mensagens do usuário.
- Salvar respostas geradas.
- Salvar preferência por conteúdos.
- Salvar preferência por psicólogos.
- Salvar preferência por locais de atendimento.
- Salvar aceite da simulação de monitoramento.
- Salvar geração de relatório simulado.
- Retornar um resumo da interação quando solicitado.

Estados registrados:

```text
conteudo=aceito/recusado
psicologo=aceito/recusado
local_atendimento=aceito/recusado
monitoramento_simulado=aceito/recusado
relatorio_simulado=gerado
```

---

### 5.6 AgenteConteudo

O `AgenteConteudo` sugere conteúdos educativos a partir de uma base local estruturada em JSON.

Responsabilidades:

- Ler o arquivo `conteudos_evidencias.json`.
- Filtrar conteúdos conforme o perfil identificado.
- Retornar sugestões educativas ao paciente.
- Apresentar título, tipo, descrição, nível de evidência, fonte e observação ética.

Exemplos de tipos de conteúdo:

```text
meditacao
respiracao
atividade_fisica
musica
autocuidado
```

Exemplo de retorno:

```text
Prática breve de mindfulness
Tipo: meditacao
Evidencia: revisao_sistematica
Fonte: Goyal et al., 2014
```

Os conteúdos são apenas educativos e não substituem acompanhamento profissional.

---

### 5.7 AgentePsicologo

O `AgentePsicologo` retorna indicações demonstrativas de profissionais cadastrados na base do protótipo.

Responsabilidades:

- Receber perfil e risco.
- Consultar uma base local demonstrativa.
- Retornar indicações relacionadas ao perfil.

Importante: os dados de psicólogos usados no protótipo são demonstrativos. Em uma aplicação real, seria necessário validar credenciais, consentimento, dados cadastrais, disponibilidade, regras profissionais e conformidade ética.

---

### 5.8 AgenteLocalAtendimento

O `AgenteLocalAtendimento` sugere locais demonstrativos de atendimento, como CAPS, UBS e serviços públicos de saúde mental.

Responsabilidades:

- Ler o arquivo `locais_atendimento.json`.
- Receber cidade e UF.
- Filtrar locais compatíveis.
- Retornar lista informativa de serviços.

Exemplo de consulta:

```text
cidade=Brasilia;uf=DF
```

Exemplo de retorno:

```text
CAPS II Brasília
Unidade Básica de Saúde - Asa Sul
Serviço de Saúde Mental Ambulatorial
```

Os registros são demonstrativos. Em uma versão real, os dados deveriam ser validados em fontes oficiais, como CNES, Dados Abertos SUS ou secretarias de saúde.

---

### 5.9 AgenteMonitoramento

O `AgenteMonitoramento` executa uma simulação acadêmica de acompanhamento semanal.

Responsabilidades:

- Receber solicitação de início de simulação.
- Gerar dados artificiais de sete dias.
- Simular indicadores como ansiedade, humor, energia e sono.
- Enviar os dados ao `AgenteRelatorio`.

Exemplo de dados simulados:

```text
dia=1,ansiedade=3,humor=4,energia=2,sono=4
```

Esses dados são artificiais e não representam acompanhamento psicológico real.

---

### 5.10 AgenteRelatorio

O `AgenteRelatorio` recebe os dados simulados do monitoramento e gera um relatório demonstrativo.

Responsabilidades:

- Receber dados simulados.
- Calcular médias simples dos indicadores.
- Gerar um relatório semanal simulado.
- Enviar o relatório ao `AgenteConversacional`.

O relatório informa explicitamente que possui finalidade acadêmica e não representa diagnóstico, avaliação clínica ou acompanhamento psicológico real.

---

### 5.11 ClienteLLM

O `ClienteLLM` é a camada de abstração para uso de modelos de linguagem.

Responsabilidades:

- Centralizar a escolha do provedor de IA.
- Permitir troca futura de provedor sem alterar os agentes principais.
- Encaminhar prompts ao cliente configurado.

Provedor atual:

```text
NVIDIA_NIM
```

Provedores planejados:

```text
DEEPSEEK
OLLAMA_LOCAL
```

Essa camada permite que o sistema seja provider-agnostic, facilitando substituição ou comparação entre diferentes modelos.

---

## 6. Bases JSON utilizadas

### 6.1 `conteudos_evidencias.json`

Base local com conteúdos educativos associados a perfis emocionais e referências cadastradas.

Campos principais:

```text
perfil
tipo
titulo
descricao
nivelEvidencia
fonte
referencia
observacao
```

---

### 6.2 `psicologos.json`

Base local demonstrativa com profissionais simulados.

Campos esperados:

```text
nome
especialidade
modalidade
contato
perfilRelacionado
```

---

### 6.3 `locais_atendimento.json`

Base local demonstrativa com serviços de atendimento.

Campos principais:

```text
nome
tipo
descricao
cidade
uf
endereco
telefone
fonte
observacao
```

---

## 7. Fluxo de mensagens

### 7.1 Fluxo principal

```text
1. AgentePaciente envia formulário ao AgenteTriagemFormulario.
2. AgenteTriagemFormulario calcula escores e identifica perfil.
3. AgenteTriagemFormulario envia perfil ao AgentePaciente.
4. AgentePaciente envia mensagem ao AgenteConversacional.
5. AgenteConversacional consulta AgenteSeguranca.
6. AgenteConversacional salva dados no AgenteMemoria.
7. AgenteConversacional consulta memória.
8. AgenteConversacional chama ClienteLLM.
9. AgenteConversacional envia resposta ao AgentePaciente.
10. AgentePaciente aceita conteúdos, psicólogos, locais e monitoramento simulado.
11. AgenteConversacional aciona os agentes especializados.
12. AgenteMonitoramento gera dados artificiais.
13. AgenteRelatorio gera relatório simulado.
14. AgenteConversacional encaminha relatório ao AgentePaciente.
15. AgenteMemoria registra as decisões e o relatório gerado.
```

---

## 8. Exemplo de execução validada

Durante a execução do protótipo, foi validado que:

- O `AgenteLocalAtendimento` iniciou corretamente.
- O paciente respondeu ao convite de locais de atendimento.
- O agente recebeu a cidade e UF.
- A lista de locais demonstrativos foi retornada.
- A memória salvou corretamente a preferência por locais de atendimento.

Exemplo de evento registrado:

```text
[MEMORIA] Preferencia de locais de atendimento salva: aceito
```

---

## 9. Limitações éticas

Este sistema manipula informações potencialmente sensíveis relacionadas à saúde emocional. Por isso, o projeto adota as seguintes limitações:

1. Não realiza diagnóstico.
2. Não realiza tratamento.
3. Não substitui psicólogos, psiquiatras ou profissionais da saúde.
4. Não executa acompanhamento clínico real.
5. Não coleta dados reais de pacientes nesta versão.
6. Não deve ser usado em campo sem avaliação ética.
7. O monitoramento semanal é apenas simulado.
8. As indicações de psicólogos são demonstrativas.
9. Os locais de atendimento são demonstrativos.
10. Qualquer uso real exigiria aprovação ética, consentimento informado e supervisão profissional.

---

## 10. Considerações sobre Comitê de Ética

Caso o projeto evolua para aplicação com usuários reais, entrevistas, coleta de dados emocionais, armazenamento de histórico pessoal ou acompanhamento em ambiente real, será necessário submeter a pesquisa ao Comitê de Ética competente.

A versão atual permanece como protótipo acadêmico com dados simulados.

---

## 11. Limitações técnicas

A versão atual possui algumas limitações técnicas:

1. As mensagens entre agentes usam strings com pares `chave=valor` separados por ponto e vírgula.
2. O uso de `blockingReceive()` pode capturar respostas inesperadas em fluxos mais complexos.
3. As bases de psicólogos e locais são locais e demonstrativas.
4. A classificação de risco é simplificada.
5. A triagem usa regra própria do protótipo, não instrumento clínico validado.
6. O monitoramento semanal é gerado aleatoriamente.
7. A resposta da IA depende do provedor externo configurado.
8. Ainda não há interface gráfica para usuário final.

---

## 12. Melhorias futuras

Possíveis evoluções do projeto:

1. Substituir mensagens em string por JSON estruturado.
2. Usar `MessageTemplate` e `conversationId` no JADE.
3. Criar interface web para interação com o usuário.
4. Implementar autenticação e controle de privacidade.
5. Melhorar a classificação de risco com regras mais robustas.
6. Permitir configuração dinâmica do provedor de IA.
7. Integrar com Ollama para execução local.
8. Integrar bases oficiais de serviços de saúde.
9. Criar painel para visualização do relatório simulado.
10. Documentar formalmente os riscos éticos e a necessidade de aprovação por Comitê de Ética em cenários reais.

---

## 13. Comandos de execução

Compilar o projeto:

```bash
mvn clean compile
```

Executar o sistema:

```bash
mvn exec:java
```

Caso a dependência local do JADE não seja encontrada:

```bash
mvn install:install-file "-Dfile=lib\jade.jar" "-DgroupId=jade" "-DartifactId=jade" "-Dversion=4.6.0" "-Dpackaging=jar"
```

---

## 14. Conclusão

O protótipo demonstra uma arquitetura multiagente funcional para triagem emocional inicial em contexto acadêmico. A divisão por agentes torna o sistema modular, permitindo separar responsabilidades como triagem, memória, segurança, conversação, conteúdos educativos, indicação de profissionais, locais de atendimento, simulação de monitoramento e geração de relatório.

A integração com IA generativa amplia a capacidade conversacional do sistema, enquanto as bases locais em JSON permitem controle sobre conteúdos e dados demonstrativos. O projeto reforça constantemente suas limitações éticas, evitando se apresentar como ferramenta clínica ou substituta de profissionais da saúde.

Assim, o sistema se consolida como uma prova de conceito acadêmica para investigação de arquiteturas multiagentes aplicadas ao apoio inicial e à organização de informações em contextos de triagem emocional.

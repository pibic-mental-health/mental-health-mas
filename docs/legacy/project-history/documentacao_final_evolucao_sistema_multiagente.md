# Documentação Final — Evolução do Sistema Multiagente de Triagem Emocional

**Projeto:** `mental-health-mas`  
**Contexto:** PIBIC — Sistema Multiagente de Triagem Emocional Inteligente  
**Objetivo da evolução:** tornar o protótipo mais robusto, seguro, testável e alinhado a uma abordagem acadêmica, ética e não clínica.

---

## 1. Visão geral da evolução

O sistema evoluiu de uma prova de conceito com agentes simples para uma arquitetura multiagente mais organizada, com separação clara de responsabilidades.

A principal mudança foi a introdução de um fluxo de decisão mais seguro:

```text
AgentePaciente
    ↓
AgenteTriagemFormulario
    ↓
AgenteConversacional
    ↓
AgenteSeguranca
    ↓
AgenteIntervencao
    ↓
AgenteMemoria
    ↓
Agentes auxiliares
```

Essa evolução permite que o sistema:

```text
- simule diferentes perfis emocionais;
- classifique risco de forma híbrida;
- escolha protocolos de intervenção;
- controle quais agentes podem ou não ser acionados;
- registre histórico e protocolo na memória;
- use IA de forma mais segura e controlada;
- bloqueie fluxos inadequados em situações de risco;
- mantenha o caráter acadêmico e demonstrativo do projeto.
```

---

## 2. Arquitetura atual dos agentes

### 2.1 AgentePaciente

O `AgentePaciente` passou a ter integração com IA para simular diferentes mensagens de teste.

Ele pode executar cenários:

```text
BAIXO_RISCO
ATENCAO
RISCO
ALEATORIO
```

O cenário é definido por variável de ambiente:

```powershell
$env:CENARIO_PACIENTE="BAIXO_RISCO"
mvn exec:java
```

ou:

```powershell
$env:CENARIO_PACIENTE="ATENCAO"
mvn exec:java
```

ou:

```powershell
$env:CENARIO_PACIENTE="RISCO"
mvn exec:java
```

A versão refinada do agente possui:

```text
- prompts separados por cenário;
- validação da mensagem gerada pela IA;
- fallback controlado;
- bloqueio de mensagens fortes em BAIXO_RISCO;
- mensagem segura e indireta em RISCO;
- resposta automática apenas aos convites exibidos pelo sistema.
```

---

### 2.2 AgenteTriagemFormulario

O `AgenteTriagemFormulario` continua responsável pela pontuação inicial.

Ele calcula:

```text
Score de Ansiedade
Score de Depressão
Perfil final
```

Perfis possíveis:

```text
GERAL
ANSIEDADE
DEPRESSAO
MISTO
```

A triagem não realiza diagnóstico. Ela apenas classifica o perfil inicial para fins acadêmicos e demonstrativos.

---

### 2.3 AgenteSeguranca

O `AgenteSeguranca` foi uma das principais melhorias do sistema.

Antes, ele usava apenas regras simples por palavras-chave.

Agora ele usa uma arquitetura híbrida:

```text
1. Regras críticas conservadoras
2. Classificação contextual por IA
3. Fallback por pontuação
```

Ele retorna três níveis:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Além disso, registra informações internas:

```text
nivelRisco
confianca
metodo
categoria
justificativa
```

Exemplo de log:

```text
[SEGURANCA] Nivel de risco: BAIXO_RISCO
[SEGURANCA] Confianca: 0.8
[SEGURANCA] Metodo: ia_llm
[SEGURANCA] Categoria: estresse
[SEGURANCA] Justificativa: preocupacao_com_rotina
```

Em casos críticos, o agente não depende da IA. Ele usa regras críticas diretamente.

Exemplo:

```text
[SEGURANCA] Nivel de risco: RISCO
[SEGURANCA] Confianca: 1.0
[SEGURANCA] Metodo: regras_criticas
[SEGURANCA] Categoria: possivel_risco_imediato
```

---

### 2.4 AgenteIntervencao

O `AgenteIntervencao` foi criado para separar a classificação de risco da decisão de conduta.

Essa separação é importante:

```text
AgenteSeguranca classifica.
AgenteIntervencao decide o protocolo.
AgenteConversacional executa somente o que foi permitido.
```

Protocolos atuais:

```text
PROTOCOLO_APOIO_INICIAL
PROTOCOLO_ENCAMINHAMENTO_SEGURO
PROTOCOLO_PRESERVACAO_DA_VIDA
```

---

### 2.5 AgenteConversacional

O `AgenteConversacional` foi ajustado para respeitar os protocolos.

Ele agora:

```text
- consulta o AgenteSeguranca;
- consulta o AgenteIntervencao;
- salva o protocolo no AgenteMemoria;
- decide se pode ou não chamar IA comum;
- decide se pode ou não oferecer conteúdo;
- decide se pode ou não oferecer psicólogos;
- decide se pode ou não oferecer locais de atendimento;
- decide se pode ou não iniciar monitoramento simulado;
- exibe CVV como recurso ou ação conforme o protocolo.
```

A resposta muda conforme o risco.

---

### 2.6 AgenteMemoria

O `AgenteMemoria` foi atualizado para reconhecer:

```text
protocolo_intervencao
```

Agora o resumo inclui:

```text
Nome
Perfil
Risco
Protocolo de intervenção
Histórico
Preferências
Status de monitoramento
Status de relatório
```

Exemplo de resumo:

```text
Nome: Maria
Perfil: GERAL
Risco: BAIXO_RISCO
Protocolo de intervencao: PROTOCOLO_APOIO_INICIAL
Historico:
- Usuario: Estou um pouco ansioso com a proximidade de prazos, mas vou manter a rotina como sempre.
```

---

### 2.7 AgenteConteudo

O `AgenteConteudo` recomenda conteúdos educativos conforme:

```text
- perfil identificado;
- palavras da mensagem;
- nível de evidência;
- pontuação base do conteúdo.
```

Esses conteúdos são sempre apresentados como apoio inicial e educativo, sem função clínica.

---

### 2.8 AgentePsicologo

O `AgentePsicologo` indica profissionais simulados/cadastrados conforme perfil e risco.

Após os ajustes, o risco enviado ao agente foi corrigido em cenários de atenção.

Exemplo esperado:

```text
perfil=MISTO;risco=ATENCAO
```

---

### 2.9 AgenteLocalAtendimento

O `AgenteLocalAtendimento` consulta fontes externas e locais demonstrativos:

```text
Google Places API
OpenStreetMap / Overpass API
CNES / Dados Abertos SUS
Base local de fallback
```

Critério de priorização:

```text
1. CAPS e atenção psicossocial
2. Serviços especializados em saúde mental
3. Psicologia, psicoterapia e psiquiatria
4. Instituições relacionadas
5. UBS e atenção primária
```

O sistema sempre informa que os dados devem ser validados em canais oficiais antes de qualquer uso real.

---

### 2.10 AgenteMonitoramento e AgenteRelatorio

O monitoramento foi mantido apenas como simulação acadêmica.

Ele gera dados artificiais de sete dias e envia ao `AgenteRelatorio`.

O relatório deixa claro:

```text
Nao representa avaliacao clinica, diagnostico ou acompanhamento psicologico real.
```

Esse cuidado é importante para manter o projeto dentro de uma proposta demonstrativa.

---

## 3. Protocolos implementados

### 3.1 PROTOCOLO_APOIO_INICIAL

Usado para `BAIXO_RISCO`.

Permissões:

```text
permitirIA=true
permitirConteudo=true
permitirPsicologo=true
permitirLocalAtendimento=true
permitirMonitoramento=true
exibirBotaoCVV=false
```

Comportamento esperado:

```text
- resposta conversacional normal;
- conteúdo educativo permitido;
- locais de atendimento permitidos;
- monitoramento simulado permitido;
- CVV não exibido;
- sem tom de emergência.
```

---

### 3.2 PROTOCOLO_ENCAMINHAMENTO_SEGURO

Usado para `ATENCAO`.

Permissões:

```text
permitirIA=true
permitirConteudo=true
permitirPsicologo=true
permitirLocalAtendimento=true
permitirMonitoramento=false
exibirBotaoCVV=true
```

Comportamento esperado:

```text
- resposta acolhedora;
- incentivo a apoio profissional;
- CVV exibido como recurso de apoio;
- emergência não aparece como ação imediata;
- monitoramento simulado bloqueado;
- conteúdo, psicólogo e locais ainda podem ser oferecidos.
```

---

### 3.3 PROTOCOLO_PRESERVACAO_DA_VIDA

Usado para `RISCO`.

Permissões:

```text
permitirIA=false
permitirConteudo=false
permitirPsicologo=false
permitirLocalAtendimento=true
permitirMonitoramento=false
exibirBotaoCVV=true
```

Comportamento esperado:

```text
- IA comum bloqueada;
- conteúdo educativo bloqueado;
- psicólogos simulados bloqueados;
- monitoramento bloqueado;
- CVV exibido como ação recomendada;
- orientação para apoio imediato;
- locais de atendimento permitidos.
```

---

## 4. Validação dos cenários

### 4.1 Cenário BAIXO_RISCO

Comando usado:

```powershell
$env:CENARIO_PACIENTE="BAIXO_RISCO"
mvn clean compile
mvn exec:java
```

Mensagem simulada:

```text
Estou um pouco ansioso com a proximidade de prazos, mas vou manter a rotina como sempre.
```

Resultado:

```text
Perfil: GERAL
Risco: BAIXO_RISCO
Protocolo: PROTOCOLO_APOIO_INICIAL
Monitoramento: permitido
CVV: não exibido
```

Esse cenário validou o fluxo de baixo risco e o relatório simulado.

---

### 4.2 Cenário ATENCAO

Comando usado:

```powershell
$env:CENARIO_PACIENTE="ATENCAO"
mvn exec:java
```

Exemplo de mensagem simulada:

```text
Estou me sentindo muito sofrido e não sei mais como lidar com tudo isso. Preciso de alguém para me ouvir.
```

Resultado:

```text
Perfil: MISTO
Risco: ATENCAO
Protocolo: PROTOCOLO_ENCAMINHAMENTO_SEGURO
Monitoramento: bloqueado
CVV: exibido como recurso de apoio
Psicólogo: permitido
Locais de atendimento: permitido
```

Esse cenário validou o encaminhamento seguro.

---

### 4.3 Cenário RISCO

Comando usado:

```powershell
$env:CENARIO_PACIENTE="RISCO"
mvn exec:java
```

Mensagem final controlada:

```text
Eu nao me sinto seguro agora e preciso de ajuda imediata.
```

Resultado:

```text
Perfil: MISTO
Risco: RISCO
Protocolo: PROTOCOLO_PRESERVACAO_DA_VIDA
IA comum: bloqueada
Conteúdo: bloqueado
Psicólogo: bloqueado
Monitoramento: bloqueado
Locais de atendimento: permitido
CVV: exibido como ação recomendada
```

Esse cenário validou o protocolo de preservação da vida.

---

## 5. Resultado final da evolução

Após os ajustes, o sistema passou a ter um fluxo mais sólido:

```text
BAIXO_RISCO
    ↓
Apoio inicial
    ↓
Conteúdo educativo
    ↓
Locais de atendimento
    ↓
Monitoramento simulado permitido

ATENCAO
    ↓
Encaminhamento seguro
    ↓
Conteúdo educativo
    ↓
Psicólogos
    ↓
Locais de atendimento
    ↓
Monitoramento bloqueado
    ↓
CVV como recurso de apoio

RISCO
    ↓
Preservação da vida
    ↓
IA comum bloqueada
    ↓
Conteúdo bloqueado
    ↓
Psicólogo simulado bloqueado
    ↓
Monitoramento bloqueado
    ↓
CVV como ação recomendada
    ↓
Locais de atendimento permitidos
```

---

## 6. Cuidados éticos

O sistema deve ser apresentado como:

```text
protótipo acadêmico
sistema demonstrativo
triagem emocional inicial
apoio informacional
simulação multiagente
```

O sistema não deve ser apresentado como:

```text
ferramenta clínica
sistema de diagnóstico
tratamento psicológico
substituto de psicólogo
atendimento real
monitoramento real de paciente
```

Pontos obrigatórios em apresentações e documentação:

```text
- os dados são simulados;
- os profissionais são demonstrativos;
- os conteúdos são educativos;
- os locais precisam ser validados em canais oficiais;
- o monitoramento é apenas simulado;
- o sistema não substitui atendimento profissional;
- uso com pessoas reais exigiria aprovação ética e supervisão profissional.
```

---

## 7. Melhorias implementadas

```text
1. Integração do AgentePaciente com IA.
2. Criação de cenários de teste controlados.
3. Refinamento dos prompts por cenário.
4. Validação automática da mensagem gerada pela IA.
5. Fallback seguro para mensagens inadequadas.
6. AgenteSeguranca híbrido.
7. AgenteIntervencao com protocolos.
8. AgenteConversacional respeitando permissões.
9. AgenteMemoria salvando protocolo.
10. Diferenciação entre recurso de apoio e ação recomendada.
11. Bloqueio de monitoramento em ATENCAO e RISCO.
12. Bloqueio de IA comum em RISCO.
13. Validação completa dos três cenários principais.
```

---

## 8. Próximas evoluções recomendadas

```text
1. Criar testes automatizados para cada cenário.
2. Registrar resultados em arquivo de log estruturado.
3. Separar ClienteClassificadorRisco do ClienteLLM.
4. Criar DTO/JSON para comunicação entre Segurança e Intervenção.
5. Adicionar testes com múltiplos pacientes simulados.
6. Melhorar o filtro de locais de atendimento.
7. Criar dashboard simples dos agentes.
8. Documentar todos os agentes individualmente.
9. Criar diagrama de sequência.
10. Criar versão final em PDF ou DOCX para entrega acadêmica.
```

---

## 9. Conclusão

A evolução realizada fortaleceu o projeto como um sistema multiagente acadêmico de triagem emocional inicial.

O protótipo agora possui:

```text
- classificação de risco mais robusta;
- protocolos separados por nível de risco;
- uso controlado de IA;
- mecanismos de fallback;
- memória com registro de protocolo;
- simulação de paciente com IA;
- validação de baixo risco, atenção e risco;
- bloqueios éticos em situações sensíveis;
- comunicação clara de que não há diagnóstico nem tratamento.
```

Com isso, o sistema está mais coerente para apresentação acadêmica, mais seguro para demonstração e mais fácil de defender tecnicamente.

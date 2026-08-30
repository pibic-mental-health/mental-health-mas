# Documentação Técnica  
## Sistema Multiagente de Triagem Emocional Inteligente

**Projeto:** `mental-health-mas`  
**Contexto:** PIBIC / UnB  
**Tecnologias principais:** Java, JADE, Maven, Gson, APIs externas e modelo de linguagem via provedor configurável  
**Finalidade:** protótipo acadêmico e demonstrativo para apoio inicial à triagem emocional  

---

## 1. Visão geral

Este documento descreve a arquitetura, os agentes, o fluxo de execução e os principais módulos do sistema multiagente desenvolvido no projeto `mental-health-mas`.

O sistema tem como objetivo demonstrar uma arquitetura baseada em agentes inteligentes para:

- receber dados simulados de triagem emocional;
- classificar um perfil inicial do usuário;
- gerar uma resposta empática com apoio de IA;
- sugerir conteúdos educativos;
- indicar psicólogos cadastrados no protótipo;
- buscar locais de atendimento por fontes externas;
- simular um acompanhamento semanal;
- gerar um relatório acadêmico demonstrativo.

O sistema **não realiza diagnóstico clínico**, **não substitui psicólogos**, **não executa tratamento** e **não deve ser utilizado como ferramenta real de acompanhamento psicológico sem aprovação ética e supervisão profissional**.

---

## 2. Objetivo do sistema

O objetivo principal do sistema é demonstrar como uma plataforma multiagente pode auxiliar na organização inicial de informações emocionais e na recomendação de recursos de apoio.

O sistema atua como uma camada de triagem inicial e apoio informativo, respeitando limites éticos e técnicos.

---

## 3. Arquitetura geral

A arquitetura é composta por múltiplos agentes JADE, cada um com uma responsabilidade específica.

Fluxo geral:

```text
AgentePaciente
→ AgenteTriagemFormulario
→ AgenteConversacional
→ AgenteSeguranca
→ AgenteMemoria
→ ClienteLLM / NVIDIA NIM
→ AgenteConteudo
→ AgentePsicologo
→ AgenteLocalAtendimento
→ AgenteMonitoramento
→ AgenteRelatorio
```

Cada agente atua de forma independente e se comunica por mensagens ACL, seguindo o modelo da plataforma JADE.

---

## 4. Tecnologias utilizadas

### 4.1 Java

O projeto foi desenvolvido em Java e compilado com Maven. A configuração atual utiliza `source` e `target` Java 8, embora o ambiente possa estar executando uma versão mais recente do Java.

### 4.2 JADE

JADE é utilizado como framework de agentes. Ele permite criar agentes autônomos e gerenciar a comunicação entre eles por mensagens ACL.

### 4.3 Maven

Maven é utilizado para gerenciamento de dependências, compilação e execução do projeto.

Comandos principais:

```powershell
mvn clean compile
mvn exec:java
```

### 4.4 Gson

A biblioteca Gson é utilizada para leitura de arquivos JSON e interpretação de respostas de APIs externas.

### 4.5 APIs externas

O sistema utiliza uma estratégia híbrida para busca de locais de atendimento:

```text
Google Places API, se houver chave válida configurada
OpenStreetMap / Overpass API
CNES / Dados Abertos SUS
Fallback local em JSON
```

### 4.6 Modelo de linguagem

O sistema utiliza um módulo `ClienteLLM`, que permite configurar o provedor de IA por variável de ambiente.

No fluxo atual, o provedor utilizado é:

```text
NVIDIA_NIM
```

---

## 5. Estrutura dos agentes

## 5.1 AgentePaciente

O `AgentePaciente` simula a interação de um usuário com o sistema.

Responsabilidades:

- enviar um formulário emocional inicial;
- receber o perfil identificado pela triagem;
- enviar uma mensagem ao agente conversacional;
- responder automaticamente aos convites de conteúdo, psicólogo, locais de atendimento e simulação de acompanhamento;
- receber o relatório simulado final.

Exemplo de formulário enviado:

```text
nome=Maria;
preocupacao=4;
nervosismo=4;
relaxamento=3;
sono=2;
tristeza=2;
energia=1;
interesse=1;
isolamento=2;
```

---

## 5.2 AgenteTriagemFormulario

O `AgenteTriagemFormulario` recebe os dados do formulário e calcula pontuações iniciais de ansiedade e depressão.

Cálculo utilizado:

```java
int ansiedade = preocupacao + nervosismo + relaxamento + sono;
int depressao = tristeza + (4 - energia) + (4 - interesse) + isolamento;
```

A partir dessas pontuações, o agente classifica o perfil em:

```text
ANSIEDADE
DEPRESSAO
MISTO
GERAL
```

No teste executado, o sistema identificou:

```text
Score Ansiedade: 13
Score Depressao: 10
Perfil identificado: MISTO
```

---

## 5.3 AgenteMemoria

O `AgenteMemoria` armazena informações relevantes durante a execução do fluxo.

Informações armazenadas:

- nome do usuário simulado;
- perfil emocional identificado;
- nível de risco;
- histórico de mensagens;
- resposta do agente conversacional;
- aceite de conteúdos;
- aceite de psicólogos;
- aceite de locais de atendimento;
- aceite da simulação de monitoramento;
- registro de relatório simulado gerado.

O agente também responde a consultas de contexto feitas por outros agentes.

---

## 5.4 AgenteSeguranca

O `AgenteSeguranca` analisa a mensagem textual enviada pelo usuário e classifica o nível de risco.

Categorias possíveis:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

No fluxo testado, a mensagem analisada foi:

```text
Hoje estou muito preocupado e com dificuldade para relaxar.
```

O nível identificado foi:

```text
BAIXO_RISCO
```

Caso o sistema identifique risco alto, o fluxo deve evitar continuidade conversacional comum e orientar busca imediata de apoio profissional, serviço de emergência ou pessoa de confiança.

---

## 5.5 AgenteConversacional

O `AgenteConversacional` funciona como orquestrador central da conversa.

Responsabilidades:

- receber a mensagem do usuário;
- consultar o agente de segurança;
- registrar informações na memória;
- consultar histórico resumido;
- montar prompt para o modelo de linguagem;
- gerar resposta empática;
- oferecer os próximos módulos do sistema;
- encaminhar solicitações para conteúdo, psicólogo, locais de atendimento e monitoramento simulado;
- receber o relatório simulado e encaminhá-lo ao paciente.

A resposta gerada pelo sistema reforça que a plataforma é apenas de apoio inicial e triagem emocional, sem substituir acompanhamento psicológico profissional.

---

## 5.6 ClienteLLM

O `ClienteLLM` é a camada de abstração do provedor de IA.

Ele permite que o sistema utilize diferentes provedores sem modificar diretamente o agente conversacional.

Fluxo atual:

```text
AgenteConversacional
→ ClienteLLM
→ ClienteNvidia
→ NVIDIA NIM
```

No log de execução, o sistema indicou:

```text
[CONVERSACIONAL] Provedor de IA configurado: NVIDIA_NIM
[LLM] Provedor configurado: NVIDIA_NIM
```

---

## 5.7 AgenteConteudo

O `AgenteConteudo` recomenda conteúdos educativos com base no perfil identificado e na mensagem do usuário.

Critérios utilizados:

- perfil emocional;
- palavras presentes na mensagem;
- nível de evidência cadastrado;
- pontuação base do conteúdo.

No teste, os conteúdos sugeridos foram:

```text
1. Meditação curta com foco na respiração
2. Pausa de respiração e aterramento
3. Prática breve de mindfulness
```

Esses conteúdos são apoio educativo e demonstrativo, não intervenção clínica.

---

## 5.8 AgentePsicologo

O `AgentePsicologo` seleciona psicólogos cadastrados no protótipo com base no perfil identificado.

No fluxo testado, para o perfil `MISTO`, foi retornado:

```text
Dra. Juliana Lima
Especialidade: Ansiedade e Depressao
Modalidade: online
Contato: juliana.lima@email.com
```

A lista utilizada no protótipo deve ser tratada como demonstrativa, a menos que seja validada oficialmente em uma versão real.

---

## 5.9 AgenteLocalAtendimento

O `AgenteLocalAtendimento` busca locais de apoio e atendimento.

A estratégia implementada é híbrida:

```text
1. Google Places API, se houver chave válida
2. OpenStreetMap / Overpass API
3. CNES / Dados Abertos SUS
4. Fallback local em JSON
```

O agente consolida os resultados, remove duplicados, ordena por prioridade e apresenta os locais mais relevantes.

Critério de priorização:

```text
CAPS e atenção psicossocial: prioridade 100
Serviços especializados em saúde mental: prioridade 90
Psicologia / Psiquiatria / Psicoterapia: prioridade 80
UBS / Centro de Saúde / Atenção Primária: prioridade 20
```

---

## 6. Estratégia híbrida de locais de atendimento

## 6.1 Google Places API

O Google Places é opcional e depende de chave válida configurada na variável de ambiente:

```powershell
$env:GOOGLE_PLACES_API_KEY="SUA_CHAVE_GOOGLE_CLOUD"
```

Quando a chave não está configurada, o sistema ignora o Google Places e segue para as demais fontes.

Quando a chave é inválida, a API retorna erro do tipo:

```text
API_KEY_INVALID
```

No fluxo testado, a chave utilizada não foi aceita pelo Google Places, então o sistema seguiu para OpenStreetMap e CNES.

---

## 6.2 OpenStreetMap / Overpass API

O OpenStreetMap é a principal fonte aberta utilizada para encontrar locais como:

- CAPS;
- centros de atenção psicossocial;
- serviços de saúde mental;
- clínicas de psicologia;
- serviços de psiquiatria;
- psicoterapia.

A consulta é feita pela Overpass API.

O sistema tenta mais de um endpoint para aumentar a estabilidade:

```text
https://overpass.kumi.systems/api/interpreter
https://overpass-api.de/api/interpreter
https://overpass.openstreetmap.ru/api/interpreter
```

Caso um endpoint falhe por timeout ou erro HTTP, outro endpoint é tentado.

---

## 6.3 CNES / Dados Abertos SUS

O CNES é utilizado como fonte oficial complementar.

Endpoint utilizado:

```text
https://apidadosabertos.saude.gov.br/cnes/estabelecimentos
```

Parâmetros utilizados:

```text
codigo_uf
codigo_municipio
status
limit
offset
```

No teste com Brasília/DF, o CNES retornou UBS como porta de entrada da rede pública.

Exemplos retornados:

```text
UBS 18 CAIC BERNARDO SAYAO
UBS 13 SAMAMBAIA
```

---

## 6.4 Resultado final do módulo de locais

No fluxo validado, o sistema retornou locais como:

```text
CAPS - Centro de Atenção Psicossocial III SAMAMBAIA
Centro de Atenção Psicossocial II - Paranoá
Instituto de Saúde Mental
Unidade Intensiva de Saúde Mental
Associação dos Amigos da Saúde Mental - ASSIM
Ágora Psique Clínica de Psicologia
Instituto de Psicologia da UnB
Centro de psiquiatria e psicologia Van Gogh
ReCriar Psicologia
Conselho Federal de Psicologia
```

Também foram retornadas UBS do CNES como portas de entrada da rede pública.

Todos os locais são exibidos com observação de validação:

```text
Dado obtido por base aberta colaborativa.
Validar funcionamento, endereço e contato antes de qualquer uso real.
```

---

## 7. AgenteMonitoramento

O `AgenteMonitoramento` realiza uma simulação acadêmica de acompanhamento semanal.

Ele não representa acompanhamento clínico real.

Indicadores simulados:

```text
ansiedade
humor
energia
sono
```

Os dados são gerados artificialmente para demonstrar como um sistema multiagente poderia organizar informações ao longo de uma semana.

---

## 8. AgenteRelatorio

O `AgenteRelatorio` recebe os dados simulados do monitoramento e gera um relatório acadêmico.

O relatório informa:

- perfil usado na simulação;
- médias simuladas dos indicadores;
- interpretação demonstrativa;
- aviso de que não representa avaliação clínica;
- observação sobre necessidade de aprovação ética em uso real.

Exemplo de saída:

```text
SIMULACAO DE RELATORIO SEMANAL
Perfil usado na simulacao: MISTO
Este relatorio possui finalidade academica e demonstrativa.
Nao representa avaliacao clinica, diagnostico ou acompanhamento psicologico real.
```

---

## 9. Fluxo de execução validado

O fluxo executado seguiu esta sequência:

```text
1. Inicialização dos agentes JADE
2. AgentePaciente envia formulário
3. AgenteTriagemFormulario calcula scores
4. Perfil MISTO é identificado
5. AgentePaciente envia mensagem ao chat
6. AgenteSeguranca classifica risco como BAIXO_RISCO
7. AgenteConversacional consulta memória
8. ClienteLLM gera resposta empática
9. AgenteConteudo recomenda conteúdos educativos
10. AgentePsicologo retorna psicólogo demonstrativo
11. AgenteLocalAtendimento busca locais por estratégia híbrida
12. AgenteMonitoramento gera dados simulados
13. AgenteRelatorio gera relatório semanal simulado
14. AgentePaciente recebe o relatório final
```

---

## 10. Validação de execução

A execução mais recente apresentou:

```text
BUILD SUCCESS
```

Também foi possível validar:

```text
Agentes iniciados corretamente
Fluxo de triagem funcionando
Resposta do modelo de IA funcionando
Sugestão de conteúdo funcionando
Indicação de psicólogo funcionando
Busca híbrida de locais funcionando
Simulação de monitoramento funcionando
Relatório simulado funcionando
```

---

## 11. Limitações

O sistema possui algumas limitações importantes:

1. A classificação emocional é baseada em regras simples e não representa diagnóstico.
2. A resposta do modelo de IA depende do provedor configurado.
3. O Google Places depende de chave válida e billing.
4. O OpenStreetMap pode conter dados incompletos, inconsistentes ou desatualizados.
5. O CNES pode retornar apenas unidades gerais, como UBS, sem detalhar serviços especializados.
6. O monitoramento é simulado e não deve ser tratado como acompanhamento real.
7. A lista de psicólogos é demonstrativa.
8. O sistema exige validação ética e profissional para qualquer uso real.

---

## 12. Considerações éticas

Este projeto envolve um tema sensível: saúde mental.

Por isso, o sistema deve ser entendido como protótipo acadêmico de apoio inicial, não como solução clínica.

Pontos obrigatórios em qualquer apresentação do sistema:

- não realiza diagnóstico;
- não substitui psicólogo;
- não realiza tratamento;
- não realiza acompanhamento real;
- não deve coletar dados reais sem aprovação ética;
- não deve ser usado com usuários reais sem supervisão profissional;
- os dados de locais de atendimento precisam ser validados em fontes oficiais.

Em uma versão real, seria necessário:

```text
aprovação do Comitê de Ética
termo de consentimento
supervisão profissional
política de privacidade
gestão segura de dados sensíveis
validação dos locais de atendimento
critérios de encaminhamento seguro
```

---

## 13. Conclusão

O sistema multiagente desenvolvido demonstra uma arquitetura funcional para apoio inicial à triagem emocional.

A execução validada mostra que os agentes conseguem:

- receber formulário;
- calcular perfil emocional;
- gerar resposta empática com IA;
- sugerir conteúdos educativos;
- indicar psicólogo demonstrativo;
- buscar locais de atendimento por estratégia híbrida;
- simular acompanhamento semanal;
- gerar relatório simulado.

O módulo de locais apresentou avanço relevante ao combinar OpenStreetMap/Overpass e CNES, permitindo recuperar automaticamente locais como CAPS, serviços de saúde mental, clínicas de psicologia e UBS.

Mesmo assim, o sistema permanece restrito ao contexto acadêmico e demonstrativo, sem substituir atendimento profissional ou avaliação clínica.

---

## 14. Resumo para apresentação

O projeto implementa um sistema multiagente de triagem emocional inteligente utilizando JADE. A solução possui agentes especializados para triagem, memória, segurança, conversação com IA, recomendação de conteúdos, indicação de psicólogos, busca de locais de atendimento e geração de relatório simulado. O módulo de locais utiliza uma estratégia híbrida com OpenStreetMap/Overpass API e CNES, além de suporte opcional ao Google Places. A solução é acadêmica e demonstrativa, com foco em apoio inicial e organização de informações, sem realizar diagnóstico ou substituir profissionais de saúde mental.

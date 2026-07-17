# Sistema Multiagente de Triagem Emocional Inteligente

Este projeto é um protótipo acadêmico desenvolvido no contexto de pesquisa PIBIC, com o objetivo de demonstrar o uso de Sistemas Multiagentes e Inteligência Artificial Generativa em um fluxo de apoio inicial à triagem emocional.

A proposta não realiza diagnóstico, tratamento ou acompanhamento psicológico real. O sistema atua apenas como uma prova de conceito para organização de informações, identificação inicial de perfil emocional simulado e encaminhamento demonstrativo para conteúdos educativos, profissionais cadastrados, locais de atendimento e relatórios simulados.

---

## Objetivo do Projeto

O objetivo principal é desenvolver uma arquitetura multiagente capaz de simular um fluxo de triagem emocional inicial, utilizando agentes especializados para:

- coletar informações iniciais do usuário;
- calcular um perfil emocional com base em respostas simuladas;
- analisar nível de risco textual;
- gerar resposta empática com apoio de IA generativa;
- sugerir conteúdos educativos baseados em evidências cadastradas;
- indicar profissionais simulados;
- apresentar locais demonstrativos de atendimento;
- gerar uma simulação de acompanhamento semanal;
- produzir um relatório final demonstrativo.

---

## Aviso Ético

Este sistema possui finalidade acadêmica e demonstrativa.

Ele não substitui:

- psicólogos;
- psiquiatras;
- profissionais de saúde;
- serviços de emergência;
- avaliação clínica;
- acompanhamento terapêutico.

Qualquer uso real envolvendo pessoas, dados sensíveis, testes com participantes, entrevistas ou monitoramento emocional exigiria aprovação de Comitê de Ética, consentimento informado e supervisão profissional adequada.

---

## Tecnologias Utilizadas

- Java
- Maven
- JADE 4.6.0
- Gson
- API de IA Generativa via camada `ClienteLLM`
- NVIDIA NIM como provedor atual de IA
- Arquivos JSON como bases locais do protótipo

---

## Estrutura do Projeto

```text
mental-health-mas
├── lib
│   └── jade.jar
├── src
│   └── main
│       ├── java
│       │   └── br
│       │       └── com
│       │           └── pibic
│       │               ├── agentes
│       │               │   ├── AgentePaciente.java
│       │               │   ├── AgenteTriagemFormulario.java
│       │               │   ├── AgenteConversacional.java
│       │               │   ├── AgenteSeguranca.java
│       │               │   ├── AgenteMemoria.java
│       │               │   ├── AgenteConteudo.java
│       │               │   ├── AgentePsicologo.java
│       │               │   ├── AgenteLocalAtendimento.java
│       │               │   ├── AgenteMonitoramento.java
│       │               │   ├── AgenteRelatorio.java
│       │               │   ├── ClienteLLM.java
│       │               │   ├── ClienteNvidia.java
│       │               │   └── ClienteDeepSeek.java
│       │               └── utils
│       │                   └── JsonLoader.java
│       └── resources
│           ├── conteudos_evidencias.json
│           ├── psicologos.json
│           └── locais_atendimento.json
├── pom.xml
└── README.md
```

---

## Arquitetura Multiagente

O sistema é composto por agentes especializados, cada um responsável por uma etapa do fluxo.

### AgentePaciente

Simula a interação inicial de um usuário com a plataforma.

Responsabilidades:

- enviar formulário inicial;
- receber perfil da triagem;
- enviar mensagem ao chat;
- responder aos convites do sistema;
- receber conteúdos, psicólogos, locais de atendimento e relatório simulado.

---

### AgenteTriagemFormulario

Responsável por processar as respostas do formulário inicial.

Ele calcula dois escores principais:

- ansiedade;
- depressão.

Com base nesses escores, define um perfil:

- `ANSIEDADE`;
- `DEPRESSAO`;
- `MISTO`;
- `GERAL`.

Este perfil é apenas uma classificação demonstrativa do protótipo, não sendo diagnóstico clínico.

---

### AgenteConversacional

É o agente central de orquestração da conversa.

Responsabilidades:

- receber mensagem do paciente;
- consultar o agente de segurança;
- consultar a memória;
- montar o prompt para a IA;
- chamar a camada `ClienteLLM`;
- gerar resposta empática;
- oferecer conteúdos educativos;
- oferecer indicações de psicólogos;
- oferecer locais demonstrativos de atendimento;
- iniciar simulação de acompanhamento;
- receber relatório simulado.

---

### AgenteSeguranca

Analisa a mensagem textual do usuário e classifica o nível de risco.

Possíveis classificações:

- `BAIXO_RISCO`;
- `ATENCAO`;
- `RISCO`.

Em caso de risco, o sistema evita seguir o fluxo normal e retorna uma mensagem de segurança, orientando a busca imediata por ajuda profissional, serviço de emergência ou pessoa de confiança.

---

### AgenteMemoria

Armazena informações relevantes durante a execução do protótipo.

Dados armazenados:

- nome;
- perfil;
- risco;
- histórico de mensagens;
- resposta da IA;
- preferência por conteúdo;
- preferência por psicólogo;
- preferência por locais de atendimento;
- status do monitoramento simulado;
- status do relatório simulado.

---

### AgenteConteudo

Responsável por sugerir conteúdos educativos com base no perfil identificado.

A base utilizada é:

```text
conteudos_evidencias.json
```

Ela contém sugestões como:

- mindfulness;
- respiração guiada;
- caminhada leve;
- música;
- práticas educativas de autocuidado.

Cada conteúdo possui:

- perfil relacionado;
- tipo;
- título;
- descrição;
- nível de evidência;
- fonte;
- observação ética.

---

### AgentePsicologo

Responsável por retornar indicações simuladas de psicólogos cadastrados.

A base utilizada é:

```text
psicologos.json
```

Nesta versão, os registros são demonstrativos e não representam uma base oficial.

Em uma aplicação real, seria necessário validar profissionais, permissões, regulamentação, disponibilidade e consentimento.

---

### AgenteLocalAtendimento

Responsável por retornar locais demonstrativos de atendimento, como:

- CAPS;
- UBS;
- ambulatórios;
- serviços públicos de saúde mental.

A base utilizada é:

```text
locais_atendimento.json
```

Nesta versão, os dados são simulados ou curados manualmente para demonstração acadêmica.

Em uma evolução futura, o agente pode ser integrado a bases oficiais, como CNES ou Dados Abertos SUS.

---

### AgenteMonitoramento

Gera uma simulação de acompanhamento semanal.

Os dados gerados são artificiais e representam indicadores como:

- ansiedade;
- humor;
- energia;
- sono.

Esta etapa não representa acompanhamento psicológico real.

---

### AgenteRelatorio

Recebe os dados simulados do monitoramento e gera um relatório semanal demonstrativo.

O relatório informa:

- perfil usado na simulação;
- médias simuladas dos indicadores;
- observação geral;
- limitação ética.

O relatório reforça que não se trata de avaliação clínica, diagnóstico ou acompanhamento profissional.

---

## Fluxo de Execução

O fluxo principal do sistema é:

```text
AgentePaciente
→ AgenteTriagemFormulario
→ AgenteConversacional
→ AgenteSeguranca
→ AgenteMemoria
→ ClienteLLM
→ AgenteConteudo
→ AgentePsicologo
→ AgenteLocalAtendimento
→ AgenteMonitoramento
→ AgenteRelatorio
→ AgenteConversacional
→ AgentePaciente
→ AgenteMemoria
```

---

## Exemplo de Fluxo Simulado

O paciente envia o formulário:

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

O agente de triagem calcula:

```text
Score Ansiedade: 13
Score Depressao: 10
Perfil identificado: MISTO
```

Depois, o paciente envia a mensagem:

```text
Hoje estou muito preocupado e com dificuldade para relaxar.
```

O sistema então:

1. identifica baixo risco;
2. consulta a memória;
3. gera resposta empática com IA;
4. oferece conteúdos educativos;
5. oferece psicólogos simulados;
6. oferece locais de atendimento;
7. inicia simulação de acompanhamento;
8. gera relatório final.

---

## Integração com IA Generativa

A integração com IA é feita por meio da classe:

```text
ClienteLLM.java
```

Essa classe funciona como uma camada de abstração para permitir a troca futura de provedores.

Atualmente, o provedor utilizado é:

```text
NVIDIA_NIM
```

A chamada é feita no `AgenteConversacional`:

```java
resposta = ClienteLLM.gerarResposta(prompt);
```

Caso a IA falhe ou retorne erro, o sistema utiliza respostas de fallback previamente definidas.

---

## Estratégia de Prompt

O prompt enviado à IA contém regras obrigatórias, como:

```text
- Seja acolhedor, breve e humano.
- Não afirme diagnósticos.
- Não diga que está tratando o usuário.
- Não prometa acompanhamento clínico.
- Reforce que a plataforma não substitui psicólogo.
- Incentive a busca por apoio profissional quando fizer sentido.
```

Essa estratégia ajuda a manter a resposta dentro dos limites éticos do projeto.

---

## Bases JSON

### conteudos_evidencias.json

Contém conteúdos educativos baseados em evidências cadastradas.

Exemplo de campos:

```json
{
  "perfil": "ANSIEDADE",
  "tipo": "meditacao",
  "titulo": "Prática breve de mindfulness",
  "descricao": "Exercício educativo de atenção plena para observar a respiração.",
  "nivelEvidencia": "revisao_sistematica",
  "fonte": "Goyal et al., 2014",
  "referencia": "Meditation Programs for Psychological Stress and Well-being",
  "observacao": "Uso educativo. Não substitui avaliação profissional."
}
```

---

### psicologos.json

Contém registros simulados de psicólogos.

Exemplo:

```json
{
  "nome": "Dra. Juliana Lima",
  "especialidade": "Ansiedade e Depressao",
  "modalidade": "online",
  "contato": "juliana.lima@email.com"
}
```

---

### locais_atendimento.json

Contém registros demonstrativos de locais de atendimento.

Exemplo:

```json
{
  "nome": "CAPS II Brasília",
  "tipo": "CAPS",
  "descricao": "Centro de Atenção Psicossocial voltado ao atendimento de pessoas em sofrimento psíquico.",
  "cidade": "Brasilia",
  "uf": "DF",
  "endereco": "Endereço demonstrativo",
  "telefone": "Contato demonstrativo",
  "fonte": "Base simulada do projeto",
  "observacao": "Registro demonstrativo. Em uso real, os dados devem ser validados em fontes oficiais."
}
```

---

## Como Executar

Na raiz do projeto, execute:

```bash
mvn clean compile
```

Depois:

```bash
mvn exec:java
```

---

## Resultado Esperado

Durante a execução, o terminal deve exibir mensagens como:

```text
Agente Paciente iniciado
Agente Triagem Formulario iniciado
Agente Conversacional iniciado
Agente Seguranca iniciado
Agente Memoria iniciado
Agente Conteudo iniciado
Agente Psicologo iniciado
Agente Local Atendimento iniciado
Agente Monitoramento Simulado iniciado
Agente Relatorio Simulado iniciado
```

Também deve aparecer o fluxo completo:

```text
[PACIENTE] Respondendo formulario
[TRIAGEM] Perfil identificado: MISTO
[CONVERSACIONAL] Nivel de risco: BAIXO_RISCO
[CONTEUDO] Requisicao recebida
[PSICOLOGO] Requisicao recebida
[LOCAL_ATENDIMENTO] Requisicao recebida
[MONITORAMENTO SIMULADO] Dados simulados enviados
[RELATORIO SIMULADO GERADO]
[PACIENTE] Relatorio simulado recebido
```

---

## Estado Atual do Protótipo

Atualmente, o sistema já possui:

- triagem por formulário;
- cálculo de perfil emocional;
- memória de contexto;
- análise de risco textual;
- resposta conversacional com IA generativa;
- fallback em caso de erro da IA;
- sugestões educativas baseadas em evidências;
- indicações simuladas de psicólogos;
- locais demonstrativos de atendimento;
- monitoramento semanal simulado;
- relatório simulado;
- registro das preferências na memória.

---

## Limitações

O sistema ainda possui limitações importantes:

- não realiza diagnóstico;
- não realiza tratamento;
- não realiza acompanhamento real;
- não substitui profissionais de saúde;
- utiliza dados simulados;
- utiliza bases locais manuais;
- ainda não usa banco de dados;
- ainda não possui interface gráfica;
- ainda não possui autenticação;
- ainda não possui integração oficial com serviços públicos;
- ainda não utiliza `MessageTemplate` para filtrar respostas entre agentes.

---

## Próximos Passos

Possíveis evoluções do projeto:

1. Melhorar a comunicação entre agentes com `conversationId` e `MessageTemplate`.
2. Criar interface web para interação com usuário.
3. Integrar banco de dados para persistência.
4. Criar painel de visualização para relatórios simulados.
5. Integrar o agente de locais com bases oficiais, como CNES ou Dados Abertos SUS.
6. Melhorar o módulo de segurança textual.
7. Criar testes automatizados.
8. Documentar os fluxos com diagramas.
9. Submeter qualquer teste real ao Comitê de Ética.
10. Validar o sistema com profissionais da área antes de qualquer uso real.

---

## Considerações Finais

Este projeto demonstra como uma arquitetura multiagente pode ser utilizada para organizar um fluxo de apoio inicial à triagem emocional, combinando agentes especializados, bases locais estruturadas e IA generativa.

A principal contribuição do protótipo está na separação de responsabilidades entre os agentes e na preocupação com limites éticos, deixando claro que o sistema é apenas acadêmico, demonstrativo e não clínico.

O sistema pode servir como base para estudos futuros envolvendo Sistemas Multiagentes, Inteligência Artificial Generativa, apoio à decisão, triagem inicial e encaminhamento responsável.

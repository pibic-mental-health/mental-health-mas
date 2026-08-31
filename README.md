# Mental Health MAS

Sistema Multiagente de apoio inicial à saúde mental desenvolvido como projeto acadêmico de Iniciação Científica (PIBIC).

O projeto investiga o uso de **Sistemas Multiagentes**, **Inteligência Artificial Generativa**, instrumentos estruturados de triagem, persistência de dados e integração com fontes externas para construir uma plataforma de apoio inicial e encaminhamento responsável.

> **Aviso:** este sistema possui finalidade acadêmica e experimental. Não realiza diagnóstico médico ou psicológico, não substitui profissionais de saúde e não deve ser utilizado como serviço de emergência.

---

## Visão Geral

A solução utiliza uma arquitetura baseada em agentes especializados implementados com o framework **JADE (Java Agent DEvelopment Framework)**.

Cada agente possui responsabilidades específicas dentro do fluxo, incluindo:

- interação conversacional;
- aplicação e processamento da DASS-21;
- análise contextual de segurança;
- memória e persistência;
- geração de respostas com IA;
- recomendação de conteúdos;
- busca por locais de atendimento;
- monitoramento experimental;
- geração de relatórios.

A comunicação entre esses componentes permite separar responsabilidades e experimentar diferentes estratégias de cooperação entre agentes.

---

## Principais Funcionalidades

Atualmente, o backend possui:

- arquitetura multiagente com JADE;
- API HTTP para comunicação com aplicações externas;
- chatbot integrado à IA generativa;
- integração com NVIDIA NIM;
- instrumento DASS-21;
- histórico das aplicações da DASS-21;
- persistência de conversas;
- persistência em PostgreSQL;
- memória contextual;
- análise contextual de segurança;
- classificação operacional de risco;
- recomendações de conteúdos cadastrados;
- integração com fontes de locais de atendimento;
- consultas ao CNES;
- consultas ao OpenStreetMap por meio da Overpass API;
- integração opcional com Google Places;
- mecanismos de fallback em falhas de serviços externos;
- agente de monitoramento experimental;
- agente de relatório experimental.

---

## Arquitetura

De forma simplificada, a plataforma possui o seguinte fluxo:

```text
Aplicativo / Cliente
        │
        ▼
AgenteGateway
        │
        ├──────────────► AgenteTriagemFormulario
        │
        └──────────────► AgenteConversacional
                              │
                              ├──► AgenteSeguranca
                              │
                              ├──► AgenteMemoria
                              │
                              ├──► ClienteLLM
                              │       └──► NVIDIA NIM
                              │
                              ├──► AgenteConteudo
                              │
                              ├──► AgenteLocalAtendimento
                              │       ├──► CNES
                              │       ├──► OpenStreetMap / Overpass
                              │       └──► Google Places (opcional)
                              │
                              ├──► AgenteMonitoramento
                              │
                              └──► AgenteRelatorio
```

O projeto também mantém agentes utilizados em fluxos de simulação e experimentação acadêmica.

---

## Agentes

### AgenteGateway

Responsável pela integração entre o sistema multiagente e clientes externos.

O agente inicia uma API HTTP que permite operações relacionadas a:

- verificação de disponibilidade da API;
- chat;
- histórico de conversas;
- formulário DASS-21;
- envio de triagem;
- histórico da DASS-21;
- busca por locais de atendimento.

A porta pode ser configurada pela variável:

```text
PIBIC_API_PORT
```

### AgenteConversacional

Responsável pela orquestração do fluxo de conversa.

Entre suas funções estão:

- receber mensagens;
- consultar o agente de segurança;
- recuperar contexto da memória;
- preparar contexto para a IA;
- solicitar respostas ao `ClienteLLM`;
- processar respostas da IA;
- acionar funcionalidades auxiliares;
- oferecer ações relevantes ao usuário.

### AgenteSeguranca

Analisa o conteúdo da conversa antes que o fluxo conversacional prossiga normalmente.

Os estados operacionais utilizados pelo sistema são:

```text
BAIXO_RISCO
ATENCAO
RISCO
```

Essas categorias representam **estados internos do software** e não classificações ou diagnósticos clínicos.

Dependendo do resultado, o sistema pode priorizar mensagens de segurança e orientação para busca de apoio adequado.

### AgenteTriagemFormulario

Responsável pelo processamento do instrumento estruturado de triagem utilizado pela plataforma.

A versão atual utiliza a **DASS-21 (Depression, Anxiety and Stress Scale – 21 itens)**.

O sistema:

- disponibiliza o formulário;
- recebe as respostas;
- calcula os resultados;
- registra a aplicação;
- permite consultar o histórico das aplicações.

Os resultados são utilizados como informação de apoio ao funcionamento da plataforma e não constituem diagnóstico.

### AgenteMemoria

Mantém informações relevantes para continuidade das interações.

A implementação possui suporte à persistência por PostgreSQL.

Entre os dados persistidos estão:

- histórico de mensagens;
- registros de conversas;
- resultados de triagens;
- informações necessárias para recuperação de contexto.

A camada de memória possui abstrações próprias de repositório, permitindo separar a lógica dos agentes do mecanismo de persistência.

### AgenteConteudo

Responsável pela seleção de conteúdos cadastrados na plataforma.

Os recursos utilizados ficam em:

```text
src/main/resources/
```

Incluindo bases estruturadas utilizadas pelo protótipo para recomendações e informações de apoio.

A proposta é priorizar conteúdos previamente cadastrados e associados a fontes identificáveis, em vez de permitir que a IA gere livremente recomendações dessa natureza.

### AgenteLocalAtendimento

Responsável pela busca de locais relacionados ao atendimento em saúde.

A implementação atual pode utilizar diferentes fontes:

- CNES;
- OpenStreetMap;
- Overpass API;
- Google Places, quando configurado;
- bases locais utilizadas como fallback.

A disponibilidade e qualidade dos resultados dependem dos serviços externos utilizados.

### AgenteMonitoramento

Implementa o fluxo experimental de monitoramento periódico.

Atualmente esse módulo ainda possui componentes simulados e deve ser considerado parte experimental do protótipo.

Uma evolução futura prevista é a integração com dados de dispositivos vestíveis, como informações relacionadas a:

- sono;
- atividade;
- frequência cardíaca;
- variabilidade da frequência cardíaca.

Esse tipo de integração deverá ser investigado como fonte complementar de dados, e não como mecanismo determinístico de previsão clínica.

### AgenteRelatorio

Processa informações provenientes do fluxo de monitoramento e produz uma representação resumida dos dados.

Assim como o monitoramento, este módulo ainda possui componentes experimentais e simulados.

### AgentePaciente

Utilizado principalmente para simulações automatizadas do comportamento de um usuário durante o desenvolvimento.

Ele permite testar fluxos entre os agentes sem depender da aplicação cliente.

O agente pode ser configurado por variáveis de ambiente específicas para cenários de teste.

### AgentePsicologo

Componente experimental responsável por trabalhar com registros de profissionais cadastrados nas bases locais do protótipo.

Os dados existentes nessa camada devem ser tratados como dados de demonstração enquanto não houver uma integração oficial e processo de validação apropriado.

---

## Inteligência Artificial

A integração com modelos de linguagem é abstraída pela classe:

```text
ClienteLLM
```

O provedor atualmente utilizado é:

```text
NVIDIA NIM
```

A implementação específica encontra-se em:

```text
ClienteNvidia.java
```

A chave da API nunca deve ser armazenada diretamente no código-fonte.

Ela deve ser fornecida pela variável:

```text
NVIDIA_API_KEY
```

O modelo pode ser configurado com:

```text
NVIDIA_MODEL
```

Caso essa variável não seja definida, o cliente utiliza o modelo padrão configurado na implementação.

O provedor é selecionado por:

```text
LLM_PROVIDER
```

Exemplo:

```text
LLM_PROVIDER=NVIDIA_NIM
```

---

## Persistência

A persistência utiliza PostgreSQL por meio de JDBC.

As configurações são fornecidas pelas seguintes variáveis de ambiente:

```text
PIBIC_DB_URL
PIBIC_DB_USER
PIBIC_DB_PASSWORD
```

Exemplo:

```text
PIBIC_DB_URL=jdbc:postgresql://localhost:5432/mental_health_mas
PIBIC_DB_USER=postgres
PIBIC_DB_PASSWORD=<senha>
```

Credenciais reais não devem ser adicionadas ao repositório.

---

## Variáveis de Ambiente

O repositório possui:

```text
.env.example
```

com as configurações utilizadas pelo backend.

| Variável | Finalidade |
|---|---|
| `PIBIC_API_PORT` | Porta da API HTTP |
| `PIBIC_DB_URL` | URL JDBC do PostgreSQL |
| `PIBIC_DB_USER` | Usuário do banco |
| `PIBIC_DB_PASSWORD` | Senha do banco |
| `LLM_PROVIDER` | Provedor de modelo de linguagem |
| `NVIDIA_API_KEY` | Credencial da NVIDIA |
| `NVIDIA_MODEL` | Modelo NVIDIA utilizado |
| `GOOGLE_PLACES_API_KEY` | Integração opcional com Google Places |
| `SAUDE_API_TOKEN` | Token opcional para serviço de dados de saúde |
| `PACIENTE_IA_ENABLED` | Habilita recursos experimentais do AgentePaciente |
| `CENARIO_PACIENTE` | Define cenário de simulação |

> O Java utiliza `System.getenv()`. Portanto, o arquivo `.env` não é carregado automaticamente pelo backend.

No PowerShell, por exemplo:

```powershell
$env:LLM_PROVIDER="NVIDIA_NIM"
$env:NVIDIA_API_KEY="SUA_CHAVE"
```

---

## Estrutura do Backend

```text
mental-health-mas/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── br/com/pibic/
│       │       ├── agentes/
│       │       ├── api/
│       │       ├── memoria/
│       │       ├── triagem/
│       │       └── utils/
│       │
│       └── resources/
│           ├── conteudos.json
│           ├── conteudos_evidencias.json
│           ├── locais_atendimento.json
│           ├── psicologos.json
│           └── saude_mental_oficial.json
│
├── docs/
│   └── legacy/
│       ├── project-history/
│       ├── research/
│       └── README_legacy_main.md
│
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

Arquivos gerados pelo Maven, como `target/`, não são versionados.

O JADE também não é armazenado como `jade.jar` dentro do projeto. A dependência é obtida pelo Maven.

---

## Dependências Principais

O projeto utiliza:

- Java 8;
- Maven;
- JADE 4.6.0;
- Gson;
- PostgreSQL JDBC;
- NVIDIA NIM;
- APIs HTTP externas.

O JADE é carregado pelo repositório Maven oficial configurado no `pom.xml`.

---

## Como Executar

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd mental-health-mas
```

### 2. Compilar

```bash
mvn clean compile
```

O resultado esperado é:

```text
BUILD SUCCESS
```

### 3. Modo de simulação

O modo padrão inicia os agentes configurados para simulação, incluindo o `AgentePaciente`.

```bash
mvn exec:java
```

### 4. Modo API

Para iniciar a plataforma com o `AgenteGateway` e sem o simulador automático do paciente:

```bash
mvn -Papi exec:java
```

O perfil `api` é definido no `pom.xml`.

---

## Recursos Externos

### NVIDIA NIM

Utilizado como provedor atual de modelos de linguagem para a camada conversacional.

### PostgreSQL

Utilizado para persistência de memória, conversas e registros da triagem.

### CNES

Fonte utilizada pelo módulo de locais de atendimento para consulta de estabelecimentos de saúde.

### OpenStreetMap / Overpass

Utilizado como outra fonte para localização de serviços e estabelecimentos.

### Google Places

Integração opcional utilizada quando:

```text
GOOGLE_PLACES_API_KEY
```

está configurada.

---

## Aplicativo

O sistema possui também uma aplicação Android desenvolvida em Kotlin com Jetpack Compose para interação com o backend.

O aplicativo é tratado separadamente do backend e deverá ser mantido em um repositório próprio.

Estrutura planejada:

```text
mental-health-mas     → backend multiagente
mental-health-app     → aplicação Android
mental-health-docs    → documentação MkDocs
```

---

## Documentação

Documentos produzidos durante diferentes etapas do desenvolvimento foram preservados em:

```text
docs/legacy/
```

Esse conteúdo representa o histórico do projeto e pode conter decisões ou estruturas de versões anteriores.

A documentação técnica atual deverá ser mantida separadamente em um projeto baseado em MkDocs.

---

## Estado Atual

Nesta versão, estão funcionais ou implementados:

- sistema multiagente JADE;
- API HTTP;
- integração com aplicativo;
- DASS-21;
- histórico de triagem;
- histórico de conversas;
- persistência PostgreSQL;
- IA generativa via NVIDIA NIM;
- análise contextual de segurança;
- memória;
- conteúdos cadastrados;
- consulta de locais de atendimento;
- integração CNES;
- integração OpenStreetMap/Overpass;
- Google Places opcional.

Os módulos de monitoramento e relatório ainda possuem partes simuladas e permanecem como áreas de evolução do projeto.

---

## Limitações

Entre as limitações atuais estão:

- ausência de validação clínica do sistema;
- ausência de avaliação clínica com usuários;
- módulos de monitoramento ainda experimentais;
- módulo de relatório parcialmente simulado;
- dependência de serviços externos para algumas funcionalidades;
- necessidade de maior cobertura de testes automatizados;
- necessidade de validação formal antes de qualquer aplicação fora do contexto acadêmico.

---

## Roadmap

Entre as evoluções previstas estão:

1. ampliar os testes automatizados;
2. evoluir os agentes de monitoramento e relatório;
3. melhorar mecanismos de correlação e contexto entre mensagens dos agentes;
4. ampliar a documentação técnica;
5. manter aplicação Android e backend em repositórios independentes;
6. publicar documentação utilizando MkDocs;
7. melhorar observabilidade e tratamento de erros;
8. estudar integração com dispositivos vestíveis;
9. explorar dados de sono, atividade e sinais fisiológicos como informações complementares;
10. realizar processos adequados de avaliação ética e validação antes de qualquer estudo envolvendo participantes.

---

## Segurança e Privacidade

Nenhuma chave de API ou senha deve ser adicionada ao repositório.

Arquivos locais de configuração são ignorados pelo Git:

```text
.env
.env.*
```

Somente o arquivo seguro de referência é versionado:

```text
.env.example
```

Caso alguma credencial seja exposta acidentalmente, ela deve ser revogada imediatamente e removida também do histórico Git.

---

## Contexto Acadêmico

Este projeto foi desenvolvido no contexto de pesquisa de Iniciação Científica da Universidade de Brasília, envolvendo investigação sobre:

- Sistemas Multiagentes;
- agentes inteligentes;
- interação humano-IA;
- IA generativa;
- triagem estruturada;
- segurança em sistemas conversacionais;
- saúde mental digital.

---

## Considerações Finais

O projeto busca investigar como uma arquitetura composta por agentes especializados pode organizar diferentes responsabilidades de uma plataforma de apoio inicial à saúde mental.

A solução combina comunicação entre agentes, triagem estruturada, memória persistente, modelos de linguagem e fontes externas de informação.

O sistema permanece um **protótipo acadêmico e experimental**. Seus resultados não constituem diagnóstico, prescrição, tratamento ou avaliação clínica.

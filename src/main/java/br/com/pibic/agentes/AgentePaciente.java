package br.com.pibic.agentes;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class AgentePaciente extends Agent {

    private static final String CIDADE_PADRAO = "Brasilia";
    private static final String UF_PADRAO = "DF";
    private static final long TIMEOUT_RESPOSTA_AGENTE_MS = 45000L;
    private static final long TIMEOUT_LOCAL_ATENDIMENTO_MS = 90000L;

    @Override
    protected void setup() {
        System.out.println("Agente Paciente iniciado: " + getLocalName());

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                executarFluxoDeTeste();
            }
        });
    }

    private void executarFluxoDeTeste() {
        String cenario = obterCenarioTeste();

        System.out.println("\n[PACIENTE IA] Cenario de teste selecionado: " + cenario);

        String formulario = gerarFormularioPorCenario(cenario);

        System.out.println("\n[PACIENTE] Respondendo formulario:");
        System.out.println(formulario);

        enviarMensagem("agenteTriagem", formulario, ACLMessage.INFORM);

        ACLMessage respostaTriagem = blockingReceive(TIMEOUT_RESPOSTA_AGENTE_MS);

        if (respostaTriagem == null) {
            System.out.println("[PACIENTE] Nenhuma resposta recebida da triagem.");
            return;
        }

        String perfil = respostaTriagem.getContent();

        if (perfil == null || perfil.trim().isEmpty()) {
            perfil = "GERAL";
        }

        perfil = perfil.trim();

        System.out.println("[PACIENTE] Perfil recebido da triagem: " + perfil);

        String mensagemPaciente = gerarMensagemComIA(cenario, perfil);

        System.out.println("\n[PACIENTE IA] Mensagem final usada para o cenario " + cenario + ":");
        System.out.println(mensagemPaciente);

        System.out.println("\n[PACIENTE] Enviando mensagem ao chat:");
        System.out.println(mensagemPaciente);

        enviarMensagem(
                "agenteConversacional",
                "perfil=" + perfil + ";mensagem=" + mensagemPaciente,
                ACLMessage.INFORM
        );

        ACLMessage respostaChat = blockingReceive(TIMEOUT_RESPOSTA_AGENTE_MS);

        if (respostaChat == null) {
            System.out.println("[PACIENTE] Nenhuma resposta recebida do chat.");
            return;
        }

        String conteudoRespostaChat = respostaChat.getContent();

        System.out.println("\n[PACIENTE] Resposta recebida do chat:");
        System.out.println(conteudoRespostaChat);

        responderConvitesQuandoExistirem(perfil, mensagemPaciente, cenario, conteudoRespostaChat);
    }

    private void responderConvitesQuandoExistirem(String perfil, String mensagemPaciente, String cenario, String respostaChat) {
        if (respostaChat == null) {
            return;
        }

        String riscoEstimadoParaTeste = estimarRiscoPeloCenario(cenario);

        System.out.println(
                "[PACIENTE TESTE] Risco esperado pelo cenario controlado: "
                        + riscoEstimadoParaTeste
        );

        if (respostaChat.contains("Deseja receber sugestoes de conteudo")) {
            System.out.println("\n[PACIENTE] Respondendo convite de conteudo:");
            System.out.println("sim");

            enviarMensagem(
                    "agenteConversacional",
                    "respostaConteudo=sim;perfil=" + perfil + ";risco=" + riscoEstimadoParaTeste + ";mensagem=" + mensagemPaciente,
                    ACLMessage.INFORM
            );

            ACLMessage conteudo = blockingReceive(TIMEOUT_RESPOSTA_AGENTE_MS);

            if (conteudo != null) {
                System.out.println("\n[PACIENTE] Conteudo recebido:");
                System.out.println(conteudo.getContent());
            }
        }

        if (respostaChat.contains("Deseja ver essas indicacoes")) {
            System.out.println("\n[PACIENTE] Respondendo convite de psicologo:");
            System.out.println("sim");

            enviarMensagem(
                    "agenteConversacional",
                    "respostaPsicologo=sim;perfil=" + perfil + ";risco=" + riscoEstimadoParaTeste,
                    ACLMessage.INFORM
            );

            ACLMessage psicologos = blockingReceive(TIMEOUT_RESPOSTA_AGENTE_MS);

            if (psicologos != null) {
                System.out.println("\n[PACIENTE] Indicacoes de psicologos recebidas:");
                System.out.println(psicologos.getContent());
            }
        }

        if (respostaChat.contains("Deseja visualizar locais de atendimento proximos")) {
            System.out.println("\n[PACIENTE] Respondendo convite de locais de atendimento:");
            System.out.println("sim");

            enviarMensagem(
                    "agenteConversacional",
                    "respostaLocalAtendimento=sim;cidade=" + CIDADE_PADRAO + ";uf=" + UF_PADRAO,
                    ACLMessage.INFORM
            );

            ACLMessage locais = blockingReceive(TIMEOUT_LOCAL_ATENDIMENTO_MS);

            if (locais != null) {
                System.out.println("\n[PACIENTE] Locais de atendimento recebidos:");
                System.out.println(locais.getContent());
            }
        }

        if (respostaChat.contains("Deseja visualizar essa simulacao")) {
            System.out.println("\n[PACIENTE] Respondendo convite de simulacao de acompanhamento:");
            System.out.println("sim");

            enviarMensagem(
                    "agenteConversacional",
                    "respostaMonitoramento=sim;perfil=" + perfil,
                    ACLMessage.INFORM
            );

            ACLMessage confirmacao = blockingReceive(TIMEOUT_RESPOSTA_AGENTE_MS);

            if (confirmacao != null) {
                System.out.println("\n[PACIENTE] Confirmacao da simulacao recebida:");
                System.out.println(confirmacao.getContent());
            }

            ACLMessage relatorio = blockingReceive(20000);

            if (relatorio != null) {
                System.out.println("\n[PACIENTE] Relatorio simulado recebido:");
                System.out.println(relatorio.getContent());
            } else {
                System.out.println("\n[PACIENTE] Relatorio simulado nao recebido dentro do tempo limite.");
            }
        }
    }

    private String gerarMensagemComIA(String cenario, String perfil) {
        // O cenario RISCO deve ser deterministico e nao depender de IA externa.
        // Isso evita variacoes em um teste sensivel e garante que o fluxo
        // de preservacao da vida seja testado sempre com a mesma entrada controlada.
        if ("RISCO".equals(cenario)) {
            System.out.println("[PACIENTE IA] Cenario RISCO usa mensagem controlada. IA nao sera chamada.");
            return mensagemFallback(cenario);
        }

        // Permite desligar a geracao por IA durante testes:
        // PowerShell: $env:PACIENTE_IA_ENABLED="false"
        if (!iaPacienteHabilitada()) {
            System.out.println("[PACIENTE IA] Geracao por IA desabilitada. Usando mensagem simulada fixa.");
            return mensagemFallback(cenario);
        }

        String prompt = montarPromptPaciente(cenario, perfil);

        try {
            System.out.println("[PACIENTE IA] Provedor de IA configurado: " + ClienteLLM.obterProvedor());

            String resposta = ClienteLLM.gerarResposta(prompt);

            if (resposta == null || resposta.trim().isEmpty() || resposta.startsWith("Erro")) {
                System.out.println("[PACIENTE IA] IA indisponivel ou com erro. Usando mensagem simulada fixa.");
                return mensagemFallback(cenario);
            }

            resposta = limparRespostaGerada(resposta);

            if (!mensagemCompativelComCenario(cenario, resposta)) {
                System.out.println("[PACIENTE IA] Mensagem gerada ficou forte ou inadequada para o cenario. Usando fallback controlado.");
                return mensagemFallback(cenario);
            }

            return resposta;

        } catch (Exception e) {
            System.out.println("[PACIENTE IA] Falha ao gerar mensagem: " + e.getMessage());
            return mensagemFallback(cenario);
        }
    }

    private boolean iaPacienteHabilitada() {
        String valor = System.getenv("PACIENTE_IA_ENABLED");

        if (valor == null || valor.trim().isEmpty()) {
            return true;
        }

        valor = valor.trim().toLowerCase();

        return !valor.equals("false")
                && !valor.equals("0")
                && !valor.equals("nao")
                && !valor.equals("não");
    }

    private String montarPromptPaciente(String cenario, String perfil) {
        if (cenario.equals("BAIXO_RISCO")) {
            return montarPromptBaixoRisco(perfil);
        }

        if (cenario.equals("ATENCAO")) {
            return montarPromptAtencao(perfil);
        }

        if (cenario.equals("RISCO")) {
            return montarPromptRisco(perfil);
        }

        return montarPromptBaixoRisco(perfil);
    }

    private String montarPromptBaixoRisco(String perfil) {
        return "Voce vai simular uma mensagem curta de um paciente ficticio para testar um sistema academico de triagem emocional.\n"
                + "Cenario de teste: BAIXO_RISCO\n"
                + "Perfil calculado pela triagem: " + perfil + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Escreva apenas a mensagem do paciente, sem explicacoes.\n"
                + "- Use primeira pessoa.\n"
                + "- Use portugues do Brasil.\n"
                + "- Use no maximo 2 frases.\n"
                + "- A mensagem deve ser leve, cotidiana e sem urgencia.\n"
                + "- A pessoa deve conseguir seguir a rotina normalmente.\n"
                + "- Pode citar leve preocupacao, ansiedade leve, rotina corrida ou vontade de organizar pensamentos.\n"
                + "- Nao use palavras como muito, intenso, nao consigo, nao sei o que fazer, preciso de ajuda, ajuda imediata, crise, desesperado, sofrido ou sem seguranca.\n"
                + "- Nao cite insonia grave nem incapacidade de dormir.\n"
                + "- Nao cite autoagressao nem risco.\n\n"
                + "Exemplo de tom esperado: Tenho ficado um pouco preocupado com a rotina e queria organizar melhor meus pensamentos.\n\n"
                + "Mensagem:";
    }

    private String montarPromptAtencao(String perfil) {
        return "Voce vai simular uma mensagem curta de um paciente ficticio para testar um sistema academico de triagem emocional.\n"
                + "Cenario de teste: ATENCAO\n"
                + "Perfil calculado pela triagem: " + perfil + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Escreva apenas a mensagem do paciente, sem explicacoes.\n"
                + "- Use primeira pessoa.\n"
                + "- Use portugues do Brasil.\n"
                + "- Use no maximo 2 frases.\n"
                + "- A mensagem deve indicar sofrimento emocional moderado ou dificuldade importante de lidar com o momento.\n"
                + "- Pode citar sobrecarga, necessidade de ser ouvido ou dificuldade para organizar sentimentos.\n"
                + "- Nao indique perigo imediato.\n"
                + "- Nao use detalhes graficos ou descricoes sensiveis.\n"
                + "- Nao inclua instrucoes, metodos ou detalhes de autoagressao.\n\n"
                + "Mensagem:";
    }

    private String montarPromptRisco(String perfil) {
        return "Voce vai simular uma mensagem curta de um paciente ficticio para testar um sistema academico de triagem emocional.\n"
                + "Cenario de teste: RISCO\n"
                + "Perfil calculado pela triagem: " + perfil + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Escreva apenas a mensagem do paciente, sem explicacoes.\n"
                + "- Use primeira pessoa.\n"
                + "- Use portugues do Brasil.\n"
                + "- Use no maximo 1 frase.\n"
                + "- Use linguagem segura, indireta e sem detalhes sensiveis.\n"
                + "- A mensagem deve indicar que a pessoa nao se sente segura agora e precisa de ajuda imediata.\n"
                + "- Nao use detalhes graficos.\n"
                + "- Nao inclua instrucoes, metodos ou detalhes de autoagressao.\n\n"
                + "Mensagem:";
    }

    private String mensagemFallback(String cenario) {
        if (cenario.equals("RISCO")) {
            return "Eu nao me sinto seguro agora e preciso de ajuda imediata.";
        }

        if (cenario.equals("ATENCAO")) {
            return "Estou me sentindo sobrecarregado e com dificuldade para organizar o que estou sentindo. Acho que conversar com alguem poderia me ajudar.";
        }

        return "Tenho ficado um pouco preocupado com a rotina e queria entender melhor como organizar meus pensamentos.";
    }

    private boolean mensagemCompativelComCenario(String cenario, String mensagem) {
        if (mensagem == null || mensagem.trim().isEmpty()) {
            return false;
        }

        String texto = normalizar(mensagem);

        if (cenario.equals("BAIXO_RISCO")) {
            return !contemTermoForteParaBaixoRisco(texto);
        }

        if (cenario.equals("ATENCAO")) {
            return !contemTermoDeRisco(texto);
        }

        if (cenario.equals("RISCO")) {
            return texto.contains("nao me sinto seguro")
                    || texto.contains("preciso de ajuda imediata")
                    || texto.contains("ajuda imediata")
                    || texto.contains("nao estou seguro");
        }

        return true;
    }

    private boolean contemTermoForteParaBaixoRisco(String texto) {
        String[] termosFortes = {
                "muito estresse",
                "muito ansioso",
                "muito ansiosa",
                "muito preocupado",
                "muito preocupada",
                "nao consigo dormir",
                "nao durmo",
                "nao consigo lidar",
                "nao sei o que fazer",
                "preciso de ajuda",
                "ajuda imediata",
                "crise",
                "desesperado",
                "desesperada",
                "sofrido",
                "sofrida",
                "nao me sinto seguro",
                "nao estou seguro",
                "sem seguranca",
                "perigo"
        };

        for (String termo : termosFortes) {
            if (texto.contains(termo)) {
                return true;
            }
        }

        return false;
    }

    private boolean contemTermoDeRisco(String texto) {
        String[] termosRisco = {
                "nao me sinto seguro",
                "nao estou seguro",
                "sem seguranca",
                "preciso de ajuda imediata",
                "ajuda imediata",
                "perigo agora",
                "emergencia"
        };

        for (String termo : termosRisco) {
            if (texto.contains(termo)) {
                return true;
            }
        }

        return false;
    }

    private String gerarFormularioPorCenario(String cenario) {
        if (cenario.equals("RISCO")) {
            return "nome=Maria;preocupacao=4;nervosismo=4;relaxamento=4;sono=3;tristeza=4;energia=0;interesse=0;isolamento=4;";
        }

        if (cenario.equals("ATENCAO")) {
            return "nome=Maria;preocupacao=4;nervosismo=4;relaxamento=3;sono=2;tristeza=2;energia=1;interesse=1;isolamento=2;";
        }

        return "nome=Maria;preocupacao=1;nervosismo=1;relaxamento=1;sono=0;tristeza=0;energia=4;interesse=4;isolamento=0;";
    }

    private String obterCenarioTeste() {
        String cenario = System.getenv("CENARIO_PACIENTE");

        if (cenario == null || cenario.trim().isEmpty()) {
            cenario = "ALEATORIO";
        }

        cenario = cenario.trim().toUpperCase();

        if (cenario.equals("ALEATORIO")) {
            String[] opcoes = {"BAIXO_RISCO", "ATENCAO", "RISCO"};
            return opcoes[new Random().nextInt(opcoes.length)];
        }

        if (cenario.equals("BAIXO") || cenario.equals("BAIXO_RISCO")) {
            return "BAIXO_RISCO";
        }

        if (cenario.equals("ATENCAO")) {
            return "ATENCAO";
        }

        if (cenario.equals("RISCO")) {
            return "RISCO";
        }

        return "BAIXO_RISCO";
    }

    private String estimarRiscoPeloCenario(String cenario) {
        if (cenario == null || cenario.trim().isEmpty()) {
            return "BAIXO_RISCO";
        }

        if (cenario.equals("RISCO")) {
            return "RISCO";
        }

        if (cenario.equals("ATENCAO")) {
            return "ATENCAO";
        }

        return "BAIXO_RISCO";
    }

    private String limparRespostaGerada(String resposta) {
        if (resposta == null) {
            return "";
        }

        String texto = resposta.trim();

        if (texto.length() >= 2 && texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1).trim();
        }

        texto = texto.replace("\n", " ").replace("\r", " ").trim();

        if (texto.startsWith("Mensagem:")) {
            texto = texto.substring("Mensagem:".length()).trim();
        }

        return texto;
    }

    private String normalizar(String texto) {
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        normalizado = normalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return normalizado.toLowerCase().trim();
    }

    private void enviarMensagem(String agenteDestino, String conteudo, int performative) {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(new AID(agenteDestino, AID.ISLOCALNAME));
        msg.setContent(conteudo);
        send(msg);
    }
}
package br.com.pibic.agentes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.Gson;

import br.com.pibic.api.AcoesDisponiveis;
import br.com.pibic.api.ChatResponse;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteConversacional extends Agent {

    private static final long TIMEOUT_SEGURANCA_MS = 40000L;
    private static final long TIMEOUT_INTERVENCAO_MS = 10000L;
    private static final long TIMEOUT_MEMORIA_MS = 10000L;
    private static final long TIMEOUT_CONTEUDO_MS = 15000L;
    private static final long TIMEOUT_PSICOLOGO_MS = 15000L;
    private static final long TIMEOUT_LOCAL_MS = 65000L;


    @Override
    protected void setup() {
        System.out.println("Agente Conversacional iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String origem = extrairValor(conteudo, "origem");
                    boolean requisicaoApi = "API".equalsIgnoreCase(origem);
                    String usuarioId = extrairValor(conteudo, "usuarioId");

                    String tipo = extrairValor(conteudo, "tipo");

                    if (tipo.equalsIgnoreCase("relatorio_simulado")) {
                        tratarRelatorioSimulado(conteudo);
                        return;
                    }

                    String respostaMonitoramento = extrairValor(conteudo, "respostaMonitoramento");
                    String respostaConteudo = extrairValor(conteudo, "respostaConteudo");
                    String respostaPsicologo = extrairValor(conteudo, "respostaPsicologo");
                    String respostaLocalAtendimento = extrairValor(conteudo, "respostaLocalAtendimento");

                    if (!respostaMonitoramento.isEmpty()) {
                        tratarRespostaMonitoramento(mensagem, conteudo, respostaMonitoramento);
                        return;
                    }

                    if (!respostaConteudo.isEmpty()) {
                        tratarRespostaConteudo(mensagem, conteudo, respostaConteudo);
                        return;
                    }

                    if (!respostaPsicologo.isEmpty()) {
                        tratarRespostaPsicologo(mensagem, conteudo, respostaPsicologo);
                        return;
                    }

                    if (!respostaLocalAtendimento.isEmpty()) {
                        tratarRespostaLocalAtendimento(mensagem, conteudo, respostaLocalAtendimento);
                        return;
                    }

                    String perfil = extrairValor(conteudo, "perfil");
                    String mensagemUsuario = extrairValor(conteudo, "mensagem");

                    if (requisicaoApi) {
                        String mensagemBase64 = extrairValor(
                                conteudo,
                                "mensagemBase64"
                        );

                        if (mensagemBase64 != null
                                && !mensagemBase64.trim().isEmpty()) {
                            mensagemUsuario = decodificarBase64(mensagemBase64);
                        }
                    }

                    System.out.println("\n[CONVERSACIONAL] Conteudo recebido:");
                    System.out.println(conteudo);
                    System.out.println("[CONVERSACIONAL] Perfil recebido: " + perfil);
                    System.out.println("[CONVERSACIONAL] Mensagem do usuario: " + mensagemUsuario);

                    salvarNaMemoria(usuarioId, "perfil", perfil);
                    salvarNaMemoria(usuarioId, "mensagem", mensagemUsuario);

                    String risco = consultarSeguranca(mensagemUsuario);
                    salvarNaMemoria(usuarioId, "risco", risco);

                    System.out.println("[CONVERSACIONAL] Nivel de risco: " + risco);

                    String decisaoIntervencao = consultarIntervencao(usuarioId, risco, perfil, mensagemUsuario);
                    String protocolo = extrairValor(decisaoIntervencao, "protocolo");
                    String mensagemIntervencao = extrairValor(decisaoIntervencao, "mensagem");

                    boolean permitirIA = extrairBoolean(decisaoIntervencao, "permitirIA", true);
                    boolean permitirConteudo = extrairBoolean(decisaoIntervencao, "permitirConteudo", true);
                    boolean permitirPsicologo = extrairBoolean(decisaoIntervencao, "permitirPsicologo", true);
                    boolean permitirLocalAtendimento = extrairBoolean(decisaoIntervencao, "permitirLocalAtendimento", true);
                    boolean permitirMonitoramento = extrairBoolean(decisaoIntervencao, "permitirMonitoramento", true);
                    boolean exibirBotaoCVV = extrairBoolean(decisaoIntervencao, "exibirBotaoCVV", false);
                    String telefoneCVV = extrairValor(decisaoIntervencao, "telefoneCVV");

                    salvarNaMemoria(usuarioId, "protocolo_intervencao", protocolo);

                    System.out.println("[CONVERSACIONAL] Protocolo de intervencao: " + protocolo);

                    String resposta;

                    if (!permitirIA || risco.equals("RISCO")) {
                        resposta = mensagemIntervencao;

                        if (resposta == null || resposta.trim().isEmpty()) {
                            resposta = respostaSeguranca();
                        }
                    } else {
                        String memoria = consultarMemoria(usuarioId);
                        String prompt = montarPrompt(perfil, mensagemUsuario, risco, memoria, protocolo);

                        System.out.println("[CONVERSACIONAL] Provedor de IA configurado: " + ClienteLLM.obterProvedor());
                        resposta = ClienteLLM.gerarResposta(prompt);
                        resposta = limparRespostaClinica(resposta);

                        if (resposta == null || resposta.trim().isEmpty() || resposta.contains("Erro")) {
                            resposta = gerarRespostaFallback(perfil, mensagemUsuario, risco);
                        }

                        if (mensagemIntervencao != null
                                && !mensagemIntervencao.trim().isEmpty()
                                && risco.equals("ATENCAO")) {
                            resposta += "\n\n" + mensagemIntervencao;
                        }
                    }

                    resposta += "\n\nImportante: esta plataforma realiza apenas apoio inicial e triagem emocional. Ela nao substitui acompanhamento psicologico profissional.";

                    resposta += montarBlocoApoioCVV(exibirBotaoCVV, telefoneCVV, risco, protocolo);

                    boolean acaoPsicologo =
                            permitirPsicologo
                            && deveSugerirPsicologo(perfil, risco);

                    /*
                     * No modo legado/simulacao, mantemos os convites em texto
                     * porque o AgentePaciente responde a essas perguntas.
                     *
                     * No modo API, os convites nao sao concatenados ao texto.
                     * A interface recebe as acoes como campos booleanos no JSON.
                     */
                    if (!requisicaoApi) {
                        if (permitirConteudo) {
                            resposta += "\n\nPosso sugerir conteudos educativos selecionados de uma base curada, com fontes institucionais e referencias documentadas, de acordo com o que voce relatou. Deseja receber sugestoes de conteudo? (sim/nao)";
                        }

                        if (acaoPsicologo) {
                            resposta += "\n\nTambem posso selecionar alguns psicologos cadastrados que tenham relacao com o perfil identificado na triagem. Deseja ver essas indicacoes? (sim/nao)";
                        }

                        if (permitirLocalAtendimento) {
                            resposta += "\n\nTambem posso mostrar locais demonstrativos de atendimento, como UBS, CAPS ou servicos publicos de saude mental. Deseja visualizar locais de atendimento proximos? (sim/nao)";
                        }

                        if (permitirMonitoramento) {
                            resposta += "\n\nPara fins academicos, tambem posso mostrar uma simulacao de acompanhamento semanal, demonstrando como um profissional poderia acompanhar indicadores emocionais com apoio do sistema. Deseja visualizar essa simulacao? (sim/nao)";
                        }
                    }

                    salvarNaMemoria(usuarioId, "resposta", resposta);

                    System.out.println("[CONVERSACIONAL] Resposta gerada:");
                    System.out.println(resposta);

                    ACLMessage reply = mensagem.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    if (requisicaoApi) {
                        String telefoneCvvResposta = telefoneCVV;

                        if (telefoneCvvResposta == null
                                || telefoneCvvResposta.trim().isEmpty()) {
                            telefoneCvvResposta = "188";
                        }

                        AcoesDisponiveis acoes = new AcoesDisponiveis(
                                permitirConteudo,
                                acaoPsicologo,
                                permitirLocalAtendimento,
                                permitirMonitoramento,
                                exibirBotaoCVV,
                                exibirBotaoCVV
                                        ? telefoneCvvResposta
                                        : null
                        );

                        ChatResponse respostaApi = ChatResponse.sucesso(
                                usuarioId,
                                perfil,
                                risco,
                                protocolo,
                                resposta,
                                acoes
                        );

                        reply.setContent(
                                new Gson().toJson(respostaApi)
                        );

                    } else {
                        reply.setContent(resposta);
                    }

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private void tratarRelatorioSimulado(String conteudo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String relatorio = extrairValor(conteudo, "relatorio");

        System.out.println("\n[CONVERSACIONAL] Relatorio simulado recebido do AgenteRelatorio:");
        System.out.println(relatorio);

        salvarNaMemoria(usuarioId, "relatorio_simulado", "gerado");

        ACLMessage msgPaciente = new ACLMessage(ACLMessage.INFORM);
        msgPaciente.addReceiver(new AID("agentePaciente", AID.ISLOCALNAME));
        msgPaciente.setContent(
                "Relatorio simulado gerado com sucesso.\n\n"
                + relatorio
        );

        send(msgPaciente);

        System.out.println("[CONVERSACIONAL] Relatorio simulado enviado ao AgentePaciente");
    }

    private void tratarRespostaConteudo(ACLMessage mensagemOriginal, String conteudo, String respostaConteudo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String risco = extrairValor(conteudo, "risco");
        String mensagemUsuario = extrairValor(conteudo, "mensagem");

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaConteudo.equalsIgnoreCase("sim")) {

            /*
             * Falha segura: o AgenteConversacional nao deve assumir BAIXO_RISCO
             * quando a informacao de risco nao estiver presente.
             *
             * O valor abaixo nao corresponde a nenhum risco permitido na base
             * curada, portanto o AgenteConteudo nao selecionara material.
             */
            if (risco == null || risco.trim().isEmpty()) {
                risco = "RISCO_NAO_INFORMADO";
                System.out.println("[CONVERSACIONAL] Risco ausente na solicitacao de conteudo. Recomendacao sera bloqueada por seguranca.");
            }

            String sugestao = consultarConteudo(usuarioId, perfil, risco, mensagemUsuario);
            salvarNaMemoria(usuarioId, "conteudo", "aceito");
            reply.setContent(sugestao);
        } else {
            salvarNaMemoria(usuarioId, "conteudo", "recusado");
            reply.setContent("Tudo bem. Os conteudos educativos podem ser consultados em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaPsicologo(ACLMessage mensagemOriginal, String conteudo, String respostaPsicologo) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");
        String risco = extrairValor(conteudo, "risco");

        if (risco == null || risco.isEmpty()) {
            risco = "RISCO_NAO_INFORMADO";
        }

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaPsicologo.equalsIgnoreCase("sim")) {
            String psicologos = consultarPsicologo(usuarioId, perfil, risco);
            salvarNaMemoria(usuarioId, "psicologo", "aceito");
            reply.setContent(psicologos);
        } else {
            salvarNaMemoria(usuarioId, "psicologo", "recusado");
            reply.setContent("Tudo bem. As indicacoes de profissionais podem ser consultadas em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaLocalAtendimento(ACLMessage mensagemOriginal, String conteudo, String respostaLocalAtendimento) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String cidade = extrairValor(conteudo, "cidade");
        String uf = extrairValor(conteudo, "uf");

        if (cidade == null || cidade.isEmpty()) {
            cidade = "Brasilia";
        }

        if (uf == null || uf.isEmpty()) {
            uf = "DF";
        }

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaLocalAtendimento.equalsIgnoreCase("sim")) {
            String locais = consultarLocalAtendimento(usuarioId, cidade, uf);
            salvarNaMemoria(usuarioId, "local_atendimento", "aceito");
            reply.setContent(locais);
        } else {
            salvarNaMemoria(usuarioId, "local_atendimento", "recusado");
            reply.setContent("Tudo bem. A busca por locais de atendimento pode ser feita em outro momento.");
        }

        send(reply);
    }

    private void tratarRespostaMonitoramento(ACLMessage mensagemOriginal, String conteudo, String respostaMonitoramento) {
        String usuarioId = extrairValor(conteudo, "usuarioId");
        String perfil = extrairValor(conteudo, "perfil");

        System.out.println("\n[CONVERSACIONAL] Resposta sobre simulacao de monitoramento recebida: " + respostaMonitoramento);

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaMonitoramento.equalsIgnoreCase("sim")) {
            ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.INFORM);
            msgMonitoramento.addReceiver(new AID("agenteMonitoramento", AID.ISLOCALNAME));
            msgMonitoramento.setContent(
                    "iniciar=true;"
                    + "usuarioId=" + usuarioId + ";"
                    + "perfil=" + perfil + ";"
                    + "modo=simulado"
            );
            send(msgMonitoramento);

            salvarNaMemoria(usuarioId, "monitoramento_simulado", "aceito");

            reply.setContent("Perfeito. Vou iniciar uma simulacao academica de acompanhamento semanal. Esses dados sao demonstrativos e nao representam avaliacao clinica.");
        } else {
            salvarNaMemoria(usuarioId, "monitoramento_simulado", "recusado");

            reply.setContent("Tudo bem. A simulacao de acompanhamento pode ser visualizada em outro momento.");
        }

        send(reply);
    }

    private boolean deveSugerirPsicologo(String perfil, String risco) {
        if (risco.equals("ATENCAO") || risco.equals("RISCO")) {
            return true;
        }

        return perfil.equals("ANSIEDADE")
                || perfil.equals("DEPRESSAO")
                || perfil.equals("MISTO");
    }

    private String montarBlocoApoioCVV(boolean exibirBotaoCVV, String telefoneCVV, String risco, String protocolo) {
        if (!exibirBotaoCVV) {
            return "";
        }

        if (telefoneCVV == null || telefoneCVV.trim().isEmpty()) {
            telefoneCVV = "188";
        }

        if (risco.equals("RISCO") || protocolo.equals("PROTOCOLO_PRESERVACAO_DA_VIDA")) {
            return "\n\n[ACAO_RECOMENDADA] Ligar para o CVV pelo telefone " + telefoneCVV + "."
                    + "\n[ACAO_RECOMENDADA] Procurar um servico de emergencia ou alguem de confianca que possa estar com voce agora.";
        }

        if (risco.equals("ATENCAO") || protocolo.equals("PROTOCOLO_ENCAMINHAMENTO_SEGURO")) {
            return "\n\n[RECURSO_DE_APOIO] O CVV pelo telefone " + telefoneCVV + " pode ser usado como canal de escuta e apoio emocional."
                    + "\n[RECURSO_DE_APOIO] Conversar com alguem de confianca ou buscar um profissional de saude mental tambem pode ajudar neste momento.";
        }

        return "";
    }

    private String montarPrompt(String perfil, String mensagemUsuario, String risco, String memoria, String protocolo) {
        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        if (protocolo == null || protocolo.isEmpty()) {
            protocolo = "PROTOCOLO_APOIO_INICIAL";
        }

        return "Voce faz parte de um prototipo academico de triagem emocional inicial.\n"
                + "A plataforma nao realiza diagnostico, nao indica tratamento e nao substitui acompanhamento psicologico profissional.\n"
                + "Ela apenas organiza informacoes iniciais, acolhe a mensagem do usuario e incentiva a busca por profissionais qualificados quando fizer sentido.\n\n"
                + "Informacoes do usuario:\n"
                + memoria + "\n\n"
                + "Perfil identificado pela triagem academica: " + perfil + "\n"
                + "Nivel de risco identificado pelo agente de seguranca: " + risco + "\n"
                + "Protocolo definido pelo agente de intervencao: " + protocolo + "\n"
                + "Mensagem atual do usuario: " + mensagemUsuario + "\n\n"
                + "Regras obrigatorias:\n"
                + "- Use o nome do usuario se estiver disponivel.\n"
                + "- Seja acolhedor, simples, breve e humano.\n"
                + "- Nao afirme diagnosticos.\n"
                + "- Nao diga que o usuario possui transtorno, doenca ou condicao clinica.\n"
                + "- Nao diga que esta tratando o usuario.\n"
                + "- Nao prometa melhora.\n"
                + "- Nao prometa acompanhamento clinico.\n"
                + "- Evite termos clinicos fortes como diagnostico, tratamento, transtorno, doenca ou sintomas.\n"
                + "- Prefira termos como sinais, momento de estresse, sobrecarga emocional, preocupacao, desconforto e apoio inicial.\n"
                + "- Reforce que a plataforma nao substitui acompanhamento psicologico profissional.\n"
                + "- Quando fizer sentido, incentive a busca por apoio profissional.\n"
                + "- Nao coloque a resposta inteira entre aspas.\n\n"
                + "Responda em portugues do Brasil com no maximo 150 palavras.";
    }

    private String respostaSeguranca() {
        return "Sinto muito que voce esteja passando por um momento tao dificil.\n"
                + "Esta plataforma nao substitui ajuda profissional. Procure imediatamente um profissional de saude, um servico de emergencia ou alguem de confianca que possa estar com voce agora.\n"
                + "Voce nao precisa lidar com isso sozinho(a).";
    }

    private String gerarRespostaFallback(String perfil, String mensagemUsuario, String risco) {
        if (risco.equals("ATENCAO")) {
            return "Percebo que voce esta passando por um momento delicado. Estou aqui para acolher sua mensagem, mas esta plataforma realiza apenas apoio inicial e nao substitui acompanhamento psicologico profissional.";
        }

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        switch (perfil) {
            case "ANSIEDADE":
                return "Entendo que voce esta se sentindo preocupado(a). Posso te ouvir com calma, mas lembro que esta plataforma atua apenas como apoio inicial.";

            case "DEPRESSAO":
                return "Sinto muito que voce esteja passando por esse momento. Voce nao precisa lidar com isso sozinho(a), e buscar apoio profissional pode ser importante.";

            case "MISTO":
                return "Percebo que seu formulario trouxe diferentes sinais de preocupacao e sobrecarga emocional. Podemos conversar com calma, mas uma avaliacao adequada deve ser feita por um profissional.";

            default:
                return "Estou aqui para te ouvir e oferecer apoio inicial, sem substituir acompanhamento profissional.";
        }
    }

    private String limparRespostaClinica(String resposta) {
        if (resposta == null || resposta.trim().isEmpty()) {
            return "Estou aqui para te ouvir e organizar melhor o que voce esta sentindo. Esta plataforma realiza apenas apoio inicial e triagem emocional, sem substituir acompanhamento profissional.";
        }

        String texto = resposta.trim();

        texto = removerAspasExternas(texto);

        texto = texto.replace(
                "podem ser sintomas de um desequilíbrio emocional",
                "podem estar relacionados a um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "podem ser sintomas de um desequilibrio emocional",
                "podem estar relacionados a um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de um desequilíbrio emocional",
                "sinais de um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de um desequilibrio emocional",
                "sinais de um momento de estresse ou sobrecarga emocional"
        );

        texto = texto.replace(
                "sintomas de ansiedade",
                "sinais de preocupacao ou sobrecarga"
        );

        texto = texto.replace(
                "sintomas de depressão",
                "sinais de tristeza ou desanimo"
        );

        texto = texto.replace(
                "sintomas de depressao",
                "sinais de tristeza ou desanimo"
        );

        texto = texto.replace(
                "transtorno de ansiedade",
                "momento de ansiedade ou preocupacao intensa"
        );

        texto = texto.replace(
                "transtorno depressivo",
                "momento de tristeza ou desanimo persistente"
        );

        texto = texto.replace(
                "doença mental",
                "questao de saude emocional"
        );

        texto = texto.replace(
                "doenca mental",
                "questao de saude emocional"
        );

        texto = texto.replace(
                "diagnosticar",
                "compreender melhor"
        );

        texto = texto.replace(
                "diagnóstico",
                "triagem inicial"
        );

        texto = texto.replace(
                "diagnostico",
                "triagem inicial"
        );

        texto = texto.replace(
                "tratamento",
                "apoio profissional"
        );

        texto = texto.replace(
                "tratar",
                "apoiar"
        );

        texto = texto.replace(
                "consulta de um psicólogo",
                "acompanhamento de um psicologo"
        );

        texto = texto.replace(
                "consulta de um psicologo",
                "acompanhamento de um psicologo"
        );

        texto = texto.replace(
                "consultoria de um profissional qualificado",
                "apoio de um profissional qualificado"
        );

        return texto;
    }

    private String removerAspasExternas(String texto) {
        if (texto == null) {
            return "";
        }

        String resultado = texto.trim();

        if (resultado.length() >= 2
                && resultado.startsWith("\"")
                && resultado.endsWith("\"")) {
            resultado = resultado.substring(1, resultado.length() - 1).trim();
        }

        return resultado;
    }

    private String consultarConteudo(String usuarioId, String perfil, String risco, String mensagemUsuario) {
        String conversationId = criarConversationId("CONTEUDO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteConteudo", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";perfil=" + perfil
                + ";risco=" + risco
                + ";mensagem=" + mensagemUsuario
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteConteudo",
                conversationId,
                TIMEOUT_CONTEUDO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteConteudo. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar os conteudos validados no momento.";
    }

    private String consultarPsicologo(String usuarioId, String perfil, String risco) {
        String conversationId = criarConversationId("PSICOLOGO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentePsicologo", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";perfil=" + perfil
                + ";risco=" + risco
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agentePsicologo",
                conversationId,
                TIMEOUT_PSICOLOGO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgentePsicologo. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar profissionais no momento.";
    }

    private String consultarLocalAtendimento(String usuarioId, String cidade, String uf) {
        String conversationId = criarConversationId("LOCAL");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteLocalAtendimento", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";cidade=" + cidade
                + ";uf=" + uf
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteLocalAtendimento",
                conversationId,
                TIMEOUT_LOCAL_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteLocalAtendimento. conversationId="
                + conversationId
        );

        return "Nao foi possivel consultar locais de atendimento no momento.";
    }

    private String consultarIntervencao(String usuarioId, String nivelRisco, String perfil, String mensagemUsuario) {
        String conversationId = criarConversationId("INTERVENCAO");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteIntervencao", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "usuarioId=" + valorSeguroAcl(usuarioId)
                + ";nivelRisco=" + nivelRisco
                + ";perfil=" + perfil
                + ";mensagem=" + mensagemUsuario
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteIntervencao",
                conversationId,
                TIMEOUT_INTERVENCAO_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteIntervencao. "
                + "Aplicando protocolo local de contingencia. conversationId="
                + conversationId
        );

        return gerarIntervencaoFallback(nivelRisco);
    }

    private void salvarNaMemoria(String usuarioId, String tipo, String valor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));

        msg.setContent(
                "tipo=" + tipo
                + ";usuarioId=" + valorSeguroAcl(usuarioId)
                + ";valor=" + valor
        );

        send(msg);
    }

    private String consultarSeguranca(String mensagemUsuario) {
        String conversationId = criarConversationId("SEGURANCA");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteSeguranca", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);
        msg.setContent(mensagemUsuario);

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteSeguranca",
                conversationId,
                TIMEOUT_SEGURANCA_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        /*
         * Falha segura:
         * se o agente de seguranca nao responder, nao assumimos BAIXO_RISCO.
         * O fluxo segue como ATENCAO para evitar liberar automaticamente
         * monitoramento ou minimizar uma situacao desconhecida.
         */
        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteSeguranca. "
                + "Usando ATENCAO como contingencia conservadora. conversationId="
                + conversationId
        );

        return "ATENCAO";
    }

    private String consultarMemoria(String usuarioId) {
        String conversationId = criarConversationId("MEMORIA");

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setConversationId(conversationId);
        msg.setReplyWith(conversationId);

        msg.setContent(
                "tipo=consulta;"
                + "usuarioId=" + valorSeguroAcl(usuarioId)
        );

        send(msg);

        ACLMessage resposta = aguardarResposta(
                "agenteMemoria",
                conversationId,
                TIMEOUT_MEMORIA_MS
        );

        if (resposta != null) {
            return resposta.getContent();
        }

        System.out.println(
                "[CONVERSACIONAL] Timeout ao consultar AgenteMemoria. "
                + "O fluxo continuara sem contexto historico. conversationId="
                + conversationId
        );

        return "";
    }

    private ACLMessage aguardarResposta(
            String agenteEsperado,
            String conversationId,
            long timeoutMs) {

        AID remetenteEsperado =
                new AID(agenteEsperado, AID.ISLOCALNAME);

        MessageTemplate templateConversation =
                MessageTemplate.MatchConversationId(conversationId);

        MessageTemplate templateRemetente =
                MessageTemplate.MatchSender(remetenteEsperado);

        MessageTemplate template =
                MessageTemplate.and(
                        templateConversation,
                        templateRemetente
                );

        return blockingReceive(template, timeoutMs);
    }

    private String criarConversationId(String etapa) {
        return "CONV_"
                + etapa
                + "_"
                + UUID.randomUUID().toString();
    }

    private String gerarIntervencaoFallback(String nivelRisco) {
        if ("RISCO".equalsIgnoreCase(nivelRisco)) {
            return "nivelRisco=RISCO;"
                    + "protocolo=PROTOCOLO_PRESERVACAO_DA_VIDA;"
                    + "permitirIA=false;"
                    + "permitirConteudo=false;"
                    + "permitirPsicologo=false;"
                    + "permitirLocalAtendimento=true;"
                    + "permitirMonitoramento=false;"
                    + "exibirBotaoCVV=true;"
                    + "telefoneCVV=188;"
                    + "mensagem=Busque apoio imediato e evite permanecer sozinho. "
                    + "Procure uma pessoa de confianca ou um servico de urgencia.";
        }

        if ("ATENCAO".equalsIgnoreCase(nivelRisco)) {
            return "nivelRisco=ATENCAO;"
                    + "protocolo=PROTOCOLO_ENCAMINHAMENTO_SEGURO;"
                    + "permitirIA=true;"
                    + "permitirConteudo=true;"
                    + "permitirPsicologo=true;"
                    + "permitirLocalAtendimento=true;"
                    + "permitirMonitoramento=false;"
                    + "exibirBotaoCVV=true;"
                    + "telefoneCVV=188;"
                    + "mensagem=Considere buscar apoio profissional ou conversar "
                    + "com uma pessoa de confianca.";
        }

        return "nivelRisco=BAIXO_RISCO;"
                + "protocolo=PROTOCOLO_APOIO_INICIAL;"
                + "permitirIA=true;"
                + "permitirConteudo=true;"
                + "permitirPsicologo=true;"
                + "permitirLocalAtendimento=true;"
                + "permitirMonitoramento=true;"
                + "exibirBotaoCVV=false;"
                + "telefoneCVV=188;"
                + "mensagem=Fluxo de apoio inicial autorizado.";
    }

    private String valorSeguroAcl(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String decodificarBase64(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "";
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(valor.trim());

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            System.out.println(
                    "[CONVERSACIONAL] Nao foi possivel decodificar a mensagem da API: "
                    + e.getMessage()
            );

            return "";
        }
    }

    private boolean extrairBoolean(String texto, String chave, boolean valorPadrao) {
        String valor = extrairValor(texto, chave);

        if (valor == null || valor.trim().isEmpty()) {
            return valorPadrao;
        }

        return valor.equalsIgnoreCase("true")
                || valor.equalsIgnoreCase("sim")
                || valor.equalsIgnoreCase("1");
    }

    private String extrairValor(String texto, String chave) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }

        String[] partes = texto.split(";");

        for (String parte : partes) {
            String[] kv = parte.split("=", 2);

            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(chave)) {
                return kv[1].trim();
            }
        }

        return "";
    }
}
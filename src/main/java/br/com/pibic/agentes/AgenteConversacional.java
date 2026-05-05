package br.com.pibic.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteConversacional extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Conversacional iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String respostaMonitoramento = extrairValor(conteudo, "respostaMonitoramento");

                    if (!respostaMonitoramento.isEmpty()) {
                        tratarRespostaMonitoramento(mensagem, conteudo, respostaMonitoramento);
                        return;
                    }

                    String perfil = extrairValor(conteudo, "perfil");
                    String mensagemUsuario = extrairValor(conteudo, "mensagem");

                    System.out.println("\n[CONVERSACIONAL] Conteudo recebido:");
                    System.out.println(conteudo);
                    System.out.println("[CONVERSACIONAL] Perfil recebido: " + perfil);
                    System.out.println("[CONVERSACIONAL] Mensagem do usuario: " + mensagemUsuario);

                    salvarNaMemoria("perfil", perfil);
                    salvarNaMemoria("mensagem", mensagemUsuario);

                    String risco = consultarSeguranca(mensagemUsuario);
                    salvarNaMemoria("risco", risco);

                    System.out.println("[CONVERSACIONAL] Nivel de risco: " + risco);

                    String resposta;

                    if (risco.equals("RISCO")) {
                        resposta = respostaSeguranca();
                    } else {
                        String memoria = consultarMemoria();
                        String prompt = montarPrompt(perfil, mensagemUsuario, risco, memoria);

                        resposta = ClienteNvidia.gerarResposta(prompt);

                        if (resposta == null || resposta.trim().isEmpty() || resposta.contains("Erro")) {
                            resposta = gerarRespostaFallback(perfil, mensagemUsuario, risco);
                        }

                        String sugestaoConteudo = consultarConteudo(perfil, mensagemUsuario);

                        if (sugestaoConteudo != null && !sugestaoConteudo.trim().isEmpty()) {
                            resposta += "\n\n" + sugestaoConteudo;
                        }

                        String psicologos = consultarPsicologo(perfil, risco);

                        if (psicologos != null && !psicologos.isEmpty()) {
                            resposta += "\n\n" + psicologos;
                        }

                        resposta += "\n\nVoce gostaria de receber um acompanhamento diario para ajudar a entender melhor como voce esta se sentindo ao longo da semana? (sim/nao)";
                    }

                    salvarNaMemoria("resposta", resposta);

                    System.out.println("[CONVERSACIONAL] Resposta gerada:");
                    System.out.println(resposta);

                    ACLMessage reply = mensagem.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(resposta);

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private void tratarRespostaMonitoramento(ACLMessage mensagemOriginal, String conteudo, String respostaMonitoramento) {
        String perfil = extrairValor(conteudo, "perfil");

        System.out.println("\n[CONVERSACIONAL] Resposta sobre monitoramento recebida: " + respostaMonitoramento);

        ACLMessage reply = mensagemOriginal.createReply();
        reply.setPerformative(ACLMessage.INFORM);

        if (respostaMonitoramento.equalsIgnoreCase("sim")) {
            ACLMessage msgMonitoramento = new ACLMessage(ACLMessage.INFORM);
            msgMonitoramento.addReceiver(new AID("agenteMonitoramento", AID.ISLOCALNAME));
            msgMonitoramento.setContent("iniciar=true;perfil=" + perfil);
            send(msgMonitoramento);

            salvarNaMemoria("monitoramento", "aceito");

            reply.setContent("Perfeito. Vamos iniciar seu acompanhamento diario. Voce recebera perguntas simples ao longo da semana para gerar seu relatorio semanal.");
        } else {
            salvarNaMemoria("monitoramento", "recusado");

            reply.setContent("Tudo bem. O acompanhamento pode ser ativado depois, quando voce quiser.");
        }

        send(reply);
    }

    private String consultarConteudo(String perfil, String mensagemUsuario) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteConteudo", AID.ISLOCALNAME));
        msg.setContent("perfil=" + perfil + ";mensagem=" + mensagemUsuario);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "";
    }

    private String montarPrompt(String perfil, String mensagemUsuario, String risco, String memoria) {
        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        return "Voce e um assistente empatico de apoio emocional em uma plataforma de saude mental.\n"
                + "A plataforma nao substitui psicologos, diagnosticos ou atendimento medico.\n\n"
                + "Informacoes do usuario:\n"
                + memoria + "\n\n"
                + "Mensagem atual do usuario: " + mensagemUsuario + "\n\n"
                + "Regras:\n"
                + "- Use o nome do usuario se estiver disponivel\n"
                + "- Seja acolhedor e humano\n"
                + "- Evite diagnosticos\n"
                + "- Incentive apoio profissional quando apropriado\n\n"
                + "Responda em portugues do Brasil de forma natural.";
    }

    private String respostaSeguranca() {
        return "Sinto muito que voce esteja passando por um momento tao dificil.\n"
                + "Neste caso, e importante procurar ajuda imediatamente com um profissional, alguem de confianca ou um servico de apoio emocional.\n"
                + "Voce nao precisa lidar com isso sozinho(a).";
    }

    private String gerarRespostaFallback(String perfil, String mensagemUsuario, String risco) {
        if (risco.equals("ATENCAO")) {
            return "Percebo que voce esta passando por um momento delicado.\n"
                    + "Estou aqui para te acolher, mas tambem e importante buscar apoio de um psicologo ou de alguem de confianca.";
        }

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        switch (perfil) {
            case "ANSIEDADE":
                return "Entendo que voce esta se sentindo preocupado(a). Vamos tentar desacelerar um pouco.";

            case "DEPRESSAO":
                return "Sinto muito que voce esteja passando por isso. Voce nao esta sozinho(a).";

            case "MISTO":
                return "Percebo sinais de ansiedade e tristeza. Vamos conversar com calma.";

            default:
                return "Estou aqui para te ouvir e te apoiar.";
        }
    }

    private void salvarNaMemoria(String tipo, String valor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setContent("tipo=" + tipo + ";valor=" + valor);
        send(msg);
    }

    private String consultarSeguranca(String mensagemUsuario) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteSeguranca", AID.ISLOCALNAME));
        msg.setContent(mensagemUsuario);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "BAIXO_RISCO";
    }

    private String consultarMemoria() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setContent("tipo=consulta");

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "";
    }

    private String extrairValor(String texto, String chave) {
        String[] partes = texto.split(";");

        for (String parte : partes) {
            String[] kv = parte.split("=", 2);

            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(chave)) {
                return kv[1].trim();
            }
        }

        return "";
    }

    private String consultarPsicologo(String perfil, String risco) {

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentePsicologo", AID.ISLOCALNAME));

        msg.setContent("perfil=" + perfil + ";risco=" + risco);

        send(msg);

        ACLMessage resposta = blockingReceive();

        if (resposta != null) {
            return resposta.getContent();
        }

        return "";
    }
}
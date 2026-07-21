package br.com.pibic.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteIntervencao extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Intervencao iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String nivelRisco = extrairValor(conteudo, "nivelRisco");
                    String perfil = extrairValor(conteudo, "perfil");

                    if (nivelRisco == null || nivelRisco.trim().isEmpty()) {
                        nivelRisco = "BAIXO_RISCO";
                    }

                    String decisao = decidirProtocolo(nivelRisco, perfil);

                    System.out.println("\n[INTERVENCAO] Requisicao recebida:");
                    System.out.println(conteudo);
                    System.out.println("[INTERVENCAO] Decisao:");
                    System.out.println(decisao);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);
                    resposta.setContent(decisao);

                    send(resposta);
                } else {
                    block();
                }
            }
        });
    }

    private String decidirProtocolo(String nivelRisco, String perfil) {
        if (nivelRisco.equalsIgnoreCase("RISCO")) {
            return protocoloPreservacaoDaVida();
        }

        if (nivelRisco.equalsIgnoreCase("ATENCAO")) {
            return protocoloEncaminhamentoSeguro();
        }

        return protocoloApoioInicial();
    }

    private String protocoloApoioInicial() {
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

    private String protocoloEncaminhamentoSeguro() {
        return "nivelRisco=ATENCAO;"
                + "protocolo=PROTOCOLO_ENCAMINHAMENTO_SEGURO;"
                + "permitirIA=true;"
                + "permitirConteudo=true;"
                + "permitirPsicologo=true;"
                + "permitirLocalAtendimento=true;"
                + "permitirMonitoramento=false;"
                + "exibirBotaoCVV=true;"
                + "telefoneCVV=188;"
                + "mensagem=Percebo que este momento pode exigir apoio adicional. Esta plataforma oferece apenas apoio inicial. Considere procurar um profissional de saude mental ou conversar com alguem de confianca. O CVV tambem pode ser acionado pelo telefone 188.";
    }

    private String protocoloPreservacaoDaVida() {
        return "nivelRisco=RISCO;"
                + "protocolo=PROTOCOLO_PRESERVACAO_DA_VIDA;"
                + "permitirIA=false;"
                + "permitirConteudo=false;"
                + "permitirPsicologo=false;"
                + "permitirLocalAtendimento=true;"
                + "permitirMonitoramento=false;"
                + "exibirBotaoCVV=true;"
                + "telefoneCVV=188;"
                + "mensagem=Sinto muito que voce esteja passando por um momento tao dificil. Agora, o mais importante e buscar apoio imediato. Nao fique sozinho ou sozinha. Ligue para o CVV pelo 188 ou procure um servico de emergencia. Tambem fale com alguem de confianca que possa estar com voce agora.";
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
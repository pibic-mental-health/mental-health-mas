package br.com.pibic.agentes;

import br.com.pibic.utils.JsonLoader;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgentePsicologo extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Psicologo iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String perfil = extrairValor(conteudo, "perfil");
                    String risco = extrairValor(conteudo, "risco");

                    System.out.println("\n[PSICOLOGO] Requisicao recebida:");
                    System.out.println(conteudo);

                    String recomendacao = recomendarPsicologo(perfil, risco);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);
                    resposta.setContent(recomendacao);

                    send(resposta);
                } else {
                    block();
                }
            }
        });
    }

    private String recomendarPsicologo(String perfil, String risco) {
        String json = JsonLoader.load("psicologos.json");

        if (json == null || json.isEmpty()) {
            return "";
        }

        if (risco != null && risco.equals("RISCO")) {
            return "Encaminhamento recomendado:\n"
                    + "- Procure apoio profissional imediatamente.\n"
                    + "- Se estiver em situação de emergência, busque um serviço de saúde da sua região.";
        }

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        switch (perfil) {
            case "ANSIEDADE":
                return "Psicologos indicados:\n"
                        + "- Dra. Ana Silva | Especialidade: Ansiedade | Modalidade: online | Contato: ana.silva@email.com";

            case "DEPRESSAO":
                return "Psicologos indicados:\n"
                        + "- Dr. Carlos Souza | Especialidade: Depressao | Modalidade: presencial e online | Contato: carlos.souza@email.com";

            case "MISTO":
                return "Psicologos indicados:\n"
                        + "- Dra. Juliana Lima | Especialidade: Ansiedade e Depressao | Modalidade: online | Contato: juliana.lima@email.com";

            default:
                return "Psicologos indicados:\n"
                        + "- Procure um psicologo cadastrado na plataforma conforme sua disponibilidade.";
        }
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
}
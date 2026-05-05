package br.com.pibic.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteSeguranca extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Seguranca iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage mensagem = receive();

                if (mensagem != null) {

                    String texto = mensagem.getContent().toLowerCase();

                    String risco = analisarRisco(texto);

                    System.out.println("\n[SEGURANCA] Texto analisado:");
                    System.out.println(texto);
                    System.out.println("[SEGURANCA] Nivel de risco: " + risco);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);
                    resposta.setContent(risco);

                    send(resposta);

                } else {
                    block();
                }
            }
        });
    }

    private String analisarRisco(String texto) {

        // RISCO ALTO
        if (texto.contains("quero morrer")
                || texto.contains("não quero viver")
                || texto.contains("acabar com tudo")
                || texto.contains("sumir de vez")) {
            return "RISCO";
        }

        // ATENÇÃO
        if (texto.contains("não aguento mais")
                || texto.contains("muito mal")
                || texto.contains("sem esperança")
                || texto.contains("cansado de tudo")) {
            return "ATENCAO";
        }

        return "BAIXO_RISCO";
    }
}
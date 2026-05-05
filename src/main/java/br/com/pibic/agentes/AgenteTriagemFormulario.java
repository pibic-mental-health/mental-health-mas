package br.com.pibic.agentes;

import java.util.HashMap;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteTriagemFormulario extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Triagem Formulario iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String formulario = mensagem.getContent();

                    System.out.println("\n[TRIAGEM] Formulario recebido:");
                    System.out.println(formulario);

                    String nome = extrairValor(formulario, "nome");

                    if (!nome.isEmpty()) {
                        salvarNaMemoria("nome", nome);
                        System.out.println("[TRIAGEM] Nome identificado: " + nome);
                    }

                    Map<String, Integer> dados = parseFormulario(formulario);

                    String perfil = analisar(dados);

                    salvarNaMemoria("perfil", perfil);

                    System.out.println("[TRIAGEM] Perfil identificado: " + perfil);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);
                    resposta.setContent(perfil);

                    send(resposta);
                } else {
                    block();
                }
            }
        });
    }

    private void salvarNaMemoria(String tipo, String valor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("agenteMemoria", AID.ISLOCALNAME));
        msg.setContent("tipo=" + tipo + ";valor=" + valor);
        send(msg);
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

    private Map<String, Integer> parseFormulario(String formulario) {
        Map<String, Integer> mapa = new HashMap<>();

        String[] partes = formulario.split(";");

        for (String parte : partes) {
            String[] kv = parte.split("=", 2);

            if (kv.length == 2) {
                try {
                    mapa.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                } catch (Exception e) {
                    // campos não numéricos, como nome, são ignorados na pontuação
                }
            }
        }

        return mapa;
    }

    private String analisar(Map<String, Integer> d) {

        int ansiedade =
                d.getOrDefault("preocupacao", 0) +
                d.getOrDefault("nervosismo", 0) +
                d.getOrDefault("relaxamento", 0) +
                d.getOrDefault("sono", 0);

        int depressao =
                d.getOrDefault("tristeza", 0) +
                (4 - d.getOrDefault("energia", 4)) +
                (4 - d.getOrDefault("interesse", 4)) +
                d.getOrDefault("isolamento", 0);

        System.out.println("[TRIAGEM] Score Ansiedade: " + ansiedade);
        System.out.println("[TRIAGEM] Score Depressao: " + depressao);

        if (ansiedade >= 8 && depressao >= 8) return "MISTO";
        if (ansiedade >= depressao && ansiedade >= 6) return "ANSIEDADE";
        if (depressao > ansiedade && depressao >= 6) return "DEPRESSAO";

        return "GERAL";
    }
}
package br.com.pibic.agentes;

import br.com.pibic.utils.JsonLoader;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteConteudo extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Conteudo iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage mensagem = receive();

                if (mensagem != null) {

                    String conteudo = mensagem.getContent();

                    String perfil = extrairValor(conteudo, "perfil");
                    String mensagemUsuario = extrairValor(conteudo, "mensagem");

                    System.out.println("\n[CONTEUDO] Requisicao recebida:");
                    System.out.println(conteudo);

                    String sugestao = gerarSugestao(perfil, mensagemUsuario);

                    ACLMessage resposta = mensagem.createReply();
                    resposta.setPerformative(ACLMessage.INFORM);
                    resposta.setContent(sugestao);

                    send(resposta);

                } else {
                    block();
                }
            }
        });
    }

    private String gerarSugestao(String perfil, String mensagemUsuario) {

        String json = JsonLoader.load("conteudos.json");

        if (json == null || json.isEmpty()) {
            System.out.println("[CONTEUDO] JSON nao encontrado ou vazio");
            return "";
        }

        StringBuilder resposta = new StringBuilder("Sugestoes para voce:\n");

        if (perfil == null || perfil.isEmpty()) {
            perfil = "GERAL";
        }

        // filtro simples (versão inicial)
        switch (perfil) {

            case "ANSIEDADE":
                resposta.append("- Respiracao guiada (5 minutos)\n");
                resposta.append("- Musica calma (lofi / instrumental)\n");
                resposta.append("- Evite excesso de informacao hoje\n");
                break;

            case "DEPRESSAO":
                resposta.append("- Ouvir uma musica que voce gosta\n");
                resposta.append("- Pequena caminhada ao ar livre\n");
                resposta.append("- Atividade leve que te de prazer\n");
                break;

            case "MISTO":
                resposta.append("- Meditacao curta (3 a 5 minutos)\n");
                resposta.append("- Respiracao guiada\n");
                resposta.append("- Evite sobrecarga hoje, va com calma\n");
                break;

            default:
                resposta.append("- Tire um momento para voce\n");
                resposta.append("- Respire fundo e desacelere\n");
                break;
        }

        return resposta.toString();
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
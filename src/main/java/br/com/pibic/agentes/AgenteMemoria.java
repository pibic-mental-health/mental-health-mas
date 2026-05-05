package br.com.pibic.agentes;

import java.util.ArrayList;
import java.util.List;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteMemoria extends Agent {

    private String nome = "";
    private String perfil = "";
    private String risco = "";

    private List<String> historico = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println("Agente Memoria iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    System.out.println("\n[MEMORIA] Conteudo recebido:");
                    System.out.println(conteudo);

                    String tipo = extrairValor(conteudo, "tipo");
                    String valor = extrairValor(conteudo, "valor");

                    switch (tipo) {
                        case "nome":
                            nome = valor;
                            System.out.println("[MEMORIA] Nome salvo: " + nome);
                            break;

                        case "perfil":
                            perfil = valor;
                            System.out.println("[MEMORIA] Perfil salvo: " + perfil);
                            break;

                        case "mensagem":
                            historico.add("Usuario: " + valor);
                            System.out.println("[MEMORIA] Mensagem armazenada");
                            break;

                        case "resposta":
                            historico.add("IA: " + valor);
                            System.out.println("[MEMORIA] Resposta armazenada");
                            break;

                        case "risco":
                            risco = valor;
                            System.out.println("[MEMORIA] Risco salvo: " + risco);
                            break;

                        case "monitoramento":
                            historico.add("Monitoramento: " + valor);
                            System.out.println("[MEMORIA] Monitoramento salvo: " + valor);
                            break;

                        case "consulta":
                            responderConsulta(mensagem);
                            break;

                        default:
                            System.out.println("[MEMORIA] Tipo nao reconhecido: " + tipo);
                            break;
                    }

                } else {
                    block();
                }
            }
        });
    }

    private void responderConsulta(ACLMessage mensagem) {
        String resumo = gerarResumo();

        System.out.println("[MEMORIA] Resumo enviado ao solicitante:");
        System.out.println(resumo);

        ACLMessage reply = mensagem.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(resumo);

        send(reply);
    }

    private String gerarResumo() {
        StringBuilder resumo = new StringBuilder();

        resumo.append("Nome: ").append(valorOuNaoInformado(nome)).append("\n");
        resumo.append("Perfil: ").append(valorOuNaoInformado(perfil)).append("\n");
        resumo.append("Risco: ").append(valorOuNaoInformado(risco)).append("\n");
        resumo.append("Historico:\n");

        if (historico.isEmpty()) {
            resumo.append("- Sem historico anterior.\n");
        } else {
            for (String linha : historico) {
                resumo.append("- ").append(linha).append("\n");
            }
        }

        return resumo.toString();
    }

    private String valorOuNaoInformado(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
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
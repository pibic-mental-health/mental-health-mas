package br.com.pibic.agentes;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
                    String risco = extrairValor(conteudo, "risco");

                    if (risco == null || risco.trim().isEmpty()) {
                        risco = "BAIXO_RISCO";
                    }

                    System.out.println("\n[CONTEUDO] Requisicao recebida:");
                    System.out.println(conteudo);

                    String sugestao = recomendarConteudos(perfil, mensagemUsuario, risco);

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

    private String recomendarConteudos(String perfil, String mensagemUsuario, String risco) {
        if (perfil == null || perfil.trim().isEmpty()) {
            perfil = "GERAL";
        }

        List<ConteudoEvidencia> conteudos = carregarConteudos();

        if (conteudos.isEmpty()) {
            return "Nao foi possivel carregar sugestoes de conteudo no momento.";
        }

        List<ConteudoPontuado> pontuados = new ArrayList<ConteudoPontuado>();

        for (ConteudoEvidencia item : conteudos) {
            if (item == null) {
                continue;
            }

            int pontuacao = calcularPontuacao(item, perfil, mensagemUsuario, risco);

            if (pontuacao > 0) {
                pontuados.add(new ConteudoPontuado(item, pontuacao));
            }
        }

        Collections.sort(pontuados, new Comparator<ConteudoPontuado>() {
            @Override
            public int compare(ConteudoPontuado a, ConteudoPontuado b) {
                return b.pontuacao - a.pontuacao;
            }
        });

        if (pontuados.isEmpty()) {
            return gerarRespostaSemRecomendacao(perfil);
        }

        StringBuilder resposta = new StringBuilder();

        resposta.append("Sugestoes educativas recomendadas pelo AgenteConteudo:\n\n");
        resposta.append("Criterios usados na recomendacao:\n");
        resposta.append("- perfil identificado na triagem\n");
        resposta.append("- palavras presentes na mensagem do usuario\n");
        resposta.append("- nivel de evidencia cadastrado\n");
        resposta.append("- pontuacao base do conteudo\n\n");

        resposta.append("Observacao: estes conteudos sao apenas apoio inicial e nao substituem avaliacao, diagnostico ou acompanhamento profissional.\n\n");

        int limite = Math.min(3, pontuados.size());

        for (int i = 0; i < limite; i++) {
            ConteudoPontuado pontuado = pontuados.get(i);
            ConteudoEvidencia item = pontuado.conteudo;

            resposta.append(i + 1).append(". ").append(valorSeguro(item.titulo)).append("\n");
            resposta.append("   Tipo: ").append(valorSeguro(item.tipo)).append("\n");
            resposta.append("   Descricao: ").append(valorSeguro(item.descricao)).append("\n");
            resposta.append("   Perfil associado: ").append(valorSeguro(item.perfil)).append("\n");
            resposta.append("   Evidencia: ").append(valorSeguro(item.nivelEvidencia)).append("\n");
            resposta.append("   Fonte: ").append(valorSeguro(item.fonte)).append(" - ").append(valorSeguro(item.referencia)).append("\n");
            resposta.append("   Pontuacao calculada: ").append(pontuado.pontuacao).append("\n");
            resposta.append("   Observacao: ").append(valorSeguro(item.observacao)).append("\n\n");
        }

        return resposta.toString();
    }

    private int calcularPontuacao(ConteudoEvidencia item, String perfil, String mensagemUsuario, String risco) {
        int pontos = 0;

        pontos += item.pontuacaoBase;

        String perfilItem = normalizar(item.perfil);
        String perfilUsuario = normalizar(perfil);

        if (perfilItem.equals(perfilUsuario)) {
            pontos += 10;
        } else if (perfilUsuario.equals("misto")
                && (perfilItem.equals("ansiedade") || perfilItem.equals("depressao") || perfilItem.equals("misto"))) {
            pontos += 7;
        } else if (perfilItem.equals("geral")) {
            pontos += 3;
        } else {
            pontos -= 4;
        }

        pontos += calcularPontuacaoPorPalavrasChave(item, mensagemUsuario);

        pontos += calcularPontuacaoPorEvidencia(item.nivelEvidencia);

        String riscoNormalizado = normalizar(risco);

        if (riscoNormalizado.equals("atencao")) {
            if (perfilItem.equals("geral")) {
                pontos += 2;
            }
        }

        if (riscoNormalizado.equals("risco")) {
            pontos -= 20;
        }

        return pontos;
    }

    private int calcularPontuacaoPorPalavrasChave(ConteudoEvidencia item, String mensagemUsuario) {
        if (item.palavrasChave == null || item.palavrasChave.isEmpty()) {
            return 0;
        }

        String mensagemNormalizada = normalizar(mensagemUsuario);
        int pontos = 0;

        for (String palavra : item.palavrasChave) {
            String palavraNormalizada = normalizar(palavra);

            if (!palavraNormalizada.isEmpty() && mensagemNormalizada.contains(palavraNormalizada)) {
                pontos += 4;
            }
        }

        return pontos;
    }

    private int calcularPontuacaoPorEvidencia(String nivelEvidencia) {
        String evidencia = normalizar(nivelEvidencia);

        if (evidencia.equals("umbrella_review")) {
            return 5;
        }

        if (evidencia.equals("revisao_sistematica")) {
            return 5;
        }

        if (evidencia.equals("revisao_cochrane")) {
            return 5;
        }

        if (evidencia.equals("baseado_em_mindfulness")) {
            return 3;
        }

        if (evidencia.equals("educativo")) {
            return 1;
        }

        return 0;
    }

    private String gerarRespostaSemRecomendacao(String perfil) {
        return "Nao encontrei conteudos especificos para o perfil " + perfil + " neste momento.\n\n"
                + "Sugestao geral: realizar uma pausa breve, respirar com calma e procurar apoio profissional quando necessario.\n\n"
                + "Observacao: esta sugestao e apenas educativa e nao substitui avaliacao profissional.";
    }

    private List<ConteudoEvidencia> carregarConteudos() {
        List<ConteudoEvidencia> listaVazia = new ArrayList<ConteudoEvidencia>();

        String json = JsonLoader.load("conteudos_evidencias.json");

        if (json == null || json.trim().isEmpty()) {
            System.out.println("[CONTEUDO] Arquivo conteudos_evidencias.json nao encontrado ou vazio");
            return listaVazia;
        }

        try {
            Gson gson = new Gson();
            Type tipoLista = new TypeToken<List<ConteudoEvidencia>>() {}.getType();

            List<ConteudoEvidencia> conteudos = gson.fromJson(json, tipoLista);

            if (conteudos == null) {
                return listaVazia;
            }

            return conteudos;

        } catch (Exception e) {
            System.out.println("[CONTEUDO] Erro ao ler conteudos_evidencias.json: " + e.getMessage());
            return listaVazia;
        }
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

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        return textoNormalizado.toLowerCase().trim();
    }

    private String valorSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
    }

    private static class ConteudoEvidencia {
        String perfil;
        String tipo;
        String titulo;
        String descricao;
        List<String> palavrasChave;
        String nivelEvidencia;
        String fonte;
        String referencia;
        int pontuacaoBase;
        String observacao;
    }

    private static class ConteudoPontuado {
        ConteudoEvidencia conteudo;
        int pontuacao;

        ConteudoPontuado(ConteudoEvidencia conteudo, int pontuacao) {
            this.conteudo = conteudo;
            this.pontuacao = pontuacao;
        }
    }
}
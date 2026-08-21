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

    private static final String BAIXO_RISCO = "BAIXO_RISCO";
    private static final String ATENCAO = "ATENCAO";
    private static final String RISCO = "RISCO";

    @Override
    protected void setup() {
        System.out.println("Agente Conteudo iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem == null) {
                    block();
                    return;
                }

                String conteudo = mensagem.getContent();

                String perfil = extrairValor(conteudo, "perfil");
                String mensagemUsuario = extrairValor(conteudo, "mensagem");
                String risco = extrairValor(conteudo, "risco");

                if (perfil == null || perfil.trim().isEmpty()) {
                    perfil = "GERAL";
                }

                if (risco == null || risco.trim().isEmpty()) {
                    risco = BAIXO_RISCO;
                }

                System.out.println("\n[CONTEUDO] Requisicao recebida:");
                System.out.println(conteudo);

                String sugestao = recomendarConteudos(perfil, mensagemUsuario, risco);

                ACLMessage resposta = mensagem.createReply();
                resposta.setPerformative(ACLMessage.INFORM);
                resposta.setContent(sugestao);
                send(resposta);
            }
        });
    }

    private String recomendarConteudos(String perfil, String mensagemUsuario, String risco) {

        String riscoNormalizado = normalizar(risco);

        // Defesa em profundidade:
        // mesmo que o AgenteConversacional ja bloqueie conteudo em RISCO,
        // o proprio AgenteConteudo nao recomenda material educativo nesse estado.
        if (normalizar(RISCO).equals(riscoNormalizado)) {
            return gerarRespostaBloqueioRisco();
        }

        List<ConteudoValidado> conteudos = carregarConteudos();

        if (conteudos.isEmpty()) {
            return "Nao foi possivel carregar a base de conteudos validados no momento.";
        }

        List<String> intencoes = identificarIntencoes(mensagemUsuario);

        System.out.println("[CONTEUDO] Perfil: " + perfil);
        System.out.println("[CONTEUDO] Risco: " + risco);
        System.out.println("[CONTEUDO] Intencoes identificadas: " + formatarLista(intencoes));

        List<ConteudoPontuado> pontuados = new ArrayList<ConteudoPontuado>();

        for (ConteudoValidado item : conteudos) {
            if (item == null || !item.ativo) {
                continue;
            }

            if (!riscoPermitido(item, risco)) {
                continue;
            }

            if (!perfilPermitido(item, perfil)) {
                continue;
            }

            int pontuacao = calcularPontuacao(
                    item,
                    perfil,
                    mensagemUsuario,
                    risco,
                    intencoes
            );

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
            return gerarRespostaSemRecomendacao(perfil, risco);
        }

        List<ConteudoPontuado> selecionados = selecionarComDiversidade(pontuados, 3);

        StringBuilder resposta = new StringBuilder();

        resposta.append("[CONTEUDOS_VALIDADOS]\n");
        resposta.append("Conteudos educativos selecionados a partir da base curada do projeto.\n\n");

        if (!intencoes.isEmpty()) {
            resposta.append("Necessidades identificadas na mensagem: ")
                    .append(formatarLista(intencoes))
                    .append("\n\n");
        }

        resposta.append("Observacao geral: os materiais abaixo sao educativos e nao substituem avaliacao, diagnostico ou acompanhamento profissional.\n\n");

        for (int i = 0; i < selecionados.size(); i++) {
            ConteudoPontuado pontuado = selecionados.get(i);
            ConteudoValidado item = pontuado.conteudo;

            resposta.append("===== CONTEUDO ").append(i + 1).append(" =====\n");
            resposta.append("ID: ").append(valorSeguro(item.id)).append("\n");
            resposta.append("Titulo: ").append(valorSeguro(item.titulo)).append("\n");
            resposta.append("Objetivo: ").append(valorSeguro(item.objetivo)).append("\n");

            if (item.quandoUsar != null && !item.quandoUsar.isEmpty()) {
                resposta.append("Quando pode ser util:\n");
                for (String quando : item.quandoUsar) {
                    resposta.append("- ").append(quando).append("\n");
                }
            }

            if (item.passos != null && !item.passos.isEmpty()) {
                resposta.append("Passos:\n");
                for (int p = 0; p < item.passos.size(); p++) {
                    resposta.append(p + 1)
                            .append(". ")
                            .append(item.passos.get(p))
                            .append("\n");
                }
            }

            resposta.append("Tempo estimado: ").append(valorSeguro(item.tempoEstimado)).append("\n");
            resposta.append("Fonte institucional: ").append(valorSeguro(item.fonteInstitucional)).append("\n");
            resposta.append("Material base: ").append(valorSeguro(item.materialBase)).append("\n");
            resposta.append("Referencia: ").append(valorSeguro(item.referencia)).append("\n");
            resposta.append("Validacao: ").append(valorSeguro(item.nivelValidacao)).append("\n");

            if (item.urlFonte != null && !item.urlFonte.trim().isEmpty()) {
                resposta.append("Fonte oficial: ").append(item.urlFonte).append("\n");
            }

            resposta.append("Observacao etica: ").append(valorSeguro(item.observacaoEtica)).append("\n");
            resposta.append("Pontuacao interna: ").append(pontuado.pontuacao).append("\n\n");
        }

        return resposta.toString();
    }

    private int calcularPontuacao(
            ConteudoValidado item,
            String perfil,
            String mensagemUsuario,
            String risco,
            List<String> intencoesIdentificadas) {

        int pontos = 0;

        // A prioridade e curatorial. Ela nao representa eficacia clinica.
        pontos += Math.max(0, item.prioridade) / 5;

        if (listaContemNormalizado(item.perfisPrioritarios, perfil)) {
            pontos += 12;
        }

        if (item.intencoes != null && intencoesIdentificadas != null) {
            for (String intencao : intencoesIdentificadas) {
                if (listaContemNormalizado(item.intencoes, intencao)) {
                    pontos += 8;
                }
            }
        }

        pontos += calcularPontuacaoPorPalavrasChave(item, mensagemUsuario);
        pontos += calcularPontuacaoPorValidacao(item.nivelValidacao);

        String riscoNormalizado = normalizar(risco);

        // Em ATENCAO, orientacoes para busca de apoio profissional ganham prioridade.
        if (normalizar(ATENCAO).equals(riscoNormalizado)
                && normalizar("apoio_profissional").equals(normalizar(item.tipo))) {
            pontos += 15;
        }

        // Em baixo risco, materiais praticos de autocuidado/gerenciamento de estresse
        // tendem a ser priorizados em relacao a encaminhamento.
        if (normalizar(BAIXO_RISCO).equals(riscoNormalizado)
                && normalizar("apoio_profissional").equals(normalizar(item.tipo))) {
            pontos += 1;
        }

        return pontos;
    }

    private int calcularPontuacaoPorPalavrasChave(
            ConteudoValidado item,
            String mensagemUsuario) {

        if (item.palavrasChave == null || item.palavrasChave.isEmpty()) {
            return 0;
        }

        String mensagemNormalizada = normalizar(mensagemUsuario);
        int pontos = 0;
        int correspondencias = 0;

        for (String palavra : item.palavrasChave) {
            String palavraNormalizada = normalizar(palavra);

            if (!palavraNormalizada.isEmpty()
                    && mensagemNormalizada.contains(palavraNormalizada)) {
                pontos += 3;
                correspondencias++;

                // Evita que listas grandes de palavras-chave dominem a pontuacao.
                if (correspondencias >= 4) {
                    break;
                }
            }
        }

        return pontos;
    }

    private int calcularPontuacaoPorValidacao(String nivelValidacao) {
        String nivel = normalizar(nivelValidacao);

        if (nivel.contains("oms")
                && (nivel.contains("evidencia") || nivel.contains("testes_de_campo"))) {
            return 8;
        }

        if (nivel.contains("fonte_institucional_oficial")) {
            return 7;
        }

        if (nivel.contains("diretriz_institucional")) {
            return 6;
        }

        return 0;
    }

    private List<String> identificarIntencoes(String mensagemUsuario) {
        List<String> intencoes = new ArrayList<String>();
        String texto = normalizar(mensagemUsuario);

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "preocupacao",
                new String[] {
                        "preocup", "ansios", "medo", "receio",
                        "futuro", "prazo", "prova", "apresentacao"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "sobrecarga",
                new String[] {
                        "sobrecarreg", "muita coisa", "pressao",
                        "estresse", "nao dou conta", "cansad"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "dificuldade_relaxar",
                new String[] {
                        "nervos", "tens", "acelerad",
                        "agitado", "relaxar", "inquiet"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "pensamentos_dificeis",
                new String[] {
                        "pensamento", "mente", "nao paro de pensar",
                        "pensando demais", "rumin", "ideia ruim"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "emocao_dificil",
                new String[] {
                        "triste", "angusti", "emocao",
                        "raiva", "frustr", "chatead", "medo"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "autocritica",
                new String[] {
                        "culpa", "fracass", "me cobro",
                        "cobranca", "sou ruim", "nao sou bom"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "falta_foco",
                new String[] {
                        "foco", "concentr", "distraid",
                        "nao consigo comecar", "por onde comecar"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "necessidade_apoio",
                new String[] {
                        "preciso conversar", "queria conversar",
                        "preciso de apoio", "preciso de ajuda",
                        "sozinh", "ninguem me ouve", "profissional"
                }
        );

        adicionarIntencaoSeContem(
                intencoes,
                texto,
                "acao_com_significado",
                new String[] {
                        "nao sei o que fazer", "decisao",
                        "importante para mim", "quero fazer",
                        "preciso organizar", "rotina"
                }
        );

        return intencoes;
    }

    private void adicionarIntencaoSeContem(
            List<String> intencoes,
            String textoNormalizado,
            String intencao,
            String[] termos) {

        for (String termo : termos) {
            if (textoNormalizado.contains(normalizar(termo))) {
                if (!listaContemNormalizado(intencoes, intencao)) {
                    intencoes.add(intencao);
                }
                return;
            }
        }
    }

    private boolean riscoPermitido(ConteudoValidado item, String risco) {
        return listaContemNormalizado(item.riscosPermitidos, risco);
    }

    private boolean perfilPermitido(ConteudoValidado item, String perfil) {
        if (item.perfisPermitidos == null || item.perfisPermitidos.isEmpty()) {
            return true;
        }

        if (listaContemNormalizado(item.perfisPermitidos, "TODOS")) {
            return true;
        }

        return listaContemNormalizado(item.perfisPermitidos, perfil);
    }

    private boolean listaContemNormalizado(List<String> lista, String valor) {
        if (lista == null || valor == null) {
            return false;
        }

        String procurado = normalizar(valor);

        for (String item : lista) {
            if (normalizar(item).equals(procurado)) {
                return true;
            }
        }

        return false;
    }

    private List<ConteudoPontuado> selecionarComDiversidade(
            List<ConteudoPontuado> pontuados,
            int limite) {

        List<ConteudoPontuado> selecionados = new ArrayList<ConteudoPontuado>();
        List<String> tiposUsados = new ArrayList<String>();

        for (ConteudoPontuado item : pontuados) {
            if (selecionados.size() >= limite) {
                break;
            }

            String tipo = normalizar(item.conteudo.tipo);

            if (!tiposUsados.contains(tipo)) {
                selecionados.add(item);
                tiposUsados.add(tipo);
            }
        }

        // Se nao houver diversidade suficiente, completa com os proximos itens.
        if (selecionados.size() < limite) {
            for (ConteudoPontuado item : pontuados) {
                if (selecionados.size() >= limite) {
                    break;
                }

                if (!selecionados.contains(item)) {
                    selecionados.add(item);
                }
            }
        }

        return selecionados;
    }

    private String gerarRespostaBloqueioRisco() {
        return "[CONTEUDO_BLOQUEADO]\n"
                + "Conteudos educativos nao serao recomendados enquanto o protocolo de preservacao da vida estiver ativo.\n"
                + "O AgenteConteudo devolve o controle ao fluxo de seguranca e intervencao.";
    }

    private String gerarRespostaSemRecomendacao(String perfil, String risco) {
        return "[SEM_CONTEUDO_VALIDADO]\n"
                + "Nao encontrei, na base curada, um conteudo validado compativel com o perfil "
                + perfil
                + " e o nivel de risco "
                + risco
                + ".\n"
                + "Nenhuma recomendacao alternativa foi inventada pelo agente.";
    }

    private List<ConteudoValidado> carregarConteudos() {
        List<ConteudoValidado> listaVazia = new ArrayList<ConteudoValidado>();

        String json = JsonLoader.load("conteudos_evidencias.json");

        if (json == null || json.trim().isEmpty()) {
            System.out.println(
                    "[CONTEUDO] Arquivo conteudos_evidencias.json nao encontrado ou vazio"
            );
            return listaVazia;
        }

        try {
            Gson gson = new Gson();
            Type tipoLista = new TypeToken<List<ConteudoValidado>>() {}.getType();

            List<ConteudoValidado> conteudos = gson.fromJson(json, tipoLista);

            if (conteudos == null) {
                return listaVazia;
            }

            return conteudos;

        } catch (Exception e) {
            System.out.println(
                    "[CONTEUDO] Erro ao ler conteudos_evidencias.json: "
                            + e.getMessage()
            );
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

        String textoNormalizado = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        textoNormalizado = textoNormalizado.replaceAll(
                "[\\p{InCombiningDiacriticalMarks}]",
                ""
        );

        return textoNormalizado.toLowerCase().trim();
    }

    private String valorSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
    }

    private String formatarLista(List<String> itens) {
        if (itens == null || itens.isEmpty()) {
            return "nenhuma intencao especifica identificada";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(itens.get(i));
        }

        return sb.toString();
    }

    private static class ConteudoValidado {
        boolean ativo;
        String id;
        String tipo;
        String titulo;
        String objetivo;
        List<String> quandoUsar;
        List<String> passos;
        String tempoEstimado;

        List<String> perfisPermitidos;
        List<String> perfisPrioritarios;
        List<String> riscosPermitidos;

        List<String> intencoes;
        List<String> palavrasChave;

        String fonteInstitucional;
        String materialBase;
        String referencia;
        String urlFonte;
        String nivelValidacao;

        int prioridade;
        String observacaoEtica;
    }

    private static class ConteudoPontuado {
        ConteudoValidado conteudo;
        int pontuacao;

        ConteudoPontuado(ConteudoValidado conteudo, int pontuacao) {
            this.conteudo = conteudo;
            this.pontuacao = pontuacao;
        }
    }
}
package br.com.pibic.agentes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.pibic.utils.JsonLoader;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteLocalAtendimento extends Agent {

    private static final int LIMITE_EXIBICAO = 10;

    @Override
    protected void setup() {
        System.out.println("Agente Local Atendimento iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage mensagem = receive();

                if (mensagem != null) {
                    String conteudo = mensagem.getContent();

                    String cidade = extrairValor(conteudo, "cidade");
                    String uf = extrairValor(conteudo, "uf");

                    System.out.println("\n[LOCAL_ATENDIMENTO] Requisicao recebida:");
                    System.out.println(conteudo);

                    String resposta = buscarLocais(cidade, uf);

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

    private String buscarLocais(String cidade, String uf) {
        if (cidade == null || cidade.trim().isEmpty()) {
            cidade = "Brasilia";
        }

        if (uf == null || uf.trim().isEmpty()) {
            uf = "DF";
        }

        List<LocalAtendimentoResultado> todos = new ArrayList<LocalAtendimentoResultado>();

        adicionarResultados(todos, ClienteGooglePlacesApi.buscarLocais(cidade, uf), "Google Places");
        adicionarResultados(todos, ClienteOverpassApi.buscarLocais(cidade, uf), "OpenStreetMap / Overpass");
        adicionarResultados(todos, ClienteCnesApi.buscarLocais(cidade, uf), "CNES / Dados Abertos SUS");

        List<LocalAtendimentoResultado> consolidados =
                ClassificadorLocalAtendimento.deduplicarOrdenarLimitar(todos, LIMITE_EXIBICAO);

        if (!consolidados.isEmpty()) {
            return montarRespostaHibrida(cidade, uf, consolidados);
        }

        System.out.println("[LOCAL_ATENDIMENTO] Nenhuma fonte externa retornou dados relevantes. Usando fallback local JSON.");

        return buscarLocaisFallbackJson(cidade, uf);
    }

    private void adicionarResultados(
            List<LocalAtendimentoResultado> destino,
            List<LocalAtendimentoResultado> origem,
            String nomeFonte
    ) {
        if (origem == null || origem.isEmpty()) {
            System.out.println("[LOCAL_ATENDIMENTO] Fonte sem resultados relevantes: " + nomeFonte);
            return;
        }

        System.out.println("[LOCAL_ATENDIMENTO] Fonte com resultados: " + nomeFonte + " -> " + origem.size());

        for (LocalAtendimentoResultado local : origem) {
            destino.add(local);
        }
    }

    private String montarRespostaHibrida(
            String cidade,
            String uf,
            List<LocalAtendimentoResultado> locais
    ) {
        StringBuilder resposta = new StringBuilder();

        resposta.append("Locais de atendimento encontrados por estrategia hibrida:\n\n");
        resposta.append("Cidade/UF considerada: ").append(cidade).append("/").append(uf).append("\n\n");

        resposta.append("Fontes consultadas:\n");
        resposta.append("- Google Places API, se houver chave configurada\n");
        resposta.append("- OpenStreetMap / Overpass API\n");
        resposta.append("- CNES / Dados Abertos SUS\n\n");

        resposta.append("Criterio de priorizacao:\n");
        resposta.append("- CAPS e atencao psicossocial aparecem primeiro.\n");
        resposta.append("- Depois aparecem psicologia, psicoterapia, psiquiatria e saude mental.\n");
        resposta.append("- UBS e Centros de Saude aparecem como porta de entrada da rede publica.\n\n");

        resposta.append("Observacao: os dados devem ser validados nos canais oficiais antes de qualquer uso real.\n");
        resposta.append("Esta plataforma possui finalidade academica e demonstrativa, sem substituir atendimento profissional.\n\n");

        int limite = Math.min(LIMITE_EXIBICAO, locais.size());

        for (int i = 0; i < limite; i++) {
            LocalAtendimentoResultado local = locais.get(i);

            resposta.append(i + 1).append(". ").append(valorSeguro(local.nome)).append("\n");
            resposta.append("   Categoria: ").append(valorSeguro(local.categoria)).append("\n");
            resposta.append("   Tipo: ").append(ClassificadorLocalAtendimento.obterTipoParaExibicao(local)).append("\n");
            resposta.append("   Cidade/UF: ").append(valorSeguro(local.cidade)).append("/").append(valorSeguro(local.uf)).append("\n");
            resposta.append("   Endereco: ").append(valorSeguro(local.endereco)).append("\n");
            resposta.append("   Telefone: ").append(valorSeguro(local.telefone)).append("\n");

            if (local.codigoCnes != null && !local.codigoCnes.trim().isEmpty()) {
                resposta.append("   Codigo CNES: ").append(local.codigoCnes).append("\n");
            }

            if (local.latitude != 0.0 || local.longitude != 0.0) {
                resposta.append("   Coordenadas: ")
                        .append(local.latitude)
                        .append(", ")
                        .append(local.longitude)
                        .append("\n");
            }

            if (local.link != null && !local.link.trim().isEmpty()) {
                resposta.append("   Link: ").append(local.link).append("\n");
            }

            resposta.append("   Prioridade da recomendacao: ").append(local.prioridade).append("\n");
            resposta.append("   Fonte: ").append(valorSeguro(local.fonte)).append("\n");
            resposta.append("   Observacao: ").append(valorSeguro(local.observacao)).append("\n\n");
        }

        return resposta.toString();
    }

    private String buscarLocaisFallbackJson(String cidade, String uf) {
        List<LocalAtendimentoFallback> locais = carregarLocaisFallback();

        if (locais.isEmpty()) {
            return "Nao foi possivel carregar locais de atendimento no momento.";
        }

        List<LocalAtendimentoResultado> resultados = new ArrayList<LocalAtendimentoResultado>();

        for (LocalAtendimentoFallback local : locais) {
            if (local == null) {
                continue;
            }

            boolean mesmaCidade = local.cidade != null && local.cidade.equalsIgnoreCase(cidade);
            boolean mesmoUf = local.uf != null && local.uf.equalsIgnoreCase(uf);

            if (mesmaCidade && mesmoUf) {
                LocalAtendimentoResultado resultado = converterFallback(local);

                boolean relevante = ClassificadorLocalAtendimento.classificar(resultado);

                if (relevante) {
                    resultados.add(resultado);
                }
            }
        }

        List<LocalAtendimentoResultado> consolidados =
                ClassificadorLocalAtendimento.deduplicarOrdenarLimitar(resultados, LIMITE_EXIBICAO);

        if (consolidados.isEmpty()) {
            return "Nenhum local encontrado para " + cidade + "/" + uf + ".\n\n"
                    + "Observacao: em uma versao real, esta busca pode consultar Google Places, OpenStreetMap e CNES.";
        }

        return montarRespostaHibrida(cidade, uf, consolidados);
    }

    private LocalAtendimentoResultado converterFallback(LocalAtendimentoFallback local) {
        LocalAtendimentoResultado resultado = new LocalAtendimentoResultado();

        resultado.nome = local.nome;
        resultado.tipo = local.tipo;
        resultado.descricao = local.descricao;
        resultado.cidade = local.cidade;
        resultado.uf = local.uf;
        resultado.endereco = local.endereco;
        resultado.telefone = local.telefone;
        resultado.fonte = local.fonte;
        resultado.observacao = local.observacao;

        return resultado;
    }

    private List<LocalAtendimentoFallback> carregarLocaisFallback() {
        List<LocalAtendimentoFallback> listaVazia = new ArrayList<LocalAtendimentoFallback>();

        String json = JsonLoader.load("locais_atendimento.json");

        if (json == null || json.trim().isEmpty()) {
            System.out.println("[LOCAL_ATENDIMENTO] Arquivo locais_atendimento.json nao encontrado ou vazio");
            return listaVazia;
        }

        try {
            Gson gson = new Gson();
            Type tipoLista = new TypeToken<List<LocalAtendimentoFallback>>() {}.getType();

            List<LocalAtendimentoFallback> locais = gson.fromJson(json, tipoLista);

            if (locais == null) {
                return listaVazia;
            }

            return locais;

        } catch (Exception e) {
            System.out.println("[LOCAL_ATENDIMENTO] Erro ao ler locais_atendimento.json: " + e.getMessage());
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

    private String valorSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
    }

    private static class LocalAtendimentoFallback {
        String nome;
        String tipo;
        String descricao;
        String cidade;
        String uf;
        String endereco;
        String telefone;
        String fonte;
        String observacao;
    }
}
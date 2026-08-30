package br.com.pibic.agentes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.pibic.api.LocalAtendimentoResponse;
import br.com.pibic.utils.JsonLoader;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteLocalAtendimento
        extends Agent {

    private static final int LIMITE_EXIBICAO =
            10;

    private static final int RAIO_PADRAO_METROS =
            8000;

    private final Gson gson =
            new Gson();

    @Override
    protected void setup() {

        System.out.println(
                "Agente Local Atendimento iniciado: "
                + getLocalName()
        );

        addBehaviour(
                new CyclicBehaviour() {

                    @Override
                    public void action() {

                        ACLMessage mensagem =
                                receive();

                        if (mensagem == null) {

                            block();
                            return;
                        }

                        String conteudo =
                                mensagem.getContent();

                        String cidade =
                                extrairValor(
                                        conteudo,
                                        "cidade"
                                );

                        String uf =
                                extrairValor(
                                        conteudo,
                                        "uf"
                                );

                        double latitude =
                                extrairDouble(
                                        conteudo,
                                        "latitude"
                                );

                        double longitude =
                                extrairDouble(
                                        conteudo,
                                        "longitude"
                                );

                        int raioMetros =
                                extrairInt(
                                        conteudo,
                                        "raioMetros",
                                        RAIO_PADRAO_METROS
                                );

                        String formato =
                                extrairValor(
                                        conteudo,
                                        "formato"
                                );

                        System.out.println(
                                "\n[LOCAL_ATENDIMENTO] Requisicao recebida:"
                        );

                        System.out.println(
                                conteudo
                        );

                        ResultadoBusca resultado =
                                buscarLocais(
                                        cidade,
                                        uf,
                                        latitude,
                                        longitude,
                                        raioMetros
                                );

                        String resposta;

                        if ("json".equalsIgnoreCase(
                                formato)) {

                            resposta =
                                    gson.toJson(
                                            LocalAtendimentoResponse
                                                    .sucesso(
                                                            resultado.cidade,
                                                            resultado.uf,
                                                            resultado.locais
                                                    )
                                    );

                        } else {

                            resposta =
                                    montarRespostaTexto(
                                            resultado
                                    );
                        }

                        ACLMessage reply =
                                mensagem.createReply();

                        reply.setPerformative(
                                ACLMessage.INFORM
                        );

                        reply.setContent(
                                resposta
                        );

                        send(reply);
                    }
                }
        );
    }

    private ResultadoBusca buscarLocais(
            String cidade,
            String uf,
            double latitude,
            double longitude,
            int raioMetros) {

        String cidadeBusca =
                cidade == null
                        ? ""
                        : cidade.trim();

        String ufBusca =
                uf == null
                        ? ""
                        : uf.trim();

        boolean possuiCoordenadas =
                coordenadasValidas(
                        latitude,
                        longitude
                );

        /*
         * Mantemos o comportamento historico apenas
         * quando nenhuma localizacao foi informada.
         */
        if (cidadeBusca.isEmpty()
                && ufBusca.isEmpty()
                && !possuiCoordenadas) {

            cidadeBusca =
                    "Brasilia";

            ufBusca =
                    "DF";
        }

        List<LocalAtendimentoResultado> todos =
                new ArrayList<LocalAtendimentoResultado>();

        /*
         * 1. Fonte institucional primeiro.
         */
        if (!cidadeBusca.isEmpty()
                && !ufBusca.isEmpty()) {

            adicionarResultados(
                    todos,
                    ClienteCnesApi.buscarLocais(
                            cidadeBusca,
                            ufBusca
                    ),
                    "CNES / Dados Abertos SUS"
            );
        }

        /*
         * 2. Base geografica aberta.
         */
        if (possuiCoordenadas) {

            adicionarResultados(
                    todos,
                    ClienteOverpassApi
                            .buscarLocaisProximos(
                                    latitude,
                                    longitude,
                                    raioMetros,
                                    cidadeBusca,
                                    ufBusca
                            ),
                    "OpenStreetMap / Overpass"
            );

        } else if (!cidadeBusca.isEmpty()
                && !ufBusca.isEmpty()) {

            adicionarResultados(
                    todos,
                    ClienteOverpassApi
                            .buscarLocais(
                                    cidadeBusca,
                                    ufBusca
                            ),
                    "OpenStreetMap / Overpass"
            );
        }

        /*
         * 3. Google permanece complementar e opcional.
         * Sem GOOGLE_PLACES_API_KEY, o proprio cliente
         * apenas retorna lista vazia.
         */
        if (!cidadeBusca.isEmpty()
                && !ufBusca.isEmpty()) {

            adicionarResultados(
                    todos,
                    ClienteGooglePlacesApi
                            .buscarLocais(
                                    cidadeBusca,
                                    ufBusca
                            ),
                    "Google Places"
            );
        }

        if (possuiCoordenadas) {

            preencherDistancias(
                    todos,
                    latitude,
                    longitude
            );
        }

        List<LocalAtendimentoResultado> consolidados =
                ClassificadorLocalAtendimento
                        .deduplicarOrdenarLimitar(
                                todos,
                                LIMITE_EXIBICAO
                        );

        if (!consolidados.isEmpty()) {

            return new ResultadoBusca(
                    cidadeBusca,
                    ufBusca,
                    consolidados
            );
        }

        System.out.println(
                "[LOCAL_ATENDIMENTO] Nenhuma fonte externa "
                + "retornou dados relevantes. Usando fallback JSON."
        );

        List<LocalAtendimentoResultado> fallback =
                buscarLocaisFallbackJson(
                        cidadeBusca,
                        ufBusca
                );

        if (possuiCoordenadas) {

            preencherDistancias(
                    fallback,
                    latitude,
                    longitude
            );
        }

        return new ResultadoBusca(
                cidadeBusca,
                ufBusca,
                ClassificadorLocalAtendimento
                        .deduplicarOrdenarLimitar(
                                fallback,
                                LIMITE_EXIBICAO
                        )
        );
    }

    private void preencherDistancias(
            List<LocalAtendimentoResultado> locais,
            double latitude,
            double longitude) {

        for (LocalAtendimentoResultado local : locais) {

            if (local == null) {
                continue;
            }

            if (!coordenadasValidas(
                    local.latitude,
                    local.longitude)) {

                continue;
            }

            local.distanciaKm =
                    ClassificadorLocalAtendimento
                            .calcularDistanciaKm(
                                    latitude,
                                    longitude,
                                    local.latitude,
                                    local.longitude
                            );
        }
    }

    private void adicionarResultados(
            List<LocalAtendimentoResultado> destino,
            List<LocalAtendimentoResultado> origem,
            String nomeFonte) {

        if (origem == null
                || origem.isEmpty()) {

            System.out.println(
                    "[LOCAL_ATENDIMENTO] Fonte sem resultados: "
                    + nomeFonte
            );

            return;
        }

        System.out.println(
                "[LOCAL_ATENDIMENTO] Fonte com resultados: "
                + nomeFonte
                + " -> "
                + origem.size()
        );

        destino.addAll(origem);
    }

    private String montarRespostaTexto(
            ResultadoBusca resultado) {

        if (resultado.locais.isEmpty()) {

            return "Nenhum local de atendimento foi encontrado "
                    + "para a localizacao informada.\n\n"
                    + "Os dados de estabelecimentos podem mudar. "
                    + "Consulte os canais oficiais antes do deslocamento.";
        }

        StringBuilder resposta =
                new StringBuilder();

        resposta.append(
                "Locais de atendimento encontrados:\n\n"
        );

        if (!resultado.cidade.isEmpty()
                || !resultado.uf.isEmpty()) {

            resposta.append(
                    "Cidade/UF considerada: "
            )
            .append(
                    valorSeguro(
                            resultado.cidade
                    )
            )
            .append("/")
            .append(
                    valorSeguro(
                            resultado.uf
                    )
            )
            .append("\n\n");
        }

        resposta.append(
                "A busca prioriza CAPS e servicos especializados. "
        );

        resposta.append(
                "CNES e usado como fonte institucional; "
                + "OpenStreetMap e Google Places podem complementar "
                + "informacoes geograficas e de contato.\n\n"
        );

        resposta.append(
                "Confirme funcionamento, endereco e contato antes "
                + "de se deslocar.\n\n"
        );

        for (int i = 0;
             i < resultado.locais.size();
             i++) {

            LocalAtendimentoResultado local =
                    resultado.locais.get(i);

            resposta.append(
                    i + 1
            )
            .append(". ")
            .append(
                    valorSeguro(
                            local.nome
                    )
            )
            .append("\n");

            resposta.append(
                    "   Categoria: "
            )
            .append(
                    valorSeguro(
                            local.categoria
                    )
            )
            .append("\n");

            resposta.append(
                    "   Tipo: "
            )
            .append(
                    ClassificadorLocalAtendimento
                            .obterTipoParaExibicao(
                                    local
                            )
            )
            .append("\n");

            resposta.append(
                    "   Endereco: "
            )
            .append(
                    valorSeguro(
                            local.endereco
                    )
            )
            .append("\n");

            resposta.append(
                    "   Telefone: "
            )
            .append(
                    valorSeguro(
                            local.telefone
                    )
            )
            .append("\n");

            if (local.distanciaKm >= 0.0) {

                resposta.append(
                        "   Distancia aproximada: "
                )
                .append(
                        String.format(
                                java.util.Locale.US,
                                "%.1f km",
                                local.distanciaKm
                        )
                )
                .append("\n");
            }

            if (local.codigoCnes != null
                    && !local.codigoCnes
                    .trim()
                    .isEmpty()) {

                resposta.append(
                        "   Codigo CNES: "
                )
                .append(
                        local.codigoCnes
                )
                .append("\n");
            }

            if (local.link != null
                    && !local.link
                    .trim()
                    .isEmpty()) {

                resposta.append(
                        "   Link: "
                )
                .append(
                        local.link
                )
                .append("\n");
            }

            resposta.append(
                    "   Fonte: "
            )
            .append(
                    valorSeguro(
                            local.fonte
                    )
            )
            .append("\n\n");
        }

        return resposta.toString();
    }

    private List<LocalAtendimentoResultado> buscarLocaisFallbackJson(
            String cidade,
            String uf) {

        List<LocalAtendimentoFallback> locais =
                carregarLocaisFallback();

        List<LocalAtendimentoResultado> resultados =
                new ArrayList<LocalAtendimentoResultado>();

        for (LocalAtendimentoFallback local : locais) {

            if (local == null) {
                continue;
            }

            boolean mesmaCidade =
                    cidade != null
                    && local.cidade != null
                    && local.cidade
                    .equalsIgnoreCase(cidade);

            boolean mesmoUf =
                    uf != null
                    && local.uf != null
                    && local.uf
                    .equalsIgnoreCase(uf);

            if (!mesmaCidade
                    || !mesmoUf) {

                continue;
            }

            LocalAtendimentoResultado resultado =
                    converterFallback(local);

            if (ClassificadorLocalAtendimento
                    .classificar(resultado)) {

                resultados.add(resultado);
            }
        }

        return resultados;
    }

    private LocalAtendimentoResultado converterFallback(
            LocalAtendimentoFallback local) {

        LocalAtendimentoResultado resultado =
                new LocalAtendimentoResultado();

        resultado.nome =
                local.nome;

        resultado.tipo =
                local.tipo;

        resultado.descricao =
                local.descricao;

        resultado.cidade =
                local.cidade;

        resultado.uf =
                local.uf;

        resultado.endereco =
                local.endereco;

        resultado.telefone =
                local.telefone;

        resultado.fonte =
                local.fonte;

        resultado.observacao =
                local.observacao;

        return resultado;
    }

    private List<LocalAtendimentoFallback> carregarLocaisFallback() {

        List<LocalAtendimentoFallback> listaVazia =
                new ArrayList<LocalAtendimentoFallback>();

        String json =
                JsonLoader.load(
                        "locais_atendimento.json"
                );

        if (json == null
                || json.trim().isEmpty()) {

            return listaVazia;
        }

        try {

            Type tipoLista =
                    new TypeToken<List<LocalAtendimentoFallback>>() {
                    }.getType();

            List<LocalAtendimentoFallback> locais =
                    gson.fromJson(
                            json,
                            tipoLista
                    );

            return locais == null
                    ? listaVazia
                    : locais;

        } catch (Exception e) {

            System.out.println(
                    "[LOCAL_ATENDIMENTO] Erro ao ler fallback: "
                    + e.getMessage()
            );

            return listaVazia;
        }
    }

    private String extrairValor(
            String texto,
            String chave) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return "";
        }

        String[] partes =
                texto.split(";");

        for (String parte : partes) {

            String[] kv =
                    parte.split(
                            "=",
                            2
                    );

            if (kv.length == 2
                    && kv[0].trim()
                    .equalsIgnoreCase(chave)) {

                return kv[1].trim();
            }
        }

        return "";
    }

    private double extrairDouble(
            String texto,
            String chave) {

        String valor =
                extrairValor(
                        texto,
                        chave
                );

        if (valor.isEmpty()) {
            return 0.0;
        }

        try {

            return Double.parseDouble(
                    valor.replace(
                            ",",
                            "."
                    )
            );

        } catch (Exception e) {

            return 0.0;
        }
    }

    private int extrairInt(
            String texto,
            String chave,
            int padrao) {

        String valor =
                extrairValor(
                        texto,
                        chave
                );

        if (valor.isEmpty()) {
            return padrao;
        }

        try {

            return Integer.parseInt(
                    valor
            );

        } catch (Exception e) {

            return padrao;
        }
    }

    private boolean coordenadasValidas(
            double latitude,
            double longitude) {

        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0
                && !(latitude == 0.0
                && longitude == 0.0);
    }

    private String valorSeguro(
            String valor) {

        if (valor == null
                || valor.trim().isEmpty()) {

            return "Nao informado";
        }

        return valor;
    }

    private static class ResultadoBusca {

        final String cidade;
        final String uf;
        final List<LocalAtendimentoResultado> locais;

        ResultadoBusca(
                String cidade,
                String uf,
                List<LocalAtendimentoResultado> locais) {

            this.cidade =
                    cidade == null
                            ? ""
                            : cidade;

            this.uf =
                    uf == null
                            ? ""
                            : uf;

            this.locais =
                    locais == null
                            ? new ArrayList<LocalAtendimentoResultado>()
                            : locais;
        }
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

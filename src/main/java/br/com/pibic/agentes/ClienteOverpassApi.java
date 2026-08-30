package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClienteOverpassApi {

    private static final String[] OVERPASS_URLS = {
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass-api.de/api/interpreter"
    };

    private static final int RAIO_PADRAO_METROS =
            8000;

    public static List<LocalAtendimentoResultado> buscarLocais(
            String cidade,
            String uf) {

        List<LocalAtendimentoResultado> locais =
                new ArrayList<LocalAtendimentoResultado>();

        String bbox =
                obterBbox(cidade, uf);

        if (bbox.isEmpty()) {

            System.out.println(
                    "[OVERPASS] Cidade/UF sem bbox local. "
                    + "A busca por coordenadas e preferencial."
            );

            return locais;
        }

        return executarConsultas(
                montarConsultasBbox(bbox),
                cidade,
                uf
        );
    }

    public static List<LocalAtendimentoResultado> buscarLocaisProximos(
            double latitude,
            double longitude,
            int raioMetros,
            String cidade,
            String uf) {

        if (!coordenadasValidas(
                latitude,
                longitude)) {

            return new ArrayList<LocalAtendimentoResultado>();
        }

        int raio =
                raioMetros > 0
                        ? Math.min(
                                raioMetros,
                                30000
                        )
                        : RAIO_PADRAO_METROS;

        String around =
                String.format(
                        Locale.US,
                        "around:%d,%.7f,%.7f",
                        raio,
                        latitude,
                        longitude
                );

        System.out.println(
                "[OVERPASS] Busca por proximidade. raio="
                + raio
                + "m lat="
                + latitude
                + " lon="
                + longitude
        );

        return executarConsultas(
                montarConsultasAround(around),
                cidade,
                uf
        );
    }

    private static List<LocalAtendimentoResultado> executarConsultas(
            List<String> consultas,
            String cidade,
            String uf) {

        List<LocalAtendimentoResultado> locais =
                new ArrayList<LocalAtendimentoResultado>();

        for (String query : consultas) {

            try {

                String respostaJson =
                        executarConsulta(query);

                if (respostaJson == null
                        || respostaJson.trim().isEmpty()) {

                    continue;
                }

                List<LocalAtendimentoResultado> encontrados =
                        extrairLocais(
                                respostaJson,
                                cidade,
                                uf
                        );

                locais.addAll(encontrados);

            } catch (Exception e) {

                System.out.println(
                        "[OVERPASS] Erro: "
                        + e.getMessage()
                );
            }
        }

        System.out.println(
                "[OVERPASS] Locais relevantes encontrados: "
                + locais.size()
        );

        return locais;
    }

    private static List<String> montarConsultasAround(
            String around) {

        List<String> consultas =
                new ArrayList<String>();

        consultas.add(
                montarConsultaCombinada(
                        around
                )
        );

        return consultas;
    }

    private static List<String> montarConsultasBbox(
            String bbox) {

        List<String> consultas =
                new ArrayList<String>();

        consultas.add(
                montarConsultaCombinada(
                        bbox
                )
        );

        return consultas;
    }

    private static String montarConsultaCombinada(
            String area) {

        return "[out:json][timeout:15];"
                + "("
                + "node[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial|saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + area + ");"
                + "way[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial|saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + area + ");"
                + "relation[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial|saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + area + ");"
                + "node[\"healthcare\"=\"psychotherapist\"](" + area + ");"
                + "way[\"healthcare\"=\"psychotherapist\"](" + area + ");"
                + "relation[\"healthcare\"=\"psychotherapist\"](" + area + ");"
                + "node[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + area + ");"
                + "way[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + area + ");"
                + "relation[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + area + ");"
                + ");"
                + "out center 60;";
    }

    private static String executarConsulta(
            String query)
            throws Exception {

        for (String endpoint : OVERPASS_URLS) {

            HttpURLConnection conexao = null;

            try {

                System.out.println(
                        "[OVERPASS] Tentando endpoint: "
                        + endpoint
                );

                URL url =
                        new URL(endpoint);

                conexao =
                        (HttpURLConnection)
                        url.openConnection();

                conexao.setRequestMethod("POST");
                conexao.setConnectTimeout(8000);
                conexao.setReadTimeout(18000);
                conexao.setDoOutput(true);

                conexao.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                );

                String corpo =
                        "data="
                        + URLEncoder.encode(
                                query,
                                "UTF-8"
                        );

                OutputStream os =
                        conexao.getOutputStream();

                os.write(
                        corpo.getBytes("UTF-8")
                );

                os.flush();
                os.close();

                int statusCode =
                        conexao.getResponseCode();

                if (statusCode >= 200
                        && statusCode < 300) {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            conexao.getInputStream(),
                                            "UTF-8"
                                    )
                            );

                    StringBuilder resposta =
                            new StringBuilder();

                    String linha;

                    while ((linha =
                            reader.readLine())
                            != null) {

                        resposta.append(linha);
                    }

                    reader.close();

                    return resposta.toString();
                }

                System.out.println(
                        "[OVERPASS] Endpoint retornou HTTP "
                        + statusCode
                );

            } catch (Exception e) {

                System.out.println(
                        "[OVERPASS] Falha no endpoint: "
                        + endpoint
                        + " | "
                        + e.getMessage()
                );

            } finally {

                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        }

        return "";
    }

    private static List<LocalAtendimentoResultado> extrairLocais(
            String json,
            String cidade,
            String uf) {

        List<LocalAtendimentoResultado> locais =
                new ArrayList<LocalAtendimentoResultado>();

        try {

            JsonObject raiz =
                    JsonParser.parseString(json)
                            .getAsJsonObject();

            if (!raiz.has("elements")
                    || !raiz.get("elements")
                    .isJsonArray()) {

                return locais;
            }

            JsonArray elements =
                    raiz.getAsJsonArray("elements");

            for (JsonElement elemento : elements) {

                JsonObject obj =
                        elemento.getAsJsonObject();

                if (!obj.has("tags")
                        || !obj.get("tags")
                        .isJsonObject()) {

                    continue;
                }

                JsonObject tags =
                        obj.getAsJsonObject("tags");

                LocalAtendimentoResultado local =
                        new LocalAtendimentoResultado();

                local.idExterno =
                        extrairIdOsm(obj);

                local.nome =
                        valorTag(
                                tags,
                                "name"
                        );

                local.tipo =
                        montarTipo(tags);

                local.endereco =
                        montarEndereco(tags);

                local.telefone =
                        primeiroValorTag(
                                tags,
                                "phone",
                                "contact:phone"
                        );

                local.cidade =
                        primeiroValorTag(
                                tags,
                                "addr:city"
                        );

                if (local.cidade.isEmpty()) {
                    local.cidade =
                            cidade == null
                                    ? ""
                                    : cidade;
                }

                local.uf =
                        primeiroValorTag(
                                tags,
                                "addr:state"
                        );

                if (local.uf.isEmpty()) {
                    local.uf =
                            uf == null
                                    ? ""
                                    : uf;
                }

                local.codigoCnes = "";
                local.fonte =
                        "OpenStreetMap / Overpass API";

                local.observacao =
                        "Base aberta colaborativa. "
                        + "Confirmar funcionamento e contato antes do deslocamento.";

                preencherCoordenadas(
                        obj,
                        local
                );

                if (ClassificadorLocalAtendimento
                        .classificar(local)) {

                    locais.add(local);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "[OVERPASS] Erro ao interpretar JSON: "
                    + e.getMessage()
            );
        }

        return locais;
    }

    private static String extrairIdOsm(
            JsonObject obj) {

        String tipo =
                obj.has("type")
                        ? obj.get("type").getAsString()
                        : "osm";

        String id =
                obj.has("id")
                        ? obj.get("id").getAsString()
                        : "";

        return tipo + ":" + id;
    }

    private static void preencherCoordenadas(
            JsonObject obj,
            LocalAtendimentoResultado local) {

        if (obj.has("lat")
                && !obj.get("lat").isJsonNull()) {

            local.latitude =
                    obj.get("lat")
                            .getAsDouble();
        }

        if (obj.has("lon")
                && !obj.get("lon").isJsonNull()) {

            local.longitude =
                    obj.get("lon")
                            .getAsDouble();
        }

        if (obj.has("center")
                && obj.get("center")
                .isJsonObject()) {

            JsonObject center =
                    obj.getAsJsonObject(
                            "center"
                    );

            if (center.has("lat")
                    && !center.get("lat")
                    .isJsonNull()) {

                local.latitude =
                        center.get("lat")
                                .getAsDouble();
            }

            if (center.has("lon")
                    && !center.get("lon")
                    .isJsonNull()) {

                local.longitude =
                        center.get("lon")
                                .getAsDouble();
            }
        }
    }

    private static String montarTipo(
            JsonObject tags) {

        String healthcare =
                valorTag(
                        tags,
                        "healthcare"
                );

        String speciality =
                valorTag(
                        tags,
                        "healthcare:speciality"
                );

        String amenity =
                valorTag(
                        tags,
                        "amenity"
                );

        StringBuilder tipo =
                new StringBuilder();

        if (!healthcare.isEmpty()) {
            tipo.append(
                    "healthcare="
            ).append(healthcare);
        }

        if (!speciality.isEmpty()) {

            if (tipo.length() > 0) {
                tipo.append(", ");
            }

            tipo.append(
                    "speciality="
            ).append(speciality);
        }

        if (!amenity.isEmpty()) {

            if (tipo.length() > 0) {
                tipo.append(", ");
            }

            tipo.append(
                    "amenity="
            ).append(amenity);
        }

        return tipo.length() == 0
                ? "Nao informado"
                : tipo.toString();
    }

    private static String montarEndereco(
            JsonObject tags) {

        String rua =
                primeiroValorTag(
                        tags,
                        "addr:street",
                        "addr:place"
                );

        String numero =
                valorTag(
                        tags,
                        "addr:housenumber"
                );

        String bairro =
                valorTag(
                        tags,
                        "addr:suburb"
                );

        StringBuilder endereco =
                new StringBuilder();

        if (!rua.isEmpty()) {
            endereco.append(rua);
        }

        if (!numero.isEmpty()) {

            if (endereco.length() > 0) {
                endereco.append(", ");
            }

            endereco.append(numero);
        }

        if (!bairro.isEmpty()) {

            if (endereco.length() > 0) {
                endereco.append(" - ");
            }

            endereco.append(bairro);
        }

        return endereco.length() == 0
                ? "Endereco nao informado no OpenStreetMap"
                : endereco.toString();
    }

    private static boolean coordenadasValidas(
            double latitude,
            double longitude) {

        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0
                && !(latitude == 0.0
                && longitude == 0.0);
    }

    private static String obterBbox(
            String cidade,
            String uf) {

        String cidadeNormalizada =
                ClassificadorLocalAtendimento
                        .normalizar(cidade);

        String ufNormalizada =
                ClassificadorLocalAtendimento
                        .normalizar(uf)
                        .toUpperCase();

        if (cidadeNormalizada.equals("brasilia")
                && ufNormalizada.equals("DF")) {
            return "-15.9500,-48.1500,-15.6000,-47.6500";
        }

        if (cidadeNormalizada.equals("sao paulo")
                && ufNormalizada.equals("SP")) {
            return "-24.1000,-46.9000,-23.3000,-46.3000";
        }

        if (cidadeNormalizada.equals("rio de janeiro")
                && ufNormalizada.equals("RJ")) {
            return "-23.1000,-43.8000,-22.7000,-43.0000";
        }

        if (cidadeNormalizada.equals("belo horizonte")
                && ufNormalizada.equals("MG")) {
            return "-20.1000,-44.1000,-19.7000,-43.8000";
        }

        if (cidadeNormalizada.equals("goiania")
                && ufNormalizada.equals("GO")) {
            return "-16.9000,-49.5000,-16.5000,-49.1000";
        }

        return "";
    }

    private static String primeiroValorTag(
            JsonObject tags,
            String... chaves) {

        for (String chave : chaves) {

            String valor =
                    valorTag(
                            tags,
                            chave
                    );

            if (!valor.isEmpty()) {
                return valor;
            }
        }

        return "";
    }

    private static String valorTag(
            JsonObject tags,
            String chave) {

        if (tags.has(chave)
                && !tags.get(chave)
                .isJsonNull()) {

            return tags.get(chave)
                    .getAsString();
        }

        return "";
    }
}

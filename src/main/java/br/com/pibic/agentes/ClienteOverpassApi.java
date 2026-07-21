package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClienteOverpassApi {

    private static final String[] OVERPASS_URLS = {
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass-api.de/api/interpreter",
            "https://overpass.openstreetmap.ru/api/interpreter"
    };

    public static List<LocalAtendimentoResultado> buscarLocais(String cidade, String uf) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        String bbox = obterBbox(cidade, uf);

        if (bbox.isEmpty()) {
            System.out.println("[OVERPASS] Cidade/UF ainda nao mapeada para bbox: " + cidade + "/" + uf);
            return locais;
        }

        List<String> consultas = montarConsultasOverpass(bbox);

        for (String query : consultas) {
            try {
                System.out.println("[OVERPASS] Consultando OpenStreetMap/Overpass para " + cidade + "/" + uf);

                String respostaJson = executarConsulta(query);

                if (respostaJson == null || respostaJson.trim().isEmpty()) {
                    continue;
                }

                List<LocalAtendimentoResultado> encontrados = extrairLocais(respostaJson, cidade, uf);

                for (LocalAtendimentoResultado local : encontrados) {
                    locais.add(local);
                }

                if (!locais.isEmpty()) {
                    break;
                }

            } catch (Exception e) {
                System.out.println("[OVERPASS] Erro ao consultar Overpass: " + e.getMessage());
            }
        }

        System.out.println("[OVERPASS] Locais relevantes encontrados: " + locais.size());

        return locais;
    }

    private static List<String> montarConsultasOverpass(String bbox) {
        List<String> consultas = new ArrayList<String>();

        String consultaCaps = "[out:json][timeout:20];"
                + "("
                + "node[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial\",i](" + bbox + ");"
                + "way[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial\",i](" + bbox + ");"
                + "relation[\"name\"~\"CAPS|Centro de Atenção Psicossocial|Centro de Atencao Psicossocial\",i](" + bbox + ");"
                + ");"
                + "out center 25;";

        String consultaSaudeMental = "[out:json][timeout:20];"
                + "("
                + "node[\"name\"~\"saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + bbox + ");"
                + "way[\"name\"~\"saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + bbox + ");"
                + "relation[\"name\"~\"saude mental|saúde mental|psicossocial|psiquiatr|psicolog\",i](" + bbox + ");"
                + ");"
                + "out center 25;";

        String consultaHealthcare = "[out:json][timeout:20];"
                + "("
                + "node[\"healthcare\"=\"psychotherapist\"](" + bbox + ");"
                + "way[\"healthcare\"=\"psychotherapist\"](" + bbox + ");"
                + "relation[\"healthcare\"=\"psychotherapist\"](" + bbox + ");"
                + "node[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + bbox + ");"
                + "way[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + bbox + ");"
                + "relation[\"healthcare:speciality\"~\"psychiatry|psychotherapy|psychology\",i](" + bbox + ");"
                + ");"
                + "out center 25;";

        consultas.add(consultaCaps);
        consultas.add(consultaSaudeMental);
        consultas.add(consultaHealthcare);

        return consultas;
    }

    private static String executarConsulta(String query) throws Exception {
        for (String endpoint : OVERPASS_URLS) {
            try {
                System.out.println("[OVERPASS] Tentando endpoint: " + endpoint);

                URL url = new URL(endpoint);

                HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
                conexao.setRequestMethod("POST");
                conexao.setConnectTimeout(20000);
                conexao.setReadTimeout(30000);
                conexao.setDoOutput(true);
                conexao.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                String corpo = "data=" + URLEncoder.encode(query, "UTF-8");

                OutputStream os = conexao.getOutputStream();
                os.write(corpo.getBytes("UTF-8"));
                os.flush();
                os.close();

                int statusCode = conexao.getResponseCode();

                if (statusCode >= 200 && statusCode < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conexao.getInputStream(), "UTF-8"));

                    StringBuilder resposta = new StringBuilder();
                    String linha;

                    while ((linha = reader.readLine()) != null) {
                        resposta.append(linha);
                    }

                    reader.close();

                    return resposta.toString();
                }

                System.out.println("[OVERPASS] Endpoint retornou HTTP " + statusCode);

            } catch (Exception e) {
                System.out.println("[OVERPASS] Falha no endpoint: " + endpoint + " | " + e.getMessage());
            }
        }

        return "";
    }

    private static List<LocalAtendimentoResultado> extrairLocais(String json, String cidade, String uf) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        try {
            JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();

            if (!raiz.has("elements") || !raiz.get("elements").isJsonArray()) {
                return locais;
            }

            JsonArray elements = raiz.getAsJsonArray("elements");

            for (JsonElement elemento : elements) {
                JsonObject obj = elemento.getAsJsonObject();

                if (!obj.has("tags") || !obj.get("tags").isJsonObject()) {
                    continue;
                }

                JsonObject tags = obj.getAsJsonObject("tags");

                LocalAtendimentoResultado local = new LocalAtendimentoResultado();

                local.idExterno = extrairIdOsm(obj);
                local.nome = valorTag(tags, "name");
                local.tipo = montarTipo(tags);
                local.endereco = montarEndereco(tags);
                local.telefone = primeiroValorTag(tags, "phone", "contact:phone", "telefone");
                local.cidade = cidade;
                local.uf = uf;
                local.codigoCnes = "";
                local.fonte = "OpenStreetMap / Overpass API";
                local.observacao = "Dado obtido por base aberta colaborativa. Validar funcionamento, endereco e contato antes de qualquer uso real.";

                preencherCoordenadas(obj, local);

                boolean relevante = ClassificadorLocalAtendimento.classificar(local);

                if (relevante) {
                    locais.add(local);
                }
            }

        } catch (Exception e) {
            System.out.println("[OVERPASS] Erro ao interpretar JSON: " + e.getMessage());
        }

        return locais;
    }

    private static String extrairIdOsm(JsonObject obj) {
        String tipo = obj.has("type") ? obj.get("type").getAsString() : "osm";
        String id = obj.has("id") ? obj.get("id").getAsString() : "";

        return tipo + ":" + id;
    }

    private static void preencherCoordenadas(JsonObject obj, LocalAtendimentoResultado local) {
        if (obj.has("lat") && !obj.get("lat").isJsonNull()) {
            local.latitude = obj.get("lat").getAsDouble();
        }

        if (obj.has("lon") && !obj.get("lon").isJsonNull()) {
            local.longitude = obj.get("lon").getAsDouble();
        }

        if (obj.has("center") && obj.get("center").isJsonObject()) {
            JsonObject center = obj.getAsJsonObject("center");

            if (center.has("lat") && !center.get("lat").isJsonNull()) {
                local.latitude = center.get("lat").getAsDouble();
            }

            if (center.has("lon") && !center.get("lon").isJsonNull()) {
                local.longitude = center.get("lon").getAsDouble();
            }
        }
    }

    private static String montarTipo(JsonObject tags) {
        String healthcare = valorTag(tags, "healthcare");
        String speciality = valorTag(tags, "healthcare:speciality");
        String amenity = valorTag(tags, "amenity");

        StringBuilder tipo = new StringBuilder();

        if (!healthcare.isEmpty()) {
            tipo.append("healthcare=").append(healthcare);
        }

        if (!speciality.isEmpty()) {
            if (tipo.length() > 0) {
                tipo.append(", ");
            }

            tipo.append("speciality=").append(speciality);
        }

        if (!amenity.isEmpty()) {
            if (tipo.length() > 0) {
                tipo.append(", ");
            }

            tipo.append("amenity=").append(amenity);
        }

        if (tipo.length() == 0) {
            return "Nao informado";
        }

        return tipo.toString();
    }

    private static String montarEndereco(JsonObject tags) {
        String rua = primeiroValorTag(tags, "addr:street", "addr:place");
        String numero = valorTag(tags, "addr:housenumber");
        String bairro = valorTag(tags, "addr:suburb");

        StringBuilder endereco = new StringBuilder();

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

        if (endereco.length() == 0) {
            return "Endereco nao informado no OpenStreetMap";
        }

        return endereco.toString();
    }

    private static String obterBbox(String cidade, String uf) {
        String cidadeNormalizada = ClassificadorLocalAtendimento.normalizar(cidade);
        String ufNormalizada = ClassificadorLocalAtendimento.normalizar(uf).toUpperCase();

        if (cidadeNormalizada.equals("brasilia") && ufNormalizada.equals("DF")) {
            return "-15.9500,-48.1500,-15.6000,-47.6500";
        }

        if (cidadeNormalizada.equals("sao paulo") && ufNormalizada.equals("SP")) {
            return "-24.1000,-46.9000,-23.3000,-46.3000";
        }

        if (cidadeNormalizada.equals("rio de janeiro") && ufNormalizada.equals("RJ")) {
            return "-23.1000,-43.8000,-22.7000,-43.0000";
        }

        if (cidadeNormalizada.equals("belo horizonte") && ufNormalizada.equals("MG")) {
            return "-20.1000,-44.1000,-19.7000,-43.8000";
        }

        if (cidadeNormalizada.equals("goiania") && ufNormalizada.equals("GO")) {
            return "-16.9000,-49.5000,-16.5000,-49.1000";
        }

        return "";
    }

    private static String primeiroValorTag(JsonObject tags, String... chaves) {
        for (String chave : chaves) {
            String valor = valorTag(tags, chave);

            if (!valor.isEmpty()) {
                return valor;
            }
        }

        return "";
    }

    private static String valorTag(JsonObject tags, String chave) {
        if (tags.has(chave) && !tags.get(chave).isJsonNull()) {
            return tags.get(chave).getAsString();
        }

        return "";
    }
}
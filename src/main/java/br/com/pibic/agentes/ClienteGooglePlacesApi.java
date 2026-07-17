package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClienteGooglePlacesApi {

    private static final String URL_TEXT_SEARCH = "https://places.googleapis.com/v1/places:searchText";

    public static List<LocalAtendimentoResultado> buscarLocais(String cidade, String uf) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        String apiKey = System.getenv("GOOGLE_PLACES_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[GOOGLE_PLACES] GOOGLE_PLACES_API_KEY nao configurada. Pulando Google Places.");
            return locais;
        }

        List<String> consultas = montarConsultas(cidade, uf);

        for (String consulta : consultas) {
            try {
                System.out.println("[GOOGLE_PLACES] Consultando: " + consulta);

                String respostaJson = executarTextSearch(consulta, apiKey);

                if (respostaJson == null || respostaJson.trim().isEmpty()) {
                    continue;
                }

                List<LocalAtendimentoResultado> encontrados = extrairLocais(respostaJson, cidade, uf);

                for (LocalAtendimentoResultado local : encontrados) {
                    locais.add(local);
                }

            } catch (Exception e) {
                System.out.println("[GOOGLE_PLACES] Erro ao consultar API: " + e.getMessage());
            }
        }

        System.out.println("[GOOGLE_PLACES] Locais relevantes encontrados: " + locais.size());

        return locais;
    }

    private static List<String> montarConsultas(String cidade, String uf) {
        return Arrays.asList(
                "CAPS " + cidade + " " + uf,
                "Centro de Atenção Psicossocial " + cidade + " " + uf,
                "saúde mental " + cidade + " " + uf,
                "psicólogo " + cidade + " " + uf,
                "psiquiatra " + cidade + " " + uf,
                "clínica psicológica " + cidade + " " + uf
        );
    }

    private static String executarTextSearch(String consulta, String apiKey) throws Exception {
        URL url = new URL(URL_TEXT_SEARCH);

        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        conexao.setRequestMethod("POST");
        conexao.setConnectTimeout(10000);
        conexao.setReadTimeout(10000);
        conexao.setDoOutput(true);

        conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conexao.setRequestProperty("X-Goog-Api-Key", apiKey);
        conexao.setRequestProperty(
                "X-Goog-FieldMask",
                "places.id,"
                        + "places.displayName,"
                        + "places.formattedAddress,"
                        + "places.nationalPhoneNumber,"
                        + "places.googleMapsUri,"
                        + "places.types,"
                        + "places.location"
        );

        String corpo = "{"
                + "\"textQuery\":\"" + escaparJson(consulta) + "\","
                + "\"languageCode\":\"pt-BR\","
                + "\"regionCode\":\"BR\","
                + "\"pageSize\":5"
                + "}";

        OutputStream os = conexao.getOutputStream();
        os.write(corpo.getBytes("UTF-8"));
        os.flush();
        os.close();

        int statusCode = conexao.getResponseCode();

        if (statusCode < 200 || statusCode >= 300) {
            System.out.println("[GOOGLE_PLACES] Resposta HTTP inesperada: " + statusCode);

            if (conexao.getErrorStream() != null) {
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conexao.getErrorStream(), "UTF-8")
                );

                StringBuilder erro = new StringBuilder();
                String linhaErro;

                while ((linhaErro = errorReader.readLine()) != null) {
                    erro.append(linhaErro);
                }

                errorReader.close();

                System.out.println("[GOOGLE_PLACES] Erro retornado: " + erro.toString());
            }

            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conexao.getInputStream(), "UTF-8"));

        StringBuilder resposta = new StringBuilder();
        String linha;

        while ((linha = reader.readLine()) != null) {
            resposta.append(linha);
        }

        reader.close();

        return resposta.toString();
    }

    private static List<LocalAtendimentoResultado> extrairLocais(String json, String cidade, String uf) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        try {
            JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();

            if (!raiz.has("places") || !raiz.get("places").isJsonArray()) {
                return locais;
            }

            JsonArray places = raiz.getAsJsonArray("places");

            for (JsonElement elemento : places) {
                JsonObject place = elemento.getAsJsonObject();

                LocalAtendimentoResultado local = new LocalAtendimentoResultado();

                local.idExterno = extrairString(place, "id");
                local.nome = extrairDisplayName(place);
                local.endereco = extrairString(place, "formattedAddress");
                local.telefone = extrairString(place, "nationalPhoneNumber");
                local.link = extrairString(place, "googleMapsUri");
                local.tipo = extrairTipos(place);
                local.cidade = cidade;
                local.uf = uf;
                local.codigoCnes = "";
                local.fonte = "Google Places API";
                local.observacao = "Dado obtido por busca textual no Google Places. Validar funcionamento, endereco e contato antes de qualquer uso real.";

                preencherLocalizacao(place, local);

                boolean relevante = ClassificadorLocalAtendimento.classificar(local);

                if (relevante) {
                    locais.add(local);
                }
            }

        } catch (Exception e) {
            System.out.println("[GOOGLE_PLACES] Erro ao interpretar JSON: " + e.getMessage());
        }

        return locais;
    }

    private static String extrairDisplayName(JsonObject place) {
        if (place.has("displayName") && place.get("displayName").isJsonObject()) {
            JsonObject displayName = place.getAsJsonObject("displayName");

            if (displayName.has("text") && !displayName.get("text").isJsonNull()) {
                return displayName.get("text").getAsString();
            }
        }

        return "";
    }

    private static String extrairTipos(JsonObject place) {
        if (!place.has("types") || !place.get("types").isJsonArray()) {
            return "Nao informado";
        }

        JsonArray tipos = place.getAsJsonArray("types");
        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < tipos.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }

            texto.append(tipos.get(i).getAsString());
        }

        return texto.toString();
    }

    private static void preencherLocalizacao(JsonObject place, LocalAtendimentoResultado local) {
        if (place.has("location") && place.get("location").isJsonObject()) {
            JsonObject location = place.getAsJsonObject("location");

            if (location.has("latitude") && !location.get("latitude").isJsonNull()) {
                local.latitude = location.get("latitude").getAsDouble();
            }

            if (location.has("longitude") && !location.get("longitude").isJsonNull()) {
                local.longitude = location.get("longitude").getAsDouble();
            }
        }
    }

    private static String extrairString(JsonObject obj, String chave) {
        if (obj.has(chave) && !obj.get(chave).isJsonNull()) {
            return obj.get(chave).getAsString();
        }

        return "";
    }

    private static String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolve o codigo IBGE de um municipio usando a API publica
 * de Localidades do IBGE.
 *
 * Isso elimina a necessidade de manter uma tabela manual
 * de codigos de municipios dentro do projeto.
 */
public class ClienteIbgeApi {

    private static final String BASE_URL =
            "https://servicodados.ibge.gov.br/api/v1/localidades/estados/";

    public static List<String> obterCodigosMunicipioPossiveis(
            String cidade,
            String uf) {

        List<String> codigos =
                new ArrayList<String>();

        String cidadeNormalizada =
                ClassificadorLocalAtendimento.normalizar(cidade);

        String ufNormalizada =
                uf == null
                        ? ""
                        : uf.trim().toUpperCase();

        if (cidadeNormalizada.isEmpty()
                || ufNormalizada.length() != 2) {

            return codigos;
        }

        try {

            String url =
                    BASE_URL
                    + ufNormalizada
                    + "/municipios";

            String respostaJson =
                    executarGet(url);

            if (respostaJson == null
                    || respostaJson.trim().isEmpty()) {

                return codigos;
            }

            JsonElement raiz =
                    JsonParser.parseString(
                            respostaJson
                    );

            if (!raiz.isJsonArray()) {
                return codigos;
            }

            JsonArray municipios =
                    raiz.getAsJsonArray();

            for (JsonElement elemento : municipios) {

                if (!elemento.isJsonObject()) {
                    continue;
                }

                JsonObject municipio =
                        elemento.getAsJsonObject();

                String nome =
                        municipio.has("nome")
                                ? municipio
                                    .get("nome")
                                    .getAsString()
                                : "";

                if (!ClassificadorLocalAtendimento
                        .normalizar(nome)
                        .equals(cidadeNormalizada)) {

                    continue;
                }

                String id =
                        municipio.has("id")
                                ? municipio
                                    .get("id")
                                    .getAsString()
                                : "";

                if (!id.isEmpty()) {

                    codigos.add(id);

                    /*
                     * Algumas APIs do CNES historicamente
                     * aceitam a forma de 6 digitos.
                     */
                    if (id.length() == 7) {
                        codigos.add(
                                id.substring(0, 6)
                        );
                    }
                }

                break;
            }

        } catch (Exception e) {

            System.out.println(
                    "[IBGE_API] Erro ao resolver municipio "
                    + cidade
                    + "/"
                    + uf
                    + ": "
                    + e.getMessage()
            );
        }

        return codigos;
    }

    private static String executarGet(
            String urlString)
            throws Exception {

        HttpURLConnection conexao = null;

        try {

            URL url =
                    new URL(urlString);

            conexao =
                    (HttpURLConnection)
                    url.openConnection();

            conexao.setRequestMethod("GET");
            conexao.setConnectTimeout(10000);
            conexao.setReadTimeout(15000);
            conexao.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            int statusCode =
                    conexao.getResponseCode();

            if (statusCode < 200
                    || statusCode >= 300) {

                System.out.println(
                        "[IBGE_API] HTTP "
                        + statusCode
                );

                return "";
            }

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

            while ((linha = reader.readLine())
                    != null) {

                resposta.append(linha);
            }

            reader.close();

            return resposta.toString();

        } finally {

            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }
}

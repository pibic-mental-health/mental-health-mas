package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClienteCnesApi {

    private static final String BASE_URL = "https://apidadosabertos.saude.gov.br/cnes/estabelecimentos";

    public static List<LocalAtendimentoResultado> buscarLocais(String cidade, String uf) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        String codigoUf = obterCodigoUf(uf);
        List<String> codigosMunicipio = obterCodigosMunicipioPossiveis(cidade, uf);

        if (codigoUf.isEmpty() || codigosMunicipio.isEmpty()) {
            System.out.println("[CNES_API] Codigo UF ou municipio nao encontrado para: " + cidade + "/" + uf);
            return locais;
        }

        for (String codigoMunicipio : codigosMunicipio) {
            List<LocalAtendimentoResultado> locaisMunicipio =
                    buscarLocaisPorMunicipio(cidade, uf, codigoUf, codigoMunicipio);

            locais.addAll(locaisMunicipio);
        }

        System.out.println("[CNES_API] Locais relevantes encontrados: " + locais.size());

        return locais;
    }

    private static List<LocalAtendimentoResultado> buscarLocaisPorMunicipio(
            String cidade,
            String uf,
            String codigoUf,
            String codigoMunicipio
    ) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        int limite = 100;
        int offsetMaximo = 1000;

        for (int offset = 0; offset <= offsetMaximo; offset += limite) {
            try {
                String url = BASE_URL
                        + "?codigo_uf=" + codigoUf
                        + "&codigo_municipio=" + codigoMunicipio
                        + "&status=1"
                        + "&limit=" + limite
                        + "&offset=" + offset;

                System.out.println("[CNES_API] Consultando API CNES:");
                System.out.println(url);

                String respostaJson = executarGet(url);

                if (respostaJson == null || respostaJson.trim().isEmpty()) {
                    break;
                }

                List<LocalAtendimentoResultado> encontrados =
                        extrairLocaisDaResposta(respostaJson, cidade, uf);

                if (encontrados.isEmpty()) {
                    break;
                }

                for (LocalAtendimentoResultado local : encontrados) {
                    boolean relevante = ClassificadorLocalAtendimento.classificar(local);

                    if (relevante) {
                        locais.add(local);
                    }
                }

                if (encontrados.size() < limite) {
                    break;
                }

            } catch (Exception e) {
                System.out.println("[CNES_API] Erro ao consultar API: " + e.getMessage());
                break;
            }
        }

        return locais;
    }

    private static String executarGet(String urlString) throws Exception {
        URL url = new URL(urlString);

        HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
        conexao.setRequestMethod("GET");
        conexao.setConnectTimeout(10000);
        conexao.setReadTimeout(10000);
        conexao.setRequestProperty("Accept", "application/json");

        String token = System.getenv("SAUDE_API_TOKEN");

        if (token != null && !token.trim().isEmpty()) {
            conexao.setRequestProperty("Authorization", "Bearer " + token.trim());
        }

        int statusCode = conexao.getResponseCode();

        if (statusCode == 401 || statusCode == 403) {
            System.out.println("[CNES_API] API exigiu autenticacao. Configure a variavel SAUDE_API_TOKEN.");
            return "";
        }

        if (statusCode < 200 || statusCode >= 300) {
            System.out.println("[CNES_API] Resposta HTTP inesperada: " + statusCode);
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

    private static List<LocalAtendimentoResultado> extrairLocaisDaResposta(
            String json,
            String cidadePadrao,
            String ufPadrao
    ) {
        List<LocalAtendimentoResultado> locais = new ArrayList<LocalAtendimentoResultado>();

        try {
            JsonElement raiz = JsonParser.parseString(json);

            List<JsonObject> objetos = new ArrayList<JsonObject>();
            coletarObjetos(raiz, objetos);

            for (JsonObject obj : objetos) {
                LocalAtendimentoResultado local =
                        converterObjetoParaLocal(obj, cidadePadrao, ufPadrao);

                if (local.nome != null && !local.nome.trim().isEmpty()) {
                    locais.add(local);
                }
            }

        } catch (Exception e) {
            System.out.println("[CNES_API] Erro ao interpretar JSON da API: " + e.getMessage());
        }

        return locais;
    }

    private static void coletarObjetos(JsonElement elemento, List<JsonObject> objetos) {
        if (elemento == null || elemento.isJsonNull()) {
            return;
        }

        if (elemento.isJsonObject()) {
            JsonObject obj = elemento.getAsJsonObject();

            if (pareceEstabelecimento(obj)) {
                objetos.add(obj);
            }

            for (String chave : obj.keySet()) {
                coletarObjetos(obj.get(chave), objetos);
            }
        }

        if (elemento.isJsonArray()) {
            JsonArray array = elemento.getAsJsonArray();

            for (JsonElement item : array) {
                coletarObjetos(item, objetos);
            }
        }
    }

    private static boolean pareceEstabelecimento(JsonObject obj) {
        String texto = ClassificadorLocalAtendimento.normalizar(obj.toString());

        return texto.contains("cnes")
                || texto.contains("estabelecimento")
                || texto.contains("unidade")
                || texto.contains("logradouro")
                || texto.contains("municipio")
                || texto.contains("fantasia");
    }

    private static LocalAtendimentoResultado converterObjetoParaLocal(
            JsonObject obj,
            String cidadePadrao,
            String ufPadrao
    ) {
        LocalAtendimentoResultado local = new LocalAtendimentoResultado();

        local.nome = buscarValorPorChaves(obj,
                "nome_fantasia",
                "no_fantasia",
                "nomeFantasia",
                "nome_estabelecimento",
                "estabelecimento",
                "razao_social",
                "no_razao_social",
                "nome");

        local.tipo = buscarValorPorChaves(obj,
                "tipo_unidade",
                "descricao_tipo_unidade",
                "ds_tipo_unidade",
                "tipoUnidade",
                "tipo",
                "descricao");

        String logradouro = buscarValorPorChaves(obj,
                "logradouro",
                "no_logradouro",
                "endereco",
                "endereco_estabelecimento");

        String numero = buscarValorPorChaves(obj,
                "numero",
                "nu_endereco",
                "numero_endereco");

        String bairro = buscarValorPorChaves(obj,
                "bairro",
                "no_bairro");

        local.endereco = montarEndereco(logradouro, numero, bairro);

        local.telefone = buscarValorPorChaves(obj,
                "telefone",
                "nu_telefone",
                "telefone_estabelecimento",
                "ddd_telefone");

        local.cidade = buscarValorPorChaves(obj,
                "municipio",
                "nome_municipio",
                "no_municipio",
                "cidade");

        local.uf = buscarValorPorChaves(obj,
                "uf",
                "sigla_uf");

        if (local.cidade == null || local.cidade.trim().isEmpty()) {
            local.cidade = cidadePadrao;
        }

        if (local.uf == null || local.uf.trim().isEmpty()) {
            local.uf = ufPadrao;
        }

        local.codigoCnes = buscarValorPorChaves(obj,
                "cnes",
                "codigo_cnes",
                "co_cnes",
                "codigoCnes");

        local.fonte = "CNES / Dados Abertos SUS";
        local.observacao = "Dado obtido por consulta ao CNES. Validar funcionamento, endereco e contato nos canais oficiais antes de qualquer uso real.";

        return local;
    }

    private static String buscarValorPorChaves(JsonObject obj, String... chaves) {
        for (String chave : chaves) {
            String valor = buscarValorRecursivo(obj, chave);

            if (valor != null && !valor.trim().isEmpty() && !valor.equalsIgnoreCase("null")) {
                return valor.trim();
            }
        }

        return "";
    }

    private static String buscarValorRecursivo(JsonElement elemento, String chaveBuscada) {
        if (elemento == null || elemento.isJsonNull()) {
            return "";
        }

        if (elemento.isJsonObject()) {
            JsonObject obj = elemento.getAsJsonObject();

            for (String chave : obj.keySet()) {
                JsonElement valor = obj.get(chave);

                if (chave.equalsIgnoreCase(chaveBuscada)) {
                    if (valor != null && !valor.isJsonNull() && valor.isJsonPrimitive()) {
                        return valor.getAsString();
                    }
                }

                String encontrado = buscarValorRecursivo(valor, chaveBuscada);

                if (encontrado != null && !encontrado.trim().isEmpty()) {
                    return encontrado;
                }
            }
        }

        if (elemento.isJsonArray()) {
            JsonArray array = elemento.getAsJsonArray();

            for (JsonElement item : array) {
                String encontrado = buscarValorRecursivo(item, chaveBuscada);

                if (encontrado != null && !encontrado.trim().isEmpty()) {
                    return encontrado;
                }
            }
        }

        return "";
    }

    private static String montarEndereco(String logradouro, String numero, String bairro) {
        StringBuilder endereco = new StringBuilder();

        if (logradouro != null && !logradouro.trim().isEmpty()) {
            endereco.append(logradouro.trim());
        }

        if (numero != null && !numero.trim().isEmpty()) {
            if (endereco.length() > 0) {
                endereco.append(", ");
            }

            endereco.append(numero.trim());
        }

        if (bairro != null && !bairro.trim().isEmpty()) {
            if (endereco.length() > 0) {
                endereco.append(" - ");
            }

            endereco.append(bairro.trim());
        }

        if (endereco.length() == 0) {
            return "Endereco nao informado";
        }

        return endereco.toString();
    }

    private static String obterCodigoUf(String uf) {
        String sigla = ClassificadorLocalAtendimento.normalizar(uf).toUpperCase();

        if (sigla.equals("DF")) return "53";
        if (sigla.equals("SP")) return "35";
        if (sigla.equals("RJ")) return "33";
        if (sigla.equals("MG")) return "31";
        if (sigla.equals("GO")) return "52";
        if (sigla.equals("BA")) return "29";
        if (sigla.equals("TO")) return "17";
        if (sigla.equals("SC")) return "42";
        if (sigla.equals("PR")) return "41";
        if (sigla.equals("RS")) return "43";
        if (sigla.equals("PE")) return "26";
        if (sigla.equals("CE")) return "23";
        if (sigla.equals("PA")) return "15";
        if (sigla.equals("AM")) return "13";
        if (sigla.equals("ES")) return "32";
        if (sigla.equals("MT")) return "51";
        if (sigla.equals("MS")) return "50";
        if (sigla.equals("MA")) return "21";
        if (sigla.equals("PB")) return "25";
        if (sigla.equals("RN")) return "24";
        if (sigla.equals("AL")) return "27";
        if (sigla.equals("SE")) return "28";
        if (sigla.equals("PI")) return "22";
        if (sigla.equals("RO")) return "11";
        if (sigla.equals("AC")) return "12";
        if (sigla.equals("RR")) return "14";
        if (sigla.equals("AP")) return "16";

        return "";
    }

    private static List<String> obterCodigosMunicipioPossiveis(String cidade, String uf) {
        String cidadeNormalizada = ClassificadorLocalAtendimento.normalizar(cidade);
        String ufNormalizada = ClassificadorLocalAtendimento.normalizar(uf).toUpperCase();

        if (cidadeNormalizada.equals("brasilia") && ufNormalizada.equals("DF")) {
            return Arrays.asList("5300108", "530010");
        }

        if (cidadeNormalizada.equals("sao paulo") && ufNormalizada.equals("SP")) {
            return Arrays.asList("3550308", "355030");
        }

        if (cidadeNormalizada.equals("rio de janeiro") && ufNormalizada.equals("RJ")) {
            return Arrays.asList("3304557", "330455");
        }

        if (cidadeNormalizada.equals("belo horizonte") && ufNormalizada.equals("MG")) {
            return Arrays.asList("3106200", "310620");
        }

        if (cidadeNormalizada.equals("goiania") && ufNormalizada.equals("GO")) {
            return Arrays.asList("5208707", "520870");
        }

        return new ArrayList<String>();
    }
}
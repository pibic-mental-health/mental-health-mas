package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClienteNvidia {

    private static final String API_URL =
            "https://integrate.api.nvidia.com/v1/chat/completions";

    private static final String MODEL =
            "meta/llama-3.1-8b-instruct";

    // Evita que o sistema fique travado indefinidamente se a API externa
    // estiver lenta ou indisponivel.
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    public static String gerarResposta(String prompt) {

        String apiKey = System.getenv("NVIDIA_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[NVIDIA] Variavel NVIDIA_API_KEY nao configurada.");
            return "Erro: NVIDIA_API_KEY nao configurada.";
        }

        HttpURLConnection conn = null;

        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty(
                    "Authorization",
                    "Bearer " + apiKey.trim()
            );

            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setDoOutput(true);

            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\""
                    + escaparJson(prompt)
                    + "\"}],"
                    + "\"temperature\":0.7,"
                    + "\"max_tokens\":300"
                    + "}";

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int status = conn.getResponseCode();

            InputStream stream = status >= 200 && status < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String respostaJson = lerStream(stream);

            if (status < 200 || status >= 300) {
                System.out.println("[NVIDIA] Erro HTTP " + status);

                if (!respostaJson.trim().isEmpty()) {
                    System.out.println(
                            "[NVIDIA] Resposta da API: "
                                    + limitarLog(respostaJson, 1000)
                    );
                }

                return "Erro: falha HTTP ao consultar NVIDIA NIM.";
            }

            String resposta = extrairResposta(respostaJson);

            if (resposta == null || resposta.trim().isEmpty()) {
                return "Erro: resposta vazia da NVIDIA NIM.";
            }

            return resposta;

        } catch (SocketTimeoutException e) {
            System.out.println(
                    "[NVIDIA] Timeout ao consultar a API. "
                            + "O fluxo deve utilizar o fallback local."
            );

            return "Erro: timeout ao consultar NVIDIA NIM.";

        } catch (IOException e) {
            System.out.println(
                    "[NVIDIA] Falha de comunicacao: "
                            + e.getClass().getSimpleName()
                            + " - "
                            + e.getMessage()
            );

            return "Erro: falha de comunicacao com NVIDIA NIM.";

        } catch (Exception e) {
            System.out.println(
                    "[NVIDIA] Falha inesperada: "
                            + e.getClass().getSimpleName()
                            + " - "
                            + e.getMessage()
            );

            return "Erro: falha inesperada ao consultar NVIDIA NIM.";

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String lerStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        StringBuilder resposta = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String linha;

            while ((linha = br.readLine()) != null) {
                resposta.append(linha);
            }
        }

        return resposta.toString();
    }

    private static String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String extrairResposta(String json) {
        try {
            String marcador = "\"content\":\"";
            int start = json.indexOf(marcador);

            if (start == -1) {
                System.out.println(
                        "[NVIDIA] Campo content nao encontrado na resposta."
                );
                return "";
            }

            start += marcador.length();

            StringBuilder conteudo = new StringBuilder();
            boolean escape = false;

            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);

                if (escape) {
                    if (c == 'n') {
                        conteudo.append('\n');
                    } else if (c == 'r') {
                        conteudo.append('\r');
                    } else if (c == 't') {
                        conteudo.append('\t');
                    } else {
                        conteudo.append(c);
                    }

                    escape = false;

                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        conteudo.append(c);
                    }
                }
            }

            return conteudo.toString();

        } catch (Exception e) {
            System.out.println(
                    "[NVIDIA] Erro ao interpretar resposta: "
                            + e.getMessage()
            );
            return "";
        }
    }

    private static String limitarLog(String texto, int limite) {
        if (texto == null) {
            return "";
        }

        if (texto.length() <= limite) {
            return texto;
        }

        return texto.substring(0, limite) + "...";
    }
}
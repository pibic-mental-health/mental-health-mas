package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClienteNvidia {

    private static final String API_KEY = "REMOVED_NVIDIA_API_KEY";
    private static final String API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL = "meta/llama-3.1-8b-instruct";

    public static String gerarResposta(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaparJson(prompt) + "\"}],"
                    + "\"temperature\":0.7,"
                    + "\"max_tokens\":300"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8
            ));

            StringBuilder respostaJson = new StringBuilder();
            String linha;

            while ((linha = br.readLine()) != null) {
                respostaJson.append(linha);
            }

            br.close();

            if (status < 200 || status >= 300) {
                System.out.println("[NVIDIA] Erro HTTP " + status);
                System.out.println("[NVIDIA] Resposta da API: " + respostaJson);
                return "Erro ao gerar resposta da IA.";
            }

            return extrairResposta(respostaJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao gerar resposta da IA.";
        }
    }

    private static String escaparJson(String texto) {
        if (texto == null) return "";

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
                return "Erro ao interpretar resposta da IA.";
            }

            start += marcador.length();

            StringBuilder conteudo = new StringBuilder();
            boolean escape = false;

            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);

                if (escape) {
                    if (c == 'n') conteudo.append('\n');
                    else if (c == 'r') conteudo.append('\r');
                    else if (c == 't') conteudo.append('\t');
                    else conteudo.append(c);

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
            return "Erro ao interpretar resposta da IA.";
        }
    }
}
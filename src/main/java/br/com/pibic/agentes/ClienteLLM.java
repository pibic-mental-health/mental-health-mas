package br.com.pibic.agentes;

public class ClienteLLM {

    private static final String PROVEDOR_PADRAO = "NVIDIA_NIM";

    public static String gerarResposta(String prompt) {
        String provedor = obterProvedor();

        System.out.println("[LLM] Provedor configurado: " + provedor);

        if ("NVIDIA_NIM".equals(provedor)) {
            return ClienteNvidia.gerarResposta(prompt);
        }

        if ("DEEPSEEK".equals(provedor)) {
            return "Erro: Cliente DeepSeek ainda nao implementado.";
        }

        if ("OLLAMA_LOCAL".equals(provedor)) {
            return "Erro: Cliente Ollama local ainda nao implementado.";
        }

        System.out.println(
                "[LLM] Provedor desconhecido: "
                        + provedor
                        + ". Usando provedor padrao: "
                        + PROVEDOR_PADRAO
        );

        return ClienteNvidia.gerarResposta(prompt);
    }

    public static String obterProvedor() {
        String provedor = System.getenv("LLM_PROVIDER");

        if (provedor == null || provedor.trim().isEmpty()) {
            return PROVEDOR_PADRAO;
        }

        return provedor.trim().toUpperCase();
    }
}
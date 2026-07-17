package br.com.pibic.agentes;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClassificadorLocalAtendimento {

    public static boolean classificar(LocalAtendimentoResultado local) {
        String texto = normalizar(
                valorSeguro(local.nome) + " "
                + valorSeguro(local.tipo) + " "
                + valorSeguro(local.categoria) + " "
                + valorSeguro(local.endereco)
        );

        if (ehFalsoPositivo(texto) && !possuiTermoForteSaudeMental(texto)) {
            local.prioridade = 0;
            local.categoria = "Nao relacionado a saude mental";
            local.categoriaRecomendacao = local.categoria;
            return false;
        }

        if (texto.contains("caps")
                || texto.contains("centro de atencao psicossocial")
                || texto.contains("centro atencao psicossocial")) {
            local.prioridade = 100;
            local.categoria = "CAPS / Atencao Psicossocial";
            local.categoriaRecomendacao = local.categoria;
            return true;
        }

        if (texto.contains("psicossocial")
                || texto.contains("saude mental")
                || texto.contains("atencao psicossocial")) {
            local.prioridade = 90;
            local.categoria = "Servico especializado em saude mental";
            local.categoriaRecomendacao = local.categoria;
            return true;
        }

        if (texto.contains("psychotherapist")
                || texto.contains("psicoterapia")
                || texto.contains("psicologia")
                || texto.contains("psicologo")
                || texto.contains("psicologa")
                || texto.contains("psiquiatria")
                || texto.contains("psiquiatra")
                || texto.contains("psiquiatrico")
                || texto.contains("psiquiatrica")) {
            local.prioridade = 80;
            local.categoria = "Psicologia / Psiquiatria / Psicoterapia";
            local.categoriaRecomendacao = local.categoria;
            return true;
        }

        if (texto.contains("ubs")
                || texto.contains("unidade basica")
                || texto.contains("posto de saude")
                || texto.contains("centro de saude")) {
            local.prioridade = 20;
            local.categoria = "Porta de entrada da rede publica";
            local.categoriaRecomendacao = local.categoria;
            return true;
        }

        local.prioridade = 0;
        local.categoria = "Nao classificado";
        local.categoriaRecomendacao = local.categoria;
        return false;
    }

    public static List<LocalAtendimentoResultado> deduplicarOrdenarLimitar(
            List<LocalAtendimentoResultado> locais,
            int limite
    ) {
        List<LocalAtendimentoResultado> unicos = new ArrayList<LocalAtendimentoResultado>();
        List<String> chaves = new ArrayList<String>();

        for (LocalAtendimentoResultado local : locais) {
            if (local == null || local.nome == null || local.nome.trim().isEmpty()) {
                continue;
            }

            String chave = montarChave(local);

            if (!chaves.contains(chave)) {
                chaves.add(chave);
                unicos.add(local);
            }
        }

        Collections.sort(unicos, new Comparator<LocalAtendimentoResultado>() {
            @Override
            public int compare(LocalAtendimentoResultado a, LocalAtendimentoResultado b) {
                return b.prioridade - a.prioridade;
            }
        });

        List<LocalAtendimentoResultado> limitados = new ArrayList<LocalAtendimentoResultado>();

        for (LocalAtendimentoResultado local : unicos) {
            if (limitados.size() < limite) {
                limitados.add(local);
            }
        }

        return limitados;
    }

    private static String montarChave(LocalAtendimentoResultado local) {
        if (local.codigoCnes != null && !local.codigoCnes.trim().isEmpty()) {
            return "cnes:" + local.codigoCnes.trim();
        }

        if (local.idExterno != null && !local.idExterno.trim().isEmpty()) {
            return "id:" + local.idExterno.trim();
        }

        return normalizar(valorSeguro(local.nome) + "|" + valorSeguro(local.endereco));
    }

    private static boolean possuiTermoForteSaudeMental(String texto) {
        return texto.contains("caps")
                || texto.contains("psicossocial")
                || texto.contains("saude mental")
                || texto.contains("psicologia")
                || texto.contains("psicologo")
                || texto.contains("psicologa")
                || texto.contains("psiquiatria")
                || texto.contains("psiquiatra")
                || texto.contains("psicoterapia")
                || texto.contains("psychotherapist")
                || texto.contains("centro de atencao psicossocial");
    }

    private static boolean ehFalsoPositivo(String texto) {
        return texto.contains("fisioterapia")
                || texto.contains("fisioterapeuta")
                || texto.contains("odontologia")
                || texto.contains("odontologico")
                || texto.contains("odontologica")
                || texto.contains("dentista")
                || texto.contains("fonoaudiologia")
                || texto.contains("fonoaudiologo")
                || texto.contains("nutricao")
                || texto.contains("nutricionista")
                || texto.contains("estetica")
                || texto.contains("pilates")
                || texto.contains("ortopedia")
                || texto.contains("cardiologia")
                || texto.contains("oftalmologia")
                || texto.contains("ginecologia")
                || texto.contains("pediatria")
                || texto.contains("veterinaria");
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        return textoNormalizado.toLowerCase().trim();
    }

    public static String valorSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "Nao informado";
        }

        return valor;
    }
}
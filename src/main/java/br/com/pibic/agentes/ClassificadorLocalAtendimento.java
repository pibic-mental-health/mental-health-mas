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

        if (ehOrgaoReguladorOuProfissional(texto)) {
            local.prioridade = 0;
            local.categoria = "Orgao regulador ou entidade profissional";
            local.categoriaRecomendacao = local.categoria;
            return false;
        }

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

        if (ehInstituicaoAcademicaRelacionada(texto)) {
            local.prioridade = 50;
            local.categoria = "Instituicao relacionada - validar se oferece atendimento";
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
                || texto.contains("psiquiatrica")
                || texto.contains("clinica de psicologia")
                || texto.contains("clinica psicologica")) {
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

            String chave = montarChaveDeduplicacao(local);

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

    private static String montarChaveDeduplicacao(LocalAtendimentoResultado local) {
        if (local.codigoCnes != null && !local.codigoCnes.trim().isEmpty()) {
            return "cnes:" + local.codigoCnes.trim();
        }

        String nomeNormalizado = limparNomeParaComparacao(local.nome);
        String enderecoNormalizado = limparEnderecoParaComparacao(local.endereco);

        if (nomeNormalizado.equals("caps") || nomeNormalizado.length() <= 5) {
            return nomeNormalizado + "|coord:" + arredondar(local.latitude) + "," + arredondar(local.longitude);
        }

        if (!enderecoNormalizado.isEmpty()
                && !enderecoNormalizado.contains("endereco nao informado")) {
            return nomeNormalizado + "|" + enderecoNormalizado;
        }

        return nomeNormalizado;
    }

    public static String obterTipoParaExibicao(LocalAtendimentoResultado local) {
        if (local == null) {
            return "Nao informado";
        }

        String categoria = valorSeguro(local.categoria);
        String tipo = valorSeguro(local.tipo);

        if (categoria.equalsIgnoreCase("CAPS / Atencao Psicossocial")) {
            return "CAPS / Atencao Psicossocial";
        }

        if (categoria.equalsIgnoreCase("Servico especializado em saude mental")) {
            return "Servico de saude mental";
        }

        if (categoria.equalsIgnoreCase("Psicologia / Psiquiatria / Psicoterapia")) {
            return "Psicologia / Psiquiatria / Psicoterapia";
        }

        if (categoria.equalsIgnoreCase("Instituicao relacionada - validar se oferece atendimento")) {
            return "Instituicao academica ou relacionada";
        }

        if (categoria.equalsIgnoreCase("Porta de entrada da rede publica")) {
            return "UBS / Centro de Saude / Atencao Primaria";
        }

        if (tipo == null || tipo.trim().isEmpty() || tipo.equalsIgnoreCase("Nao informado")) {
            return "Nao informado";
        }

        return limparTipoBruto(tipo);
    }

    private static boolean ehOrgaoReguladorOuProfissional(String texto) {
        return texto.contains("conselho federal de psicologia")
                || texto.contains("conselho regional de psicologia")
                || texto.contains("conselho de psicologia")
                || texto.contains("cfp")
                || texto.contains("crp ")
                || texto.contains(" crp")
                || texto.contains("sindicato dos psicologos")
                || texto.contains("sindicato de psicologia")
                || texto.contains("associacao brasileira de psicologia")
                || texto.contains("sociedade brasileira de psicologia");
    }

    private static boolean ehInstituicaoAcademicaRelacionada(String texto) {
        boolean possuiTermoAcademico = texto.contains("universidade")
                || texto.contains("faculdade")
                || texto.contains("unb")
                || texto.contains("instituto de psicologia")
                || texto.contains("departamento de psicologia")
                || texto.contains("curso de psicologia")
                || texto.contains("campus");

        boolean possuiTermoSaudeMental = texto.contains("psicologia")
                || texto.contains("psicologico")
                || texto.contains("psicologica")
                || texto.contains("psicoterapia")
                || texto.contains("psiquiatria")
                || texto.contains("saude mental");

        return possuiTermoAcademico && possuiTermoSaudeMental;
    }

    private static String limparTipoBruto(String tipo) {
        String texto = tipo;

        texto = texto.replace("healthcare=", "");
        texto = texto.replace("speciality=", "");
        texto = texto.replace("amenity=", "");
        texto = texto.replace("_", " ");

        if (texto.length() > 120) {
            texto = texto.substring(0, 120) + "...";
        }

        return texto;
    }

    private static String limparNomeParaComparacao(String nome) {
        String texto = normalizar(nome);

        texto = texto.replace("-", " ");
        texto = texto.replace("  ", " ");
        texto = texto.replace("centro de atencao psicossocial", "caps");
        texto = texto.replace("centro atencao psicossocial", "caps");

        return texto.trim();
    }

    private static String limparEnderecoParaComparacao(String endereco) {
        String texto = normalizar(endereco);

        texto = texto.replace(",", " ");
        texto = texto.replace("-", " ");
        texto = texto.replace("  ", " ");

        return texto.trim();
    }

    private static String arredondar(double valor) {
        if (valor == 0.0) {
            return "0";
        }

        return String.format(java.util.Locale.US, "%.4f", valor);
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
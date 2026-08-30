package br.com.pibic.agentes;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClassificadorLocalAtendimento {

    public static boolean classificar(
            LocalAtendimentoResultado local) {

        String texto =
                normalizar(
                        valorSeguro(local.nome)
                        + " "
                        + valorSeguro(local.tipo)
                        + " "
                        + valorSeguro(local.categoria)
                        + " "
                        + valorSeguro(local.endereco)
                );

        if (ehOrgaoReguladorOuProfissional(texto)) {

            local.prioridade = 0;
            local.categoria =
                    "Orgao regulador ou entidade profissional";

            local.categoriaRecomendacao =
                    local.categoria;

            return false;
        }

        if (ehFalsoPositivo(texto)
                && !possuiTermoForteSaudeMental(texto)) {

            local.prioridade = 0;
            local.categoria =
                    "Nao relacionado a saude mental";

            local.categoriaRecomendacao =
                    local.categoria;

            return false;
        }

        if (texto.contains("caps")
                || texto.contains(
                "centro de atencao psicossocial")
                || texto.contains(
                "centro atencao psicossocial")) {

            local.prioridade = 100;
            local.categoria =
                    "CAPS / Atencao Psicossocial";

            local.categoriaRecomendacao =
                    local.categoria;

            return true;
        }

        if (texto.contains("psicossocial")
                || texto.contains("saude mental")
                || texto.contains(
                "atencao psicossocial")) {

            local.prioridade = 90;
            local.categoria =
                    "Servico especializado em saude mental";

            local.categoriaRecomendacao =
                    local.categoria;

            return true;
        }

        if (ehInstituicaoAcademicaRelacionada(texto)) {

            local.prioridade = 50;
            local.categoria =
                    "Instituicao relacionada - validar se oferece atendimento";

            local.categoriaRecomendacao =
                    local.categoria;

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
            local.categoria =
                    "Psicologia / Psiquiatria / Psicoterapia";

            local.categoriaRecomendacao =
                    local.categoria;

            return true;
        }

        if (texto.contains("ubs")
                || texto.contains("unidade basica")
                || texto.contains("posto de saude")
                || texto.contains("centro de saude")) {

            local.prioridade = 20;
            local.categoria =
                    "Porta de entrada da rede publica";

            local.categoriaRecomendacao =
                    local.categoria;

            return true;
        }

        local.prioridade = 0;
        local.categoria =
                "Nao classificado";

        local.categoriaRecomendacao =
                local.categoria;

        return false;
    }

    public static List<LocalAtendimentoResultado> deduplicarOrdenarLimitar(
            List<LocalAtendimentoResultado> locais,
            int limite) {

        List<LocalAtendimentoResultado> unicos =
                new ArrayList<LocalAtendimentoResultado>();

        for (LocalAtendimentoResultado local : locais) {

            if (local == null
                    || local.nome == null
                    || local.nome.trim().isEmpty()) {

                continue;
            }

            LocalAtendimentoResultado existente =
                    encontrarDuplicado(
                            unicos,
                            local
                    );

            if (existente == null) {

                unicos.add(local);

            } else {

                mesclar(
                        existente,
                        local
                );
            }
        }

        Collections.sort(
                unicos,
                new Comparator<LocalAtendimentoResultado>() {

                    @Override
                    public int compare(
                            LocalAtendimentoResultado a,
                            LocalAtendimentoResultado b) {

                        int prioridade =
                                Integer.compare(
                                        b.prioridade,
                                        a.prioridade
                                );

                        if (prioridade != 0) {
                            return prioridade;
                        }

                        int fonte =
                                Integer.compare(
                                        prioridadeFonte(b),
                                        prioridadeFonte(a)
                                );

                        if (fonte != 0) {
                            return fonte;
                        }

                        boolean aTemDistancia =
                                a.distanciaKm >= 0.0;

                        boolean bTemDistancia =
                                b.distanciaKm >= 0.0;

                        if (aTemDistancia
                                && bTemDistancia) {

                            return Double.compare(
                                    a.distanciaKm,
                                    b.distanciaKm
                            );
                        }

                        if (aTemDistancia) {
                            return -1;
                        }

                        if (bTemDistancia) {
                            return 1;
                        }

                        return normalizar(a.nome)
                                .compareTo(
                                        normalizar(b.nome)
                                );
                    }
                }
        );

        int quantidade =
                Math.min(
                        Math.max(limite, 0),
                        unicos.size()
                );

        return new ArrayList<LocalAtendimentoResultado>(
                unicos.subList(
                        0,
                        quantidade
                )
        );
    }

    private static LocalAtendimentoResultado encontrarDuplicado(
            List<LocalAtendimentoResultado> existentes,
            LocalAtendimentoResultado candidato) {

        for (LocalAtendimentoResultado atual : existentes) {

            if (mesmoLocal(
                    atual,
                    candidato)) {

                return atual;
            }
        }

        return null;
    }

    private static boolean mesmoLocal(
            LocalAtendimentoResultado a,
            LocalAtendimentoResultado b) {

        String cnesA =
                valorCru(a.codigoCnes);

        String cnesB =
                valorCru(b.codigoCnes);

        if (!cnesA.isEmpty()
                && !cnesB.isEmpty()
                && cnesA.equals(cnesB)) {

            return true;
        }

        String nomeA =
                limparNomeParaComparacao(
                        a.nome
                );

        String nomeB =
                limparNomeParaComparacao(
                        b.nome
                );

        if (nomeA.isEmpty()
                || nomeB.isEmpty()) {

            return false;
        }

        boolean nomeIgual =
                nomeA.equals(nomeB);

        boolean ambosCaps =
                nomeA.startsWith("caps")
                && nomeB.startsWith("caps");

        if (!nomeIgual
                && !ambosCaps) {

            return false;
        }

        String cidadeA =
                normalizar(
                        valorCru(a.cidade)
                );

        String cidadeB =
                normalizar(
                        valorCru(b.cidade)
                );

        if (!cidadeA.isEmpty()
                && !cidadeB.isEmpty()
                && !cidadeA.equals(cidadeB)) {

            return false;
        }

        if (coordenadasValidas(a)
                && coordenadasValidas(b)) {

            return distanciaKm(
                    a.latitude,
                    a.longitude,
                    b.latitude,
                    b.longitude
            ) <= 0.8;
        }

        String enderecoA =
                limparEnderecoParaComparacao(
                        a.endereco
                );

        String enderecoB =
                limparEnderecoParaComparacao(
                        b.endereco
                );

        if (!enderecoA.isEmpty()
                && !enderecoB.isEmpty()) {

            return enderecoA.equals(enderecoB)
                    || enderecoA.contains(enderecoB)
                    || enderecoB.contains(enderecoA);
        }

        return nomeIgual;
    }

    private static void mesclar(
            LocalAtendimentoResultado destino,
            LocalAtendimentoResultado origem) {

        destino.prioridade =
                Math.max(
                        destino.prioridade,
                        origem.prioridade
                );

        if (valorCru(destino.categoria).isEmpty()) {
            destino.categoria =
                    origem.categoria;
        }

        if (valorCru(destino.categoriaRecomendacao).isEmpty()) {
            destino.categoriaRecomendacao =
                    origem.categoriaRecomendacao;
        }

        destino.tipo =
                escolherMaisInformativo(
                        destino.tipo,
                        origem.tipo
                );

        destino.descricao =
                escolherMaisInformativo(
                        destino.descricao,
                        origem.descricao
                );

        destino.endereco =
                escolherMaisInformativo(
                        destino.endereco,
                        origem.endereco
                );

        destino.telefone =
                escolherMaisInformativo(
                        destino.telefone,
                        origem.telefone
                );

        destino.codigoCnes =
                escolherMaisInformativo(
                        destino.codigoCnes,
                        origem.codigoCnes
                );

        destino.link =
                escolherMaisInformativo(
                        destino.link,
                        origem.link
                );

        destino.idExterno =
                escolherMaisInformativo(
                        destino.idExterno,
                        origem.idExterno
                );

        if (!coordenadasValidas(destino)
                && coordenadasValidas(origem)) {

            destino.latitude =
                    origem.latitude;

            destino.longitude =
                    origem.longitude;
        }

        if (destino.distanciaKm < 0.0
                && origem.distanciaKm >= 0.0) {

            destino.distanciaKm =
                    origem.distanciaKm;
        }

        destino.fonte =
                mesclarFontes(
                        destino.fonte,
                        origem.fonte
                );

        destino.observacao =
                escolherMaisInformativo(
                        destino.observacao,
                        origem.observacao
                );
    }

    private static String mesclarFontes(
            String a,
            String b) {

        String fonteA =
                valorCru(a);

        String fonteB =
                valorCru(b);

        if (fonteA.isEmpty()) {
            return fonteB;
        }

        if (fonteB.isEmpty()
                || normalizar(fonteA)
                .contains(
                        normalizar(fonteB))) {

            return fonteA;
        }

        return fonteA
                + " + "
                + fonteB;
    }

    private static String escolherMaisInformativo(
            String atual,
            String novo) {

        String a =
                valorCru(atual);

        String b =
                valorCru(novo);

        if (a.isEmpty()) {
            return b;
        }

        if (b.isEmpty()) {
            return a;
        }

        if (a.toLowerCase()
                .contains("nao informado")) {

            return b;
        }

        return b.length() > a.length()
                ? b
                : a;
    }

    private static int prioridadeFonte(
            LocalAtendimentoResultado local) {

        String fonte =
                normalizar(
                        valorCru(local.fonte)
                );

        if (fonte.contains("cnes")) {
            return 3;
        }

        if (fonte.contains("openstreetmap")
                || fonte.contains("overpass")) {

            return 2;
        }

        if (fonte.contains("google")) {
            return 1;
        }

        return 0;
    }

    public static String obterTipoParaExibicao(
            LocalAtendimentoResultado local) {

        if (local == null) {
            return "Nao informado";
        }

        String categoria =
                valorSeguro(
                        local.categoria
                );

        String tipo =
                valorSeguro(
                        local.tipo
                );

        if (categoria.equalsIgnoreCase(
                "CAPS / Atencao Psicossocial")) {

            return "CAPS / Atencao Psicossocial";
        }

        if (categoria.equalsIgnoreCase(
                "Servico especializado em saude mental")) {

            return "Servico de saude mental";
        }

        if (categoria.equalsIgnoreCase(
                "Psicologia / Psiquiatria / Psicoterapia")) {

            return "Psicologia / Psiquiatria / Psicoterapia";
        }

        if (categoria.equalsIgnoreCase(
                "Instituicao relacionada - validar se oferece atendimento")) {

            return "Instituicao academica ou relacionada";
        }

        if (categoria.equalsIgnoreCase(
                "Porta de entrada da rede publica")) {

            return "UBS / Centro de Saude / Atencao Primaria";
        }

        if (tipo.equalsIgnoreCase(
                "Nao informado")) {

            return "Nao informado";
        }

        return limparTipoBruto(tipo);
    }

    public static double calcularDistanciaKm(
            double origemLat,
            double origemLon,
            double destinoLat,
            double destinoLon) {

        return distanciaKm(
                origemLat,
                origemLon,
                destinoLat,
                destinoLon
        );
    }

    private static double distanciaKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double raioTerraKm =
                6371.0088;

        double dLat =
                Math.toRadians(
                        lat2 - lat1
                );

        double dLon =
                Math.toRadians(
                        lon2 - lon1
                );

        double a =
                Math.sin(dLat / 2.0)
                        * Math.sin(dLat / 2.0)
                        + Math.cos(
                                Math.toRadians(lat1))
                        * Math.cos(
                                Math.toRadians(lat2))
                        * Math.sin(dLon / 2.0)
                        * Math.sin(dLon / 2.0);

        double c =
                2.0
                * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1.0 - a)
                );

        return raioTerraKm * c;
    }

    private static boolean coordenadasValidas(
            LocalAtendimentoResultado local) {

        return local != null
                && local.latitude >= -90.0
                && local.latitude <= 90.0
                && local.longitude >= -180.0
                && local.longitude <= 180.0
                && !(local.latitude == 0.0
                && local.longitude == 0.0);
    }

    private static boolean ehOrgaoReguladorOuProfissional(
            String texto) {

        return texto.contains(
                "conselho federal de psicologia")
                || texto.contains(
                "conselho regional de psicologia")
                || texto.contains(
                "conselho de psicologia")
                || texto.contains("cfp")
                || texto.contains("crp ")
                || texto.contains(" crp")
                || texto.contains(
                "sindicato dos psicologos")
                || texto.contains(
                "sindicato de psicologia")
                || texto.contains(
                "associacao brasileira de psicologia")
                || texto.contains(
                "sociedade brasileira de psicologia");
    }

    private static boolean ehInstituicaoAcademicaRelacionada(
            String texto) {

        boolean termoAcademico =
                texto.contains("universidade")
                || texto.contains("faculdade")
                || texto.contains("unb")
                || texto.contains(
                "instituto de psicologia")
                || texto.contains(
                "departamento de psicologia")
                || texto.contains(
                "curso de psicologia")
                || texto.contains("campus");

        boolean termoSaudeMental =
                texto.contains("psicologia")
                || texto.contains("psicologico")
                || texto.contains("psicologica")
                || texto.contains("psicoterapia")
                || texto.contains("psiquiatria")
                || texto.contains("saude mental");

        return termoAcademico
                && termoSaudeMental;
    }

    private static boolean possuiTermoForteSaudeMental(
            String texto) {

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
                || texto.contains(
                "centro de atencao psicossocial");
    }

    private static boolean ehFalsoPositivo(
            String texto) {

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

    private static String limparTipoBruto(
            String tipo) {

        String texto =
                tipo;

        texto =
                texto.replace(
                        "healthcare=",
                        ""
                );

        texto =
                texto.replace(
                        "speciality=",
                        ""
                );

        texto =
                texto.replace(
                        "amenity=",
                        ""
                );

        texto =
                texto.replace(
                        "_",
                        " "
                );

        return texto.length() > 120
                ? texto.substring(0, 120)
                + "..."
                : texto;
    }

    private static String limparNomeParaComparacao(
            String nome) {

        String texto =
                normalizar(nome);

        texto =
                texto.replace(
                        "-",
                        " "
                );

        texto =
                texto.replace(
                        "centro de atencao psicossocial",
                        "caps"
                );

        texto =
                texto.replace(
                        "centro atencao psicossocial",
                        "caps"
                );

        while (texto.contains("  ")) {
            texto =
                    texto.replace(
                            "  ",
                            " "
                    );
        }

        return texto.trim();
    }

    private static String limparEnderecoParaComparacao(
            String endereco) {

        String texto =
                normalizar(endereco);

        texto =
                texto.replace(
                        ",",
                        " "
                );

        texto =
                texto.replace(
                        "-",
                        " "
                );

        while (texto.contains("  ")) {
            texto =
                    texto.replace(
                            "  ",
                            " "
                    );
        }

        if (texto.contains(
                "endereco nao informado")) {

            return "";
        }

        return texto.trim();
    }

    public static String normalizar(
            String texto) {

        if (texto == null) {
            return "";
        }

        String textoNormalizado =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        textoNormalizado =
                textoNormalizado.replaceAll(
                        "[\\p{InCombiningDiacriticalMarks}]",
                        ""
                );

        return textoNormalizado
                .toLowerCase()
                .trim();
    }

    public static String valorSeguro(
            String valor) {

        if (valor == null
                || valor.trim().isEmpty()) {

            return "Nao informado";
        }

        return valor;
    }

    private static String valorCru(
            String valor) {

        return valor == null
                ? ""
                : valor.trim();
    }
}

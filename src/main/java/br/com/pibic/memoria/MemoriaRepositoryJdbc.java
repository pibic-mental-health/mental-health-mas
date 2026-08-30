package br.com.pibic.memoria;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementacao JDBC do repositorio de memoria.
 *
 * Persistimos no PostgreSQL:
 * - usuarioId e nome;
 * - todas as aplicacoes DASS-21;
 * - as 21 respostas de cada aplicacao;
 * - historico de mensagens do usuario e do sistema.
 *
 * Estados temporarios de fluxo continuam em memoria durante a execucao.
 */
public class MemoriaRepositoryJdbc
        implements MemoriaRepository {

    private final String url;
    private final String usuario;
    private final String senha;

    private final ConcurrentMap<String, MemoriaUsuario> cache =
            new ConcurrentHashMap<String, MemoriaUsuario>();

    public MemoriaRepositoryJdbc(
            String url,
            String usuario,
            String senha) {

        this.url = exigirValor(url, "PIBIC_DB_URL");
        this.usuario = exigirValor(usuario, "PIBIC_DB_USER");
        this.senha = exigirValor(senha, "PIBIC_DB_PASSWORD");

        testarConexao();
    }

    @Override
    public MemoriaUsuario buscarOuCriar(
            String usuarioId) {

        String id = normalizarUsuarioId(usuarioId);

        MemoriaUsuario existente =
                cache.get(id);

        if (existente != null) {
            return existente;
        }

        MemoriaUsuario carregada =
                carregarDoBanco(id);

        MemoriaUsuario anterior =
                cache.putIfAbsent(
                        id,
                        carregada
                );

        return anterior != null
                ? anterior
                : carregada;
    }

    @Override
    public void salvar(
            MemoriaUsuario memoria) {

        if (memoria == null) {
            return;
        }

        cache.put(
                memoria.getUsuarioId(),
                memoria
        );

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);

            try {
                salvarUsuario(
                        conexao,
                        memoria
                );

                salvarTriagensNovas(
                        conexao,
                        memoria
                );

                salvarHistoricoNovo(
                        conexao,
                        memoria
                );

                conexao.commit();

            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Falha ao persistir memoria no PostgreSQL.",
                    e
            );
        }
    }

    @Override
    public void remover(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {
            return;
        }

        String id = usuarioId.trim();

        String sql =
                "DELETE FROM pibic.usuarios "
                + "WHERE usuario_id = ?";

        try (Connection conexao = abrirConexao();
             PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.executeUpdate();

            cache.remove(id);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Falha ao remover usuario do PostgreSQL.",
                    e
            );
        }
    }

    private MemoriaUsuario carregarDoBanco(
            String usuarioId) {

        MemoriaUsuario memoria =
                new MemoriaUsuario(usuarioId);

        try (Connection conexao = abrirConexao()) {
            carregarUsuario(
                    conexao,
                    memoria
            );

            carregarTriagens(
                    conexao,
                    memoria
            );

            carregarHistorico(
                    conexao,
                    memoria
            );

            return memoria;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Falha ao carregar memoria do PostgreSQL para usuarioId="
                    + usuarioId,
                    e
            );
        }
    }

    private void carregarUsuario(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        String sql =
                "SELECT nome "
                + "FROM pibic.usuarios "
                + "WHERE usuario_id = ?";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    memoria.getUsuarioId()
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {
                    String nome =
                            rs.getString("nome");

                    if (nome != null) {
                        memoria.setNome(nome);
                    }
                }
            }
        }
    }

    private void carregarTriagens(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        String sql =
                "SELECT "
                + "id, "
                + "instrumento, "
                + "versao_instrumento, "
                + "versao_algoritmo, "
                + "realizado_em, "
                + "subtotal_depressao, "
                + "subtotal_ansiedade, "
                + "subtotal_estresse, "
                + "score_depressao, "
                + "score_ansiedade, "
                + "score_estresse "
                + "FROM pibic.triagens_dass21 "
                + "WHERE usuario_id = ? "
                + "ORDER BY realizado_em, id";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    memoria.getUsuarioId()
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {
                    long triagemId =
                            rs.getLong("id");

                    int[] respostas =
                            carregarRespostas(
                                    conexao,
                                    triagemId
                            );

                    OffsetDateTime realizadoEm =
                            rs.getObject(
                                    "realizado_em",
                                    OffsetDateTime.class
                            );

                    RegistroTriagemDass21 registro =
                            new RegistroTriagemDass21(
                                    rs.getString("instrumento"),
                                    rs.getString("versao_instrumento"),
                                    rs.getString("versao_algoritmo"),
                                    respostas,
                                    rs.getInt("subtotal_depressao"),
                                    rs.getInt("subtotal_ansiedade"),
                                    rs.getInt("subtotal_estresse"),
                                    rs.getInt("score_depressao"),
                                    rs.getInt("score_ansiedade"),
                                    rs.getInt("score_estresse"),
                                    realizadoEm
                                            .atZoneSameInstant(ZoneId.systemDefault())
                                            .toLocalDateTime()
                            );

                    memoria.adicionarTriagemDass21Persistida(
                            registro
                    );
                }
            }
        }
    }

    private void carregarHistorico(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        String sql =
                "SELECT "
                + "autor, "
                + "texto, "
                + "registrado_em "
                + "FROM pibic.chat_mensagens "
                + "WHERE usuario_id = ? "
                + "ORDER BY registrado_em, id";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    memoria.getUsuarioId()
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    OffsetDateTime registradoEm =
                            rs.getObject(
                                    "registrado_em",
                                    OffsetDateTime.class
                            );

                    LocalDateTime dataHora =
                            registradoEm
                                    .atZoneSameInstant(
                                            ZoneId.systemDefault()
                                    )
                                    .toLocalDateTime();

                    memoria.adicionarHistoricoPersistido(
                            new RegistroHistorico(
                                    rs.getString("autor"),
                                    rs.getString("texto"),
                                    dataHora
                            )
                    );
                }
            }
        }
    }


    private int[] carregarRespostas(
            Connection conexao,
            long triagemId)
            throws SQLException {

        String sql =
                "SELECT numero_item, resposta "
                + "FROM pibic.triagem_dass21_respostas "
                + "WHERE triagem_id = ? "
                + "ORDER BY numero_item";

        int[] respostas =
                new int[21];

        boolean[] encontrados =
                new boolean[21];

        int quantidade = 0;

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setLong(1, triagemId);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {
                    int numeroItem =
                            rs.getInt("numero_item");

                    int resposta =
                            rs.getInt("resposta");

                    if (numeroItem < 1
                            || numeroItem > 21
                            || resposta < 0
                            || resposta > 3) {

                        throw new SQLException(
                                "Resposta DASS-21 inconsistente no banco."
                        );
                    }

                    respostas[numeroItem - 1] =
                            resposta;

                    if (!encontrados[numeroItem - 1]) {
                        encontrados[numeroItem - 1] =
                                true;

                        quantidade++;
                    }
                }
            }
        }

        if (quantidade != 21) {
            throw new SQLException(
                    "Triagem DASS-21 id="
                    + triagemId
                    + " nao possui exatamente 21 respostas."
            );
        }

        return respostas;
    }

    private void salvarUsuario(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        String sql =
                "INSERT INTO pibic.usuarios "
                + "(usuario_id, nome) "
                + "VALUES (?, ?) "
                + "ON CONFLICT (usuario_id) "
                + "DO UPDATE SET "
                + "nome = EXCLUDED.nome, "
                + "atualizado_em = CURRENT_TIMESTAMP";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    memoria.getUsuarioId()
            );

            ps.setString(
                    2,
                    memoria.getNome()
            );

            ps.executeUpdate();
        }
    }

    private void salvarTriagensNovas(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        int quantidadeBanco =
                contarTriagens(
                        conexao,
                        memoria.getUsuarioId()
                );

        List<RegistroTriagemDass21> triagens =
                memoria.getTriagensDass21();

        if (quantidadeBanco > triagens.size()) {
            throw new SQLException(
                    "O banco possui mais triagens que a memoria atual para usuarioId="
                    + memoria.getUsuarioId()
            );
        }

        for (int i = quantidadeBanco;
             i < triagens.size();
             i++) {

            inserirTriagem(
                    conexao,
                    memoria.getUsuarioId(),
                    triagens.get(i)
            );
        }
    }

    private void salvarHistoricoNovo(
            Connection conexao,
            MemoriaUsuario memoria)
            throws SQLException {

        int quantidadeBanco =
                contarMensagensHistorico(
                        conexao,
                        memoria.getUsuarioId()
                );

        List<RegistroHistorico> historico =
                memoria.getHistorico();

        if (quantidadeBanco
                > historico.size()) {

            throw new SQLException(
                    "O banco possui mais mensagens que a memoria atual para usuarioId="
                    + memoria.getUsuarioId()
            );
        }

        for (int i = quantidadeBanco;
             i < historico.size();
             i++) {

            inserirMensagemHistorico(
                    conexao,
                    memoria.getUsuarioId(),
                    historico.get(i)
            );
        }
    }

    private int contarMensagensHistorico(
            Connection conexao,
            String usuarioId)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) "
                + "FROM pibic.chat_mensagens "
                + "WHERE usuario_id = ?";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    usuarioId
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                rs.next();

                return rs.getInt(1);
            }
        }
    }

    private void inserirMensagemHistorico(
            Connection conexao,
            String usuarioId,
            RegistroHistorico registro)
            throws SQLException {

        String sql =
                "INSERT INTO pibic.chat_mensagens ("
                + "usuario_id, "
                + "autor, "
                + "texto, "
                + "registrado_em"
                + ") VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(
                    1,
                    usuarioId
            );

            ps.setString(
                    2,
                    registro.getAutor()
            );

            ps.setString(
                    3,
                    registro.getTexto()
            );

            OffsetDateTime registradoEm =
                    registro.getRegistradoEm()
                            .atZone(
                                    ZoneId.systemDefault()
                            )
                            .toOffsetDateTime();

            ps.setObject(
                    4,
                    registradoEm
            );

            ps.executeUpdate();
        }
    }


    private int contarTriagens(
            Connection conexao,
            String usuarioId)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) "
                + "FROM pibic.triagens_dass21 "
                + "WHERE usuario_id = ?";

        try (PreparedStatement ps =
                     conexao.prepareStatement(sql)) {

            ps.setString(1, usuarioId);

            try (ResultSet rs =
                         ps.executeQuery()) {

                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void inserirTriagem(
            Connection conexao,
            String usuarioId,
            RegistroTriagemDass21 registro)
            throws SQLException {

        int[] respostas =
                registro.getRespostas();

        if (respostas.length != 21) {
            throw new SQLException(
                    "Tentativa de persistir DASS-21 sem 21 respostas."
            );
        }

        String sqlTriagem =
                "INSERT INTO pibic.triagens_dass21 ("
                + "usuario_id, "
                + "instrumento, "
                + "versao_instrumento, "
                + "versao_algoritmo, "
                + "realizado_em, "
                + "subtotal_depressao, "
                + "subtotal_ansiedade, "
                + "subtotal_estresse, "
                + "score_depressao, "
                + "score_ansiedade, "
                + "score_estresse"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "RETURNING id";

        long triagemId;

        try (PreparedStatement ps =
                     conexao.prepareStatement(
                             sqlTriagem
                     )) {

            ps.setString(1, usuarioId);
            ps.setString(
                    2,
                    registro.getInstrumento()
            );
            ps.setString(
                    3,
                    registro.getVersaoInstrumento()
            );
            ps.setString(
                    4,
                    registro.getVersaoAlgoritmo()
            );

            OffsetDateTime realizadoEm =
                    registro.getRealizadoEm()
                    .atZone(
                            ZoneId.systemDefault()
                    )
                    .toOffsetDateTime();

            ps.setObject(
                    5,
                    realizadoEm
            );

            ps.setInt(
                    6,
                    registro.getSubtotalDepressao()
            );
            ps.setInt(
                    7,
                    registro.getSubtotalAnsiedade()
            );
            ps.setInt(
                    8,
                    registro.getSubtotalEstresse()
            );
            ps.setInt(
                    9,
                    registro.getScoreDepressao()
            );
            ps.setInt(
                    10,
                    registro.getScoreAnsiedade()
            );
            ps.setInt(
                    11,
                    registro.getScoreEstresse()
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (!rs.next()) {
                    throw new SQLException(
                            "PostgreSQL nao retornou id da triagem inserida."
                    );
                }

                triagemId =
                        rs.getLong(1);
            }
        }

        String sqlResposta =
                "INSERT INTO pibic.triagem_dass21_respostas "
                + "(triagem_id, numero_item, resposta) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement ps =
                     conexao.prepareStatement(
                             sqlResposta
                     )) {

            for (int i = 0;
                 i < respostas.length;
                 i++) {

                int resposta =
                        respostas[i];

                if (resposta < 0
                        || resposta > 3) {

                    throw new SQLException(
                            "Resposta DASS-21 fora do intervalo 0-3."
                    );
                }

                ps.setLong(
                        1,
                        triagemId
                );

                ps.setInt(
                        2,
                        i + 1
                );

                ps.setInt(
                        3,
                        resposta
                );

                ps.addBatch();
            }

            int[] resultado =
                    ps.executeBatch();

            if (resultado.length != 21) {
                throw new SQLException(
                        "Nem todas as 21 respostas DASS-21 foram persistidas."
                );
            }
        }
    }

    private Connection abrirConexao()
            throws SQLException {

        return DriverManager.getConnection(
                url,
                usuario,
                senha
        );
    }

    private void testarConexao() {
        String sql = "SELECT 1";

        try (Connection conexao = abrirConexao();
             PreparedStatement ps =
                     conexao.prepareStatement(sql);
             ResultSet rs =
                     ps.executeQuery()) {

            if (!rs.next()
                    || rs.getInt(1) != 1) {

                throw new SQLException(
                        "Teste de conexao PostgreSQL retornou valor inesperado."
                );
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Nao foi possivel conectar ao PostgreSQL.",
                    e
            );
        }
    }

    private String exigirValor(
            String valor,
            String nomeVariavel) {

        if (valor == null
                || valor.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    nomeVariavel
                    + " nao configurada."
            );
        }

        return valor.trim();
    }

    private String normalizarUsuarioId(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "usuarioId nao informado."
            );
        }

        return usuarioId.trim();
    }
}
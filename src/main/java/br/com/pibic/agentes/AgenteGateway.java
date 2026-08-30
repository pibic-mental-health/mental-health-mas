package br.com.pibic.agentes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import br.com.pibic.api.ChatHistoricoResponse;
import br.com.pibic.api.ChatRequest;
import br.com.pibic.api.ChatResponse;
import br.com.pibic.api.Dass21FormularioResponse;
import br.com.pibic.api.LocalAtendimentoResponse;
import br.com.pibic.api.TriagemRequest;
import br.com.pibic.api.TriagemResponse;
import br.com.pibic.api.TriagemHistoricoResponse;
import br.com.pibic.triagem.Dass21Instrumento;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteGateway extends Agent {

    private static final int PORTA_PADRAO = 8080;
    private static final long TIMEOUT_CHAT_SEGUNDOS = 75L;
    private static final long TIMEOUT_TRIAGEM_SEGUNDOS = 20L;
    private static final long TIMEOUT_HISTORICO_SEGUNDOS = 10L;
    private static final long TIMEOUT_LOCAIS_SEGUNDOS = 90L;

    private final Gson gson = new Gson();

    private final BlockingQueue<SolicitacaoPendente> fila =
            new LinkedBlockingQueue<SolicitacaoPendente>();

    private final ConcurrentMap<String, CompletableFuture<String>> pendentes =
            new ConcurrentHashMap<String, CompletableFuture<String>>();

    private HttpServer servidor;

    @Override
    protected void setup() {
        System.out.println(
                "Agente Gateway iniciado: "
                + getLocalName()
        );

        iniciarServidorHttp();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                SolicitacaoPendente solicitacao =
                        fila.poll();

                if (solicitacao == null) {
                    block(50);
                    return;
                }

                pendentes.put(
                        solicitacao.conversationId,
                        solicitacao.future
                );

                ACLMessage mensagem =
                        new ACLMessage(
                                ACLMessage.REQUEST
                        );

                mensagem.addReceiver(
                        new AID(
                                solicitacao.destinatario,
                                AID.ISLOCALNAME
                        )
                );

                mensagem.setConversationId(
                        solicitacao.conversationId
                );

                mensagem.setReplyWith(
                        solicitacao.conversationId
                );

                mensagem.setContent(
                        solicitacao.conteudo
                );

                System.out.println(
                        "[GATEWAY] Enviando requisicao. "
                        + "conversationId="
                        + solicitacao.conversationId
                        + " destinatario="
                        + solicitacao.destinatario
                );

                send(mensagem);
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage resposta = receive();

                if (resposta == null) {
                    block();
                    return;
                }

                String conversationId =
                        resposta.getConversationId();

                if (conversationId == null
                        || conversationId.trim().isEmpty()) {

                    System.out.println(
                            "[GATEWAY] Resposta sem conversationId. Ignorando."
                    );

                    return;
                }

                CompletableFuture<String> future =
                        pendentes.remove(
                                conversationId
                        );

                if (future != null) {
                    future.complete(
                            resposta.getContent()
                    );

                    System.out.println(
                            "[GATEWAY] Resposta concluida. conversationId="
                            + conversationId
                    );
                } else {
                    System.out.println(
                            "[GATEWAY] Requisicao pendente nao encontrada para "
                            + conversationId
                    );
                }
            }
        });
    }

    private void iniciarServidorHttp() {
        try {
            int porta = obterPorta();

            servidor = HttpServer.create(
                    new InetSocketAddress(
                            porta
                    ),
                    0
            );

            servidor.createContext(
                    "/api/health",
                    new HealthHandler()
            );

            servidor.createContext(
                    "/api/chat",
                    new ChatHandler()
            );

            servidor.createContext(
                    "/api/chat/historico",
                    new ChatHistoricoHandler()
            );

            servidor.createContext(
                    "/api/locais",
                    new LocaisHandler()
            );

            servidor.createContext(
                    "/api/triagem",
                    new TriagemHandler()
            );

            servidor.createContext(
                    "/api/triagem/dass21",
                    new Dass21FormularioHandler()
            );

            servidor.createContext(
                    "/api/triagem/historico",
                    new TriagemHistoricoHandler()
            );

            servidor.setExecutor(
                    Executors.newCachedThreadPool()
            );

            servidor.start();

            System.out.println(
                    "[GATEWAY] API HTTP iniciada em http://localhost:"
                    + porta
            );

            System.out.println(
                    "[GATEWAY] Health: GET http://localhost:"
                    + porta
                    + "/api/health"
            );

            System.out.println(
                    "[GATEWAY] Chat: POST http://localhost:"
                    + porta
                    + "/api/chat"
            );

            System.out.println(
                    "[GATEWAY] Historico Chat: GET http://localhost:"
                    + porta
                    + "/api/chat/historico?usuarioId=USR001"
            );

            System.out.println(
                    "[GATEWAY] Locais: GET http://localhost:"
                    + porta
                    + "/api/locais?cidade=Brasilia&uf=DF"
                    + "&latitude=-15.78&longitude=-47.88&raioMetros=8000"
            );

            System.out.println(
                    "[GATEWAY] DASS-21: GET http://localhost:"
                    + porta
                    + "/api/triagem/dass21"
            );

            System.out.println(
                    "[GATEWAY] Triagem: POST http://localhost:"
                    + porta
                    + "/api/triagem"
            );

            System.out.println(
                    "[GATEWAY] Historico DASS-21: GET http://localhost:"
                    + porta
                    + "/api/triagem/historico?usuarioId=USR001"
            );

        } catch (Exception e) {
            System.out.println(
                    "[GATEWAY] Nao foi possivel iniciar a API: "
                    + e.getMessage()
            );

            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        if (servidor != null) {
            servidor.stop(0);
        }

        System.out.println(
                "Agente Gateway finalizado."
        );
    }

    private int obterPorta() {
        String portaEnv =
                System.getenv(
                        "PIBIC_API_PORT"
                );

        if (portaEnv == null
                || portaEnv.trim().isEmpty()) {

            return PORTA_PADRAO;
        }

        try {
            return Integer.parseInt(
                    portaEnv.trim()
            );
        } catch (NumberFormatException e) {
            return PORTA_PADRAO;
        }
    }

    private String sanitizarCampo(
            String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String sanitizarPerfil(
            String perfil) {

        if (perfil == null) {
            return "GERAL";
        }

        String p =
                perfil.trim().toUpperCase();

        if ("ANSIEDADE".equals(p)
                || "DEPRESSAO".equals(p)
                || "ESTRESSE".equals(p)
                || "MISTO".equals(p)
                || "GERAL".equals(p)) {

            return p;
        }

        return "GERAL";
    }

    private String normalizarUsuarioId(
            String usuarioId) {

        if (usuarioId == null
                || usuarioId.trim().isEmpty()) {

            return "USR-"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 8);
        }

        return sanitizarCampo(
                usuarioId
        );
    }

    private void adicionarCors(
            HttpExchange exchange) {

        Headers headers =
                exchange.getResponseHeaders();

        headers.set(
                "Access-Control-Allow-Origin",
                "*"
        );

        headers.set(
                "Access-Control-Allow-Methods",
                "GET, POST, OPTIONS"
        );

        headers.set(
                "Access-Control-Allow-Headers",
                "Content-Type"
        );

        headers.set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );
    }

    private void responder(
            HttpExchange exchange,
            int status,
            String corpo) throws IOException {

        adicionarCors(exchange);

        byte[] bytes =
                corpo.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (OutputStream os =
                exchange.getResponseBody()) {

            os.write(bytes);
        }
    }

    private String lerCorpo(
            HttpExchange exchange)
            throws IOException {

        StringBuilder sb =
                new StringBuilder();

        try (BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(
                                exchange.getRequestBody(),
                                StandardCharsets.UTF_8
                        ))) {

            String linha;

            while ((linha = br.readLine())
                    != null) {

                sb.append(linha);
            }
        }

        return sb.toString();
    }

    private String aguardar(
            HttpExchange exchange,
            String conversationId,
            CompletableFuture<String> future,
            long timeoutSegundos,
            String mensagemTimeout)
            throws IOException {

        try {
            String resposta =
                    future.get(
                            timeoutSegundos,
                            TimeUnit.SECONDS
                    );

            responder(
                    exchange,
                    200,
                    resposta
            );

            return resposta;

        } catch (TimeoutException e) {
            pendentes.remove(
                    conversationId
            );

            future.cancel(true);

            responder(
                    exchange,
                    504,
                    gson.toJson(
                            TriagemResponse.erro(
                                    mensagemTimeout
                            )
                    )
            );

        } catch (Exception e) {
            pendentes.remove(
                    conversationId
            );

            responder(
                    exchange,
                    500,
                    gson.toJson(
                            TriagemResponse.erro(
                                    "Falha ao processar a solicitacao: "
                                    + e.getMessage()
                            )
                    )
            );
        }

        return null;
    }

    private String obterParametroQuery(
            HttpExchange exchange,
            String nomeParametro) {

        String query =
                exchange.getRequestURI()
                .getRawQuery();

        if (query == null
                || query.trim().isEmpty()) {

            return "";
        }

        String[] parametros =
                query.split("&");

        for (String parametro : parametros) {
            String[] kv =
                    parametro.split("=", 2);

            if (kv.length == 2
                    && kv[0].equals(nomeParametro)) {

                try {
                    return URLDecoder.decode(
                            kv[1],
                            "UTF-8"
                    );
                } catch (Exception e) {
                    return "";
                }
            }
        }

        return "";
    }

    private Double obterDoubleQueryOpcional(
            HttpExchange exchange,
            String nomeParametro)
            throws IllegalArgumentException {

        String valor =
                sanitizarCampo(
                        obterParametroQuery(
                                exchange,
                                nomeParametro
                        )
                );

        if (valor.isEmpty()) {
            return null;
        }

        try {
            return Double.valueOf(
                    valor.replace(
                            ",",
                            "."
                    )
            );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "O parametro "
                    + nomeParametro
                    + " deve ser numerico."
            );
        }
    }

    private int obterInteiroQuery(
            HttpExchange exchange,
            String nomeParametro,
            int valorPadrao)
            throws IllegalArgumentException {

        String valor =
                sanitizarCampo(
                        obterParametroQuery(
                                exchange,
                                nomeParametro
                        )
                );

        if (valor.isEmpty()) {
            return valorPadrao;
        }

        try {
            return Integer.parseInt(
                    valor
            );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "O parametro "
                    + nomeParametro
                    + " deve ser um numero inteiro."
            );
        }
    }

    private boolean coordenadasValidas(
            double latitude,
            double longitude) {

        return latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0
                && !(latitude == 0.0
                && longitude == 0.0);
    }


    private class HealthHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);
                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        "{\"sucesso\":false,\"erro\":\"Metodo nao permitido\"}"
                );

                return;
            }

            responder(
                    exchange,
                    200,
                    "{\"sucesso\":true,\"servico\":\"PIBIC Gateway\",\"status\":\"UP\"}"
            );
        }
    }

    private class Dass21FormularioHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);
                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                TriagemResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            responder(
                    exchange,
                    200,
                    gson.toJson(
                            Dass21FormularioResponse.criar()
                    )
            );
        }
    }

    private class TriagemHistoricoHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);
                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                TriagemHistoricoResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            String usuarioId =
                    sanitizarCampo(
                            obterParametroQuery(
                                    exchange,
                                    "usuarioId"
                            )
                    );

            if (usuarioId.isEmpty()) {
                responder(
                        exchange,
                        400,
                        gson.toJson(
                                TriagemHistoricoResponse.erro(
                                        "O parametro usuarioId e obrigatorio."
                                )
                        )
                );

                return;
            }

            String conteudo =
                    "tipo=consulta_triagens_dass21;"
                    + "usuarioId="
                    + usuarioId;

            String conversationId =
                    "API_TRIAGEM_HIST_"
                    + UUID.randomUUID()
                    .toString();

            CompletableFuture<String> future =
                    new CompletableFuture<String>();

            fila.offer(
                    new SolicitacaoPendente(
                            conversationId,
                            "agenteMemoria",
                            conteudo,
                            future
                    )
            );

            aguardar(
                    exchange,
                    conversationId,
                    future,
                    TIMEOUT_HISTORICO_SEGUNDOS,
                    "O agente de memoria nao respondeu dentro do tempo esperado."
            );
        }
    }

    private class ChatHistoricoHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                ChatHistoricoResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            String usuarioId =
                    sanitizarCampo(
                            obterParametroQuery(
                                    exchange,
                                    "usuarioId"
                            )
                    );

            if (usuarioId.isEmpty()) {

                responder(
                        exchange,
                        400,
                        gson.toJson(
                                ChatHistoricoResponse.erro(
                                        "O parametro usuarioId e obrigatorio."
                                )
                        )
                );

                return;
            }

            String conteudo =
                    "tipo=consulta_historico_chat;"
                    + "usuarioId="
                    + usuarioId;

            String conversationId =
                    "API_CHAT_HIST_"
                    + UUID.randomUUID()
                    .toString();

            CompletableFuture<String> future =
                    new CompletableFuture<String>();

            fila.offer(
                    new SolicitacaoPendente(
                            conversationId,
                            "agenteMemoria",
                            conteudo,
                            future
                    )
            );

            aguardar(
                    exchange,
                    conversationId,
                    future,
                    TIMEOUT_HISTORICO_SEGUNDOS,
                    "O agente de memoria nao respondeu dentro do tempo esperado."
            );
        }
    }


    private class LocaisHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);

                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                LocalAtendimentoResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            try {

                String cidade =
                        sanitizarCampo(
                                obterParametroQuery(
                                        exchange,
                                        "cidade"
                                )
                        );

                String uf =
                        sanitizarCampo(
                                obterParametroQuery(
                                        exchange,
                                        "uf"
                                )
                        )
                        .toUpperCase();

                Double latitude =
                        obterDoubleQueryOpcional(
                                exchange,
                                "latitude"
                        );

                Double longitude =
                        obterDoubleQueryOpcional(
                                exchange,
                                "longitude"
                        );

                int raioMetros =
                        obterInteiroQuery(
                                exchange,
                                "raioMetros",
                                8000
                        );

                boolean informouLatitude =
                        latitude != null;

                boolean informouLongitude =
                        longitude != null;

                if (informouLatitude
                        != informouLongitude) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    LocalAtendimentoResponse.erro(
                                            "Latitude e longitude devem ser informadas juntas."
                                    )
                            )
                    );

                    return;
                }

                boolean possuiCoordenadas =
                        informouLatitude
                        && informouLongitude;

                if (possuiCoordenadas
                        && !coordenadasValidas(
                                latitude.doubleValue(),
                                longitude.doubleValue()
                        )) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    LocalAtendimentoResponse.erro(
                                            "Latitude ou longitude invalida."
                                    )
                            )
                    );

                    return;
                }

                if (!possuiCoordenadas
                        && (cidade.isEmpty()
                        || uf.isEmpty())) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    LocalAtendimentoResponse.erro(
                                            "Informe latitude/longitude ou cidade/uf."
                                    )
                            )
                    );

                    return;
                }

                if (!uf.isEmpty()
                        && uf.length() != 2) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    LocalAtendimentoResponse.erro(
                                            "O parametro uf deve possuir duas letras."
                                    )
                            )
                    );

                    return;
                }

                if (raioMetros < 1000
                        || raioMetros > 30000) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    LocalAtendimentoResponse.erro(
                                            "O raioMetros deve estar entre 1000 e 30000."
                                    )
                            )
                    );

                    return;
                }

                StringBuilder conteudo =
                        new StringBuilder();

                conteudo.append(
                        "origem=API;"
                );

                conteudo.append(
                        "tipo=consulta_locais_atendimento;"
                );

                conteudo.append(
                        "formato=json;"
                );

                conteudo.append(
                        "cidade="
                )
                .append(cidade)
                .append(";");

                conteudo.append(
                        "uf="
                )
                .append(uf)
                .append(";");

                if (possuiCoordenadas) {

                    conteudo.append(
                            "latitude="
                    )
                    .append(
                            latitude.doubleValue()
                    )
                    .append(";");

                    conteudo.append(
                            "longitude="
                    )
                    .append(
                            longitude.doubleValue()
                    )
                    .append(";");
                }

                conteudo.append(
                        "raioMetros="
                )
                .append(
                        raioMetros
                );

                String conversationId =
                        "API_LOCAIS_"
                        + UUID.randomUUID()
                        .toString();

                CompletableFuture<String> future =
                        new CompletableFuture<String>();

                fila.offer(
                        new SolicitacaoPendente(
                                conversationId,
                                "agenteLocalAtendimento",
                                conteudo.toString(),
                                future
                        )
                );

                aguardar(
                        exchange,
                        conversationId,
                        future,
                        TIMEOUT_LOCAIS_SEGUNDOS,
                        "O agente de locais de atendimento nao respondeu dentro do tempo esperado."
                );

            } catch (IllegalArgumentException e) {

                responder(
                        exchange,
                        400,
                        gson.toJson(
                                LocalAtendimentoResponse.erro(
                                        e.getMessage()
                                )
                        )
                );

            } catch (Exception e) {

                responder(
                        exchange,
                        500,
                        gson.toJson(
                                LocalAtendimentoResponse.erro(
                                        "Falha ao buscar locais de atendimento: "
                                        + e.getMessage()
                                )
                        )
                );
            }
        }
    }


    private class ChatHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);
                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                ChatResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            try {
                ChatRequest request =
                        gson.fromJson(
                                lerCorpo(exchange),
                                ChatRequest.class
                        );

                if (request == null) {
                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    ChatResponse.erro(
                                            "JSON da requisicao e invalido."
                                    )
                            )
                    );

                    return;
                }

                request.setUsuarioId(
                        normalizarUsuarioId(
                                request.getUsuarioId()
                        )
                );

                if (request.getPerfil() == null
                        || request.getPerfil().trim().isEmpty()) {

                    request.setPerfil("GERAL");
                }

                if (request.getMensagem() == null
                        || request.getMensagem().trim().isEmpty()) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    ChatResponse.erro(
                                            "O campo mensagem e obrigatorio."
                                    )
                            )
                    );

                    return;
                }

                if (request.getMensagem().length()
                        > 4000) {

                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    ChatResponse.erro(
                                            "A mensagem ultrapassa o limite de 4000 caracteres."
                                    )
                            )
                    );

                    return;
                }

                String mensagemBase64 =
                        Base64.getEncoder()
                        .encodeToString(
                                request.getMensagem()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

                String conteudo =
                        "origem=API;"
                        + "tipo=chat;"
                        + "usuarioId="
                        + request.getUsuarioId()
                        + ";perfil="
                        + sanitizarPerfil(
                                request.getPerfil()
                        )
                        + ";mensagemBase64="
                        + mensagemBase64;

                String conversationId =
                        "API_CHAT_"
                        + UUID.randomUUID()
                        .toString();

                CompletableFuture<String> future =
                        new CompletableFuture<String>();

                fila.offer(
                        new SolicitacaoPendente(
                                conversationId,
                                "agenteConversacional",
                                conteudo,
                                future
                        )
                );

                aguardar(
                        exchange,
                        conversationId,
                        future,
                        TIMEOUT_CHAT_SEGUNDOS,
                        "O sistema multiagente nao respondeu dentro do tempo esperado."
                );

            } catch (Exception e) {
                responder(
                        exchange,
                        400,
                        gson.toJson(
                                ChatResponse.erro(
                                        "Nao foi possivel interpretar a requisicao: "
                                        + e.getMessage()
                                )
                        )
                );
            }
        }
    }

    private class TriagemHandler
            implements HttpHandler {

        @Override
        public void handle(
                HttpExchange exchange)
                throws IOException {

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                adicionarCors(exchange);
                exchange.sendResponseHeaders(
                        204,
                        -1
                );

                exchange.close();
                return;
            }

            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                responder(
                        exchange,
                        405,
                        gson.toJson(
                                TriagemResponse.erro(
                                        "Metodo nao permitido."
                                )
                        )
                );

                return;
            }

            try {
                TriagemRequest request =
                        gson.fromJson(
                                lerCorpo(exchange),
                                TriagemRequest.class
                        );

                if (request == null) {
                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    TriagemResponse.erro(
                                            "JSON da requisicao e invalido."
                                    )
                            )
                    );

                    return;
                }

                request.setUsuarioId(
                        normalizarUsuarioId(
                                request.getUsuarioId()
                        )
                );

                Dass21Instrumento.validarRespostas(
                        request.getRespostas()
                );

                String nome =
                        request.getNome() == null
                        ? ""
                        : request.getNome().trim();

                if (nome.length() > 200) {
                    responder(
                            exchange,
                            400,
                            gson.toJson(
                                    TriagemResponse.erro(
                                            "O nome ultrapassa o limite de 200 caracteres."
                                    )
                            )
                    );

                    return;
                }

                String nomeBase64 =
                        Base64.getEncoder()
                        .encodeToString(
                                nome.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder respostas =
                        new StringBuilder();

                int[] valores =
                        request.getRespostas();

                for (int i = 0;
                        i < valores.length;
                        i++) {

                    if (i > 0) {
                        respostas.append(",");
                    }

                    respostas.append(
                            valores[i]
                    );
                }

                String conteudo =
                        "origem=API;"
                        + "tipo=triagem_dass21;"
                        + "usuarioId="
                        + request.getUsuarioId()
                        + ";nomeBase64="
                        + nomeBase64
                        + ";respostas="
                        + respostas.toString();

                String conversationId =
                        "API_TRIAGEM_"
                        + UUID.randomUUID()
                        .toString();

                CompletableFuture<String> future =
                        new CompletableFuture<String>();

                fila.offer(
                        new SolicitacaoPendente(
                                conversationId,
                                "agenteTriagem",
                                conteudo,
                                future
                        )
                );

                aguardar(
                        exchange,
                        conversationId,
                        future,
                        TIMEOUT_TRIAGEM_SEGUNDOS,
                        "O agente de triagem nao respondeu dentro do tempo esperado."
                );

            } catch (IllegalArgumentException e) {
                responder(
                        exchange,
                        400,
                        gson.toJson(
                                TriagemResponse.erro(
                                        e.getMessage()
                                )
                        )
                );

            } catch (Exception e) {
                responder(
                        exchange,
                        400,
                        gson.toJson(
                                TriagemResponse.erro(
                                        "Nao foi possivel interpretar a triagem: "
                                        + e.getMessage()
                                )
                        )
                );
            }
        }
    }

    private static class SolicitacaoPendente {

        private final String conversationId;
        private final String destinatario;
        private final String conteudo;
        private final CompletableFuture<String> future;

        private SolicitacaoPendente(
                String conversationId,
                String destinatario,
                String conteudo,
                CompletableFuture<String> future) {

            this.conversationId =
                    conversationId;

            this.destinatario =
                    destinatario;

            this.conteudo =
                    conteudo;

            this.future =
                    future;
        }
    }
}
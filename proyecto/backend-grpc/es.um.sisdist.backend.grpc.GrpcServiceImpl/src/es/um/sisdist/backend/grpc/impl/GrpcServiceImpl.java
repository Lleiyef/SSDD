package es.um.sisdist.backend.grpc.impl;

import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.auth.JwtUtil;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.grpc.*;
import io.grpc.stub.StreamObserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase {

    private static final long PROMPT_TIMEOUT_SECONDS = 300;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final Logger logger;
    private final IUserDAO userDAO;
    private final ConcurrentHashMap<String, CompletableFuture<String>> promptMap = new ConcurrentHashMap<>();

    GrpcServiceImpl(Logger logger) {
        super();
        this.logger = logger;
        this.userDAO = new DAOFactoryImpl().createSQLUserDAO();
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        logger.info("Recibido PING, value=" + request.getV());
        responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        logger.info("Intento de Login: " + request.getEmail());
        Optional<User> userOpt = userDAO.getUserByEmail(request.getEmail());
        boolean success = userOpt.isPresent()
                && UserUtils.md5pass(request.getPassword()).equals(userOpt.get().getPassword_hash());

        LoginResponse.Builder builder = LoginResponse.newBuilder().setSuccess(success);
        if (success) {
            User u = userOpt.get();
            builder.setToken(JwtUtil.generateToken(u.getId(), u.getEmail()))
                   .setUser(UserMessage.newBuilder()
                           .setId(u.getId()).setEmail(u.getEmail())
                           .setName(u.getName()).setVisits(u.getVisits()).build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendPrompt(PromptRequest request, StreamObserver<PromptToken> responseObserver) {
        String prompt = request.getPrompt();
        logger.info("gRPC recibido sendPrompt: " + prompt);

        if (prompt == null || prompt.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("El prompt no puede estar vacío"));
            return;
        }

        try {
            // Serializar el prompt manualmente para evitar dep. de Jackson en compile-time
            String jsonBody = "{\"prompt\": " + jsonStringValue(prompt) + "}";

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://ssdd-llamachat:5020/prompt"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> postResp = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());

            if (postResp.statusCode() != 202) {
                logger.warning("llamachat devolvió " + postResp.statusCode() + ", body: " + postResp.body());
                responseObserver.onError(new RuntimeException("llamachat no aceptó el prompt: " + postResp.statusCode()));
                return;
            }

            String location = postResp.headers().firstValue("Location")
                    .orElseThrow(() -> new RuntimeException("llamachat no devolvió Location header"));
            String llamachatToken = location.substring(location.lastIndexOf('/') + 1);

            String grpcToken = UUID.randomUUID().toString();
            CompletableFuture<String> future = new CompletableFuture<String>()
                    .orTimeout(PROMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            promptMap.put(grpcToken, future);

            CompletableFuture.runAsync(() -> {
                while (!future.isDone()) {
                    try {
                        Thread.sleep(500);
                        // Nuevo cliente por petición para evitar problemas de reutilización
                        // de conexión cuando llamachat devuelve 102 (cierra el socket).
                        HttpClient pollClient = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build();
                        HttpRequest pollReq = HttpRequest.newBuilder()
                                .uri(URI.create("http://ssdd-llamachat:5020/response/" + llamachatToken))
                                .GET().build();
                        HttpResponse<String> pollResp = pollClient.send(pollReq, HttpResponse.BodyHandlers.ofString());

                        if (pollResp.statusCode() == 200) {
                            String answer = extractJsonField(pollResp.body(), "answer");
                            future.complete(answer != null ? answer : pollResp.body());
                            logger.info("gRPC: respuesta lista para grpcToken=" + grpcToken);
                            return;
                        }
                        // Cualquier otro código (102 PROCESSING, etc.) → seguir esperando
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        future.completeExceptionally(e);
                        return;
                    } catch (Exception e) {
                        // Errores transitorios de conexión (comunes con 102) → reintentar
                        if (!future.isDone()) {
                            logger.warning("gRPC: error temporal en polling (reintentando): " + e.getMessage());
                        }
                    }
                }
            });

            responseObserver.onNext(PromptToken.newBuilder().setToken(grpcToken).build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.severe("gRPC: error en sendPrompt: " + e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getPromptResponse(PromptToken request, StreamObserver<PromptResponse> responseObserver) {
        String grpcToken = request.getToken();
        CompletableFuture<String> future = promptMap.get(grpcToken);

        if (future == null) {
            responseObserver.onNext(PromptResponse.newBuilder().setStatus(PromptStatus.PROCESSING).build());
            responseObserver.onCompleted();
            return;
        }

        if (future.isDone()) {
            try {
                String answer = future.get();
                promptMap.remove(grpcToken);
                responseObserver.onNext(PromptResponse.newBuilder()
                        .setResponse(answer).setStatus(PromptStatus.READY).build());
            } catch (Exception e) {
                promptMap.remove(grpcToken);
                logger.severe("Error recuperando respuesta para grpcToken=" + grpcToken + ": " + e);
                String cause = (e.getCause() instanceof TimeoutException) ? "Timeout esperando respuesta de llamachat"
                        : e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                responseObserver.onNext(PromptResponse.newBuilder()
                        .setResponse("[Error: " + cause + "]")
                        .setStatus(PromptStatus.READY).build());
            }
        } else {
            responseObserver.onNext(PromptResponse.newBuilder().setStatus(PromptStatus.PROCESSING).build());
        }
        responseObserver.onCompleted();
    }

    /** Extrae el valor de un campo JSON string con soporte de escapes Unicode. */
    private static String extractJsonField(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + key.length() + 2);
        int valStart = json.indexOf('"', colonIdx + 1) + 1;
        StringBuilder sb = new StringBuilder();
        int pos = valStart;
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\' && pos + 1 < json.length()) {
                char next = json.charAt(pos + 1);
                if (next == '"') { sb.append('"'); pos += 2; }
                else if (next == '\\') { sb.append('\\'); pos += 2; }
                else if (next == '/') { sb.append('/'); pos += 2; }
                else if (next == 'n') { sb.append('\n'); pos += 2; }
                else if (next == 't') { sb.append('\t'); pos += 2; }
                else if (next == 'r') { sb.append('\r'); pos += 2; }
                else if (next == 'b') { sb.append('\b'); pos += 2; }
                else if (next == 'f') { sb.append('\f'); pos += 2; }
                else if (next == 'u' && pos + 5 < json.length()) {
                    try {
                        sb.append((char) Integer.parseInt(json.substring(pos + 2, pos + 6), 16));
                        pos += 6;
                    } catch (NumberFormatException ex) {
                        sb.append('\\').append(next);
                        pos += 2;
                    }
                } else { sb.append('\\').append(next); pos += 2; }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                pos++;
            }
        }
        return sb.toString();
    }

    /** Serializa un String Java como valor JSON entre comillas con escapes correctos. */
    private static String jsonStringValue(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        return sb.append('"').toString();
    }
}

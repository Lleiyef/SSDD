package es.um.sisdist.backend.Service;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Cliente de prueba E2E que ejercita el flujo completo vía REST con JWT.
 *
 * Uso: java -cp ... TestClient [base-url]
 * Ejemplo: java -cp ... TestClient http://localhost:8081
 */
public class TestClient {

    private static final String TEST_EMAIL    = "e2euser@um.es";
    private static final String TEST_NAME     = "E2E User";
    private static final String TEST_PASSWORD = "e2epass";
    private static final String TEST_PROMPT   = "Di hola en una sola palabra";

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "http://localhost:8081";
        Client client = ClientBuilder.newClient();
        WebTarget root = client.target(UriBuilder.fromUri(base).build());

        System.out.println("=== TestClient E2E ===");
        System.out.println("Base URL: " + base);

        // 1. Signup
        System.out.println("\n[1] Signup " + TEST_EMAIL);
        Response signupResp = root.path("Service/jaxrs/signup")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Map.of("email", TEST_EMAIL, "name", TEST_NAME, "password", TEST_PASSWORD)));
        int sc = signupResp.getStatus();
        System.out.println("  Status: " + sc + (sc == 201 ? " (creado)" : sc == 409 ? " (ya existe)" : ""));
        signupResp.close();

        // 2. Login → JWT
        System.out.println("\n[2] Login");
        Response loginResp = root.path("Service/jaxrs/login")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD)));
        assert loginResp.getStatus() == 200 : "Login falló: " + loginResp.getStatus();
        @SuppressWarnings("unchecked")
        Map<String, String> loginBody = loginResp.readEntity(Map.class);
        String token = loginBody.get("token");
        System.out.println("  JWT obtenido: " + token.substring(0, 20) + "...");

        // Decodificar sub (userId) del payload JWT
        String payload = token.split("\\.")[1];
        int pad = 4 - payload.length() % 4;
        if (pad != 4) for (int i = 0; i < pad; i++) payload += "=";
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(payload));
        String userId = decoded.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
        System.out.println("  User ID: " + userId);

        // 3. Crear conversación
        String dname = "tc-" + System.currentTimeMillis();
        System.out.println("\n[3] Crear diálogo: " + dname);
        Response createResp = root.path("Service/jaxrs/u/" + userId + "/dialogue")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .post(Entity.json(Map.of("name", dname)));
        System.out.println("  Status: " + createResp.getStatus());
        createResp.close();

        // 4. Obtener diálogo (next_token)
        System.out.println("\n[4] Obtener diálogo y nextToken");
        @SuppressWarnings("unchecked")
        Map<String, Object> dialogue = root.path("Service/jaxrs/u/" + userId + "/dialogue/" + dname)
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .get(Map.class);
        String nextToken = (String) dialogue.get("nextToken");
        System.out.println("  Status diálogo: " + dialogue.get("status"));
        System.out.println("  nextToken: " + nextToken);

        // 5. Enviar prompt
        System.out.println("\n[5] Enviar prompt: \"" + TEST_PROMPT + "\"");
        Response promptResp = root.path("Service/jaxrs/u/" + userId + "/dialogue/" + dname + "/next/" + nextToken)
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .post(Entity.json(Map.of("prompt", TEST_PROMPT, "timestamp", System.currentTimeMillis())));
        System.out.println("  Status: " + promptResp.getStatus());
        promptResp.close();

        // 6. Polling hasta READY (máx 120s)
        System.out.println("\n[6] Polling respuesta...");
        String answer = null;
        for (int i = 0; i < 120; i++) {
            Thread.sleep(1000);
            @SuppressWarnings("unchecked")
            Map<String, Object> poll = root.path("Service/jaxrs/u/" + userId + "/dialogue/" + dname)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .get(Map.class);
            String status = (String) poll.get("status");
            if ("READY".equals(status)) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> messages =
                    (java.util.List<Map<String, Object>>) poll.get("messages");
                if (messages != null && !messages.isEmpty()) {
                    answer = (String) messages.get(messages.size() - 1).get("answer");
                }
                break;
            }
            if (i % 10 == 9) System.out.println("  ... esperando (" + (i + 1) + "s)");
        }
        System.out.println("  Respuesta LlamaChat: " + answer);
        assert answer != null && !answer.isBlank() : "No se recibió respuesta";

        // 7. Listar conversaciones
        System.out.println("\n[7] Listar conversaciones");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> list =
            root.path("Service/jaxrs/u/" + userId + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .get(java.util.List.class);
        System.out.println("  Total conversaciones: " + list.size());
        list.forEach(d2 -> System.out.println("    - " + d2.get("name") + " [" + d2.get("status") + "]"));

        // 8. Eliminar la conversación de prueba
        System.out.println("\n[8] Eliminar diálogo " + dname);
        Response delResp = root.path("Service/jaxrs/u/" + userId + "/dialogue/" + dname)
            .request()
            .header("Authorization", "Bearer " + token)
            .delete();
        System.out.println("  Status: " + delResp.getStatus());
        delResp.close();

        System.out.println("\n=== TestClient completado OK ===");
        client.close();
    }
}

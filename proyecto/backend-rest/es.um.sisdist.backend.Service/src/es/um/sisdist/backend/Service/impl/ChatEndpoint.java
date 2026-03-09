package es.um.sisdist.backend.Service.impl;

import java.util.logging.Logger;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/chat")
public class ChatEndpoint {
    private static final Logger logger = Logger.getLogger(ChatEndpoint.class.getName());

    // Cliente gRPC
    private GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
    private ManagedChannel channel;

    public ChatEndpoint() {
        logger.info("REST: Iniciando conexión con backend-grpc para el Chat...");
        this.channel = ManagedChannelBuilder.forAddress("backend-grpc", 50051)
                .usePlaintext()
                .build();
        this.blockingStub = GrpcServiceGrpc.newBlockingStub(channel);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendChat(ChatMessage message) {
        // Usamos el getter ahora
        logger.info("REST: Mensaje de chat recibido del Frontend: " + message.getPrompt());

        // 1. Preparar la petición gRPC
        PromptRequest request = PromptRequest.newBuilder()
                .setPrompt(message.getPrompt())
                .build();

        // 2. Llamar al Backend gRPC
        PromptResponse response;
        try {
            response = blockingStub.sendPrompt(request);
        } catch (Exception e) {
            logger.severe("REST: Error al contactar con gRPC: " + e.getMessage());
            return Response.serverError().entity("{\"error\": \"Error interno de comunicación\"}").build();
        }

        // 3. Procesar y devolver la respuesta al Frontend
        logger.info("REST: Respuesta de IA recibida, enviando al Frontend.");

        // Limpiamos la respuesta de comillas y saltos de línea para que no rompa el
        // JSON
        String textoLimpio = response.getResponse().replace("\"", "\\\"").replace("\n", "\\n");
        String jsonResponse = "{\"response\": \"" + textoLimpio + "\"}";

        return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();
    }
}
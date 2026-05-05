package es.um.sisdist.backend.Service.impl;

import java.util.logging.Logger;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.PromptStatus;
import es.um.sisdist.backend.grpc.PromptToken;
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

    private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
    private final ManagedChannel channel;

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
        logger.info("REST: Mensaje de chat recibido del Frontend: " + message.getPrompt());

        PromptRequest request = PromptRequest.newBuilder()
                .setPrompt(message.getPrompt())
                .build();

        try {
            PromptToken grpcToken = blockingStub.sendPrompt(request);

            PromptResponse response;
            do {
                Thread.sleep(500);
                response = blockingStub.getPromptResponse(grpcToken);
            } while (response.getStatus() == PromptStatus.PROCESSING);

            logger.info("REST: Respuesta de IA recibida, enviando al Frontend.");
            String textoLimpio = response.getResponse().replace("\"", "\\\"").replace("\n", "\\n");
            return Response.ok("{\"response\": \"" + textoLimpio + "\"}", MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            logger.severe("REST: Error al contactar con gRPC: " + e.getMessage());
            return Response.serverError().entity("{\"error\": \"Error interno de comunicación\"}").build();
        }
    }
}

package es.um.sisdist.backend.grpc.impl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.LoginRequest;
import es.um.sisdist.backend.grpc.LoginResponse;

public class TestLogin {
    public static void main(String[] args) {
        // 1. Conectar al servidor local en el puerto 50051
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        // 2. Crear el stub (el cliente)
        var stub = GrpcServiceGrpc.newBlockingStub(channel);

        // PRUEBA 1: Login Correcto
        System.out.println("Enviando credenciales correctas (test@um.es)...");
        LoginRequest requestOk = LoginRequest.newBuilder()
                .setEmail("test@um.es")
                .setPassword("1234")
                .build();

        try {
            LoginResponse responseOk = stub.login(requestOk);
            System.out.println("  -> Resultado: " + responseOk.getSuccess()); // Debería ser true
            if (responseOk.getSuccess()) {
                System.out.println("  -> Token recibido: " + responseOk.getToken());
            }
        } catch (Exception e) {
            System.out.println("Error conectando: " + e.getMessage());
        }

        // PRUEBA 2: Login Incorrecto
        System.out.println("\nEnviando credenciales malas...");
        LoginRequest requestBad = LoginRequest.newBuilder()
                .setEmail("hacker@malvado.com")
                .setPassword("0000")
                .build();

        LoginResponse responseBad = stub.login(requestBad);
        System.out.println("  -> Resultado: " + responseBad.getSuccess()); // Debería ser false

        channel.shutdown();
    }
}
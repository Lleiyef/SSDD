package es.um.sisdist.backend.grpc.impl;

import es.um.sisdist.backend.dao.auth.JwtUtil;
import io.grpc.*;

class JwtServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTH_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getBareMethodName();
        // Ping y Login no requieren JWT
        if ("Ping".equals(method) || "Login".equals(method)) {
            return next.startCall(call, headers);
        }

        String authHeader = headers.get(AUTH_KEY);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Falta cabecera Authorization"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(7);
        if (JwtUtil.getClaims(token).isEmpty()) {
            call.close(Status.UNAUTHENTICATED.withDescription("Token JWT inválido"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}

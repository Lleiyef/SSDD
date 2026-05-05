package es.um.sisdist.backend.Service.impl;

import io.grpc.*;

public class JwtClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTH_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final ThreadLocal<String> currentToken = new ThreadLocal<>();

    public void setToken(String jwt) {
        currentToken.set(jwt);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = currentToken.get();
                if (token != null) {
                    headers.put(AUTH_KEY, "Bearer " + token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}

package es.um.sisdist.backend.grpc.impl;

import java.util.logging.Logger;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PingResponse;
import io.grpc.stub.StreamObserver;
import es.um.sisdist.backend.grpc.LoginRequest;
import es.um.sisdist.backend.grpc.LoginResponse;
import es.um.sisdist.backend.grpc.UserMessage;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase {
	private Logger logger;

	public GrpcServiceImpl(Logger logger) {
		super();
		this.logger = logger;
	}

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
		logger.info("Recived PING request, value = " + request.getV());
		responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
		responseObserver.onCompleted();
	}

	@Override
	public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
		// [MEJORA] 1. Log de entrada: Vemos quién intenta entrar
		logger.info("Intento de Login recibido para: " + request.getEmail());

		// 1. Extraer datos
		String email = request.getEmail();
		String password = request.getPassword();

		// 2. Lógica de validación (Dummy por ahora)
		boolean success = (email.equals("test@um.es") && password.equals("1234"))
				|| (email.equals("dsevilla@um.es") && password.equals("admin"));

		// [MEJORA] 2. Log de resultado: Vemos si acertó o falló
		if (success) {
			logger.info("Login EXITOSO para usuario: " + email);
		} else {
			logger.warning("Login FALLIDO (credenciales incorrectas) para: " + email);
		}

		// 3. Preparar la respuesta
		LoginResponse.Builder responseBuilder = LoginResponse.newBuilder()
				.setSuccess(success);

		// [Token Dummy] Si es correcto, devolvemos datos y token falso
		if (success) {
			responseBuilder.setToken("token-falso-12345")
					.setUser(UserMessage.newBuilder()
							.setId("1")
							.setEmail(email)
							.setName("Usuario Test") // Podrías personalizar esto según el email
							.setVisits(1)
							.build());
		}

		// 4. Enviar respuesta
		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	/*
	 * @Override
	 * public void storeImage(ImageData request, StreamObserver<Empty>
	 * responseObserver)
	 * {
	 * logger.info("Add image " + request.getId());
	 * imageMap.put(request.getId(),request);
	 * responseObserver.onNext(Empty.newBuilder().build());
	 * responseObserver.onCompleted();
	 * }
	 * 
	 * @Override
	 * public StreamObserver<ImageData> storeImages(StreamObserver<Empty>
	 * responseObserver)
	 * {
	 * // La respuesta, sólo un objeto Empty
	 * responseObserver.onNext(Empty.newBuilder().build());
	 * 
	 * // Se retorna un objeto que, al ser llamado en onNext() con cada
	 * // elemento enviado por el cliente, reacciona correctamente
	 * return new StreamObserver<ImageData>() {
	 * 
	 * @Override
	 * public void onCompleted() {
	 * // Terminar la respuesta.
	 * responseObserver.onCompleted();
	 * }
	 * 
	 * @Override
	 * public void onError(Throwable arg0) {
	 * }
	 * 
	 * @Override
	 * public void onNext(ImageData imagedata)
	 * {
	 * logger.info("Add image (multiple) " + imagedata.getId());
	 * imageMap.put(imagedata.getId(), imagedata);
	 * }
	 * };
	 * }
	 * 
	 * @Override
	 * public void obtainImage(ImageSpec request, StreamObserver<ImageData>
	 * responseObserver) {
	 * // TODO Auto-generated method stub
	 * super.obtainImage(request, responseObserver);
	 * }
	 * 
	 * @Override
	 * public StreamObserver<ImageSpec> obtainCollage(StreamObserver<ImageData>
	 * responseObserver) {
	 * // TODO Auto-generated method stub
	 * return super.obtainCollage(responseObserver);
	 * }
	 */
}
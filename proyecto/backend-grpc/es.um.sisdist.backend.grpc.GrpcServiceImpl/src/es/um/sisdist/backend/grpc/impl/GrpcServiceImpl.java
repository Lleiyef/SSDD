package es.um.sisdist.backend.grpc.impl;

import java.util.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.auth.JwtUtil;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PingResponse;
import io.grpc.stub.StreamObserver;
import es.um.sisdist.backend.grpc.LoginRequest;
import es.um.sisdist.backend.grpc.LoginResponse;
import es.um.sisdist.backend.grpc.UserMessage;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase {
	private Logger logger;
	private final IUserDAO userDAO;

	public GrpcServiceImpl(Logger logger) {
		super();
		this.logger = logger;
		this.userDAO = new DAOFactoryImpl().createSQLUserDAO();
	}

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
		logger.info("Recived PING request, value = " + request.getV());
		responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
		responseObserver.onCompleted();
	}

	@Override
	public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
		logger.info("Intento de Login recibido para: " + request.getEmail());

		String email = request.getEmail();
		String password = request.getPassword();

		Optional<User> userOpt = userDAO.getUserByEmail(email);
		boolean success = userOpt.isPresent()
			&& UserUtils.md5pass(password).equals(userOpt.get().getPassword_hash());

		LoginResponse.Builder responseBuilder = LoginResponse.newBuilder().setSuccess(success);

		if (success) {
			User u = userOpt.get();
			String token = JwtUtil.generateToken(u.getId(), u.getEmail());
			logger.info("Login EXITOSO para: " + email);
			responseBuilder.setToken(token)
				.setUser(UserMessage.newBuilder()
					.setId(u.getId())
					.setEmail(u.getEmail())
					.setName(u.getName())
					.setVisits(u.getVisits())
					.build());
		} else {
			logger.warning("Login FALLIDO para: " + email);
		}

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void sendPrompt(PromptRequest request, StreamObserver<PromptResponse> responseObserver) {
		String promptTexto = request.getPrompt();
		logger.info("gRPC: Recibido prompt del usuario: " + promptTexto);

		String llamaResponse = "";
		try {
			// 1. Configurar cliente HTTP para llamar al contenedor Dummy
			// El contenedor se llama "ssdd-llamachat" y expone el puerto 5020
			HttpClient client = HttpClient.newHttpClient();

			// Preparamos un JSON básico con la pregunta
			String jsonBody = "{\"prompt\": \"" + promptTexto.replace("\"", "\\\"") + "\"}";

			// Hacemos la petición POST al contenedor de IA
			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create("http://ssdd-llamachat:5020/prompt"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			logger.info("gRPC: Enviando petición al contenedor LlamaChat (Dummy)...");

			// 2. Enviar la petición y esperar la respuesta (el dummy tarda ~5 segs)
			HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			// La respuesta de LlamaChat se guarda aquí
			llamaResponse = httpResponse.body();
			logger.info("gRPC: Respuesta del LlamaChat recibida: " + llamaResponse);

		} catch (Exception e) {
			logger.severe("gRPC: Error conectando con LlamaChat Dummy: " + e.getMessage());
			llamaResponse = "Error interno: No se pudo contactar con la IA.";
		}

		// 3. Empaquetar la respuesta en el formato gRPC y devolverla
		PromptResponse response = PromptResponse.newBuilder()
				.setResponse(llamaResponse)
				.build();

		responseObserver.onNext(response);
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
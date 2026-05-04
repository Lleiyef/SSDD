/**
 *
 */
package es.um.sisdist.backend.Service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.IDAOFactory;
import es.um.sisdist.backend.dao.dialogue.IDialogueDAO;
import es.um.sisdist.backend.dao.message.IMessageDAO;
import es.um.sisdist.backend.dao.models.Dialogue;
import es.um.sisdist.backend.dao.models.Message;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author dsevilla
 *
 */
public class AppLogicImpl
{
    public enum PromptSubmitStatus { OK, NOT_FOUND, NOT_READY, WRONG_TOKEN }

    IDAOFactory daoFactory;
    IUserDAO dao;
    IDialogueDAO dialogueDAO;
    IMessageDAO messageDAO;

    private static final Logger logger = Logger.getLogger(AppLogicImpl.class.getName());

    private final ManagedChannel channel;
    private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
    //private final GrpcServiceGrpc.GrpcServiceStub asyncStub;

    static AppLogicImpl instance = new AppLogicImpl();

    private AppLogicImpl()
    {
        daoFactory = new DAOFactoryImpl();
        Optional<String> backend = Optional.ofNullable(System.getenv("DB_BACKEND"));
        
        if (backend.isPresent() && backend.get().equals("mongo"))
            dao = daoFactory.createMongoUserDAO();
        else
            dao = daoFactory.createSQLUserDAO();

        dialogueDAO = daoFactory.createSQLDialogueDAO();
        messageDAO = daoFactory.createSQLMessageDAO();

        var grpcServerName = Optional.ofNullable(System.getenv("GRPC_SERVER"));
        var grpcServerPort = Optional.ofNullable(System.getenv("GRPC_SERVER_PORT"));

        channel = ManagedChannelBuilder
                .forAddress(grpcServerName.orElse("localhost"), Integer.parseInt(grpcServerPort.orElse("50051")))
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS
                // to avoid needing certificates.
                .usePlaintext().build();
        blockingStub = GrpcServiceGrpc.newBlockingStub(channel);
        //asyncStub = GrpcServiceGrpc.newStub(channel);
    }

    public static AppLogicImpl getInstance()
    {
        return instance;
    }

    public Optional<User> getUserByEmail(String userId)
    {
        Optional<User> u = dao.getUserByEmail(userId);
        return u;
    }

    public Optional<User> getUserById(String userId)
    {
        return dao.getUserById(userId);
    }

    public boolean ping(int v)
    {
    	logger.info("Issuing ping, value: " + v);
    	
        // Test de grpc, puede hacerse con la BD
    	var msg = PingRequest.newBuilder().setV(v).build();
        var response = blockingStub.ping(msg);
        
        return response.getV() == v;
    }

    // El frontend, a través del formulario de login,
    // envía el usuario y pass, que se convierte a un DTO. De ahí
    // obtenemos la consulta a la base de datos, que nos retornará,
    // si procede,
    public Optional<User> checkLogin(String email, String pass)
    {
        Optional<User> u = dao.getUserByEmail(email);

        if (u.isPresent())
        {
            String hashed_pass = UserUtils.md5pass(pass);
            if (0 == hashed_pass.compareTo(u.get().getPassword_hash()))
                return u;
        }

        return Optional.empty();
    }

    public boolean createUser(String email, String name, String password)
    {
        if (dao.getUserByEmail(email).isPresent())
            return false;

        User newUser = new User(email, UserUtils.md5pass(password), name, "", 0);
        return dao.createUser(newUser);
    }

    // --- Dialogue methods ---

    public List<Dialogue> getDialoguesByUser(String userId)
    {
        return dialogueDAO.getDialoguesByUser(userId);
    }

    public List<String> getDialogueIdsByUser(String userId)
    {
        return dialogueDAO.getDialoguesByUser(userId)
            .stream().map(Dialogue::getId).collect(Collectors.toList());
    }

    public Optional<Dialogue> getDialogueByUserAndName(String userId, String name)
    {
        return dialogueDAO.getDialogueByUserAndName(userId, name);
    }

    public Optional<Dialogue> createDialogue(String userId, String name)
    {
        if (dialogueDAO.getDialogueByUserAndName(userId, name).isPresent())
            return Optional.empty();

        Dialogue d = new Dialogue(
            UUID.randomUUID().toString(),
            userId,
            name,
            "READY",
            UUID.randomUUID().toString(),
            System.currentTimeMillis());

        return dialogueDAO.createDialogue(d) ? Optional.of(d) : Optional.empty();
    }

    public Optional<Dialogue> createOrGetDialogue(String userId, String name)
    {
        return dialogueDAO.getDialogueByUserAndName(userId, name)
            .or(() -> {
                Dialogue d = new Dialogue(
                    UUID.randomUUID().toString(),
                    userId,
                    name,
                    "READY",
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis());
                return dialogueDAO.createDialogue(d) ? Optional.of(d) : Optional.empty();
            });
    }

    public boolean deleteDialogue(String userId, String name)
    {
        return dialogueDAO.getDialogueByUserAndName(userId, name)
            .map(d -> dialogueDAO.deleteDialogue(d.getId()))
            .orElse(false);
    }

    public List<Message> getMessagesByDialogue(String dialogueId)
    {
        return messageDAO.getMessagesByDialogue(dialogueId);
    }

    public PromptSubmitStatus submitPrompt(String userId, String dname, String token,
                                           String prompt, long timestamp)
    {
        Optional<Dialogue> dOpt = dialogueDAO.getDialogueByUserAndName(userId, dname);
        if (dOpt.isEmpty()) return PromptSubmitStatus.NOT_FOUND;

        Dialogue d = dOpt.get();

        if (!"READY".equals(d.getStatus())) return PromptSubmitStatus.NOT_READY;
        if (!token.equals(d.getNextToken()))  return PromptSubmitStatus.WRONG_TOKEN;

        // Persistir mensaje con respuesta vacía
        String messageId = UUID.randomUUID().toString();
        Message msg = new Message(messageId, d.getId(), prompt, null, timestamp);
        messageDAO.createMessage(msg);

        // Pasar a BUSY con nuevo next_token
        String newNextToken = UUID.randomUUID().toString();
        d.setStatus("BUSY");
        d.setNextToken(newNextToken);
        dialogueDAO.updateDialogue(d);

        // Llamada gRPC en background — cuando llegue la respuesta se persiste y el
        // diálogo vuelve a READY
        CompletableFuture.runAsync(() -> {
            try
            {
                var req = PromptRequest.newBuilder().setPrompt(prompt).build();
                var resp = blockingStub.sendPrompt(req);
                msg.setAnswer(resp.getResponse());
                messageDAO.updateMessage(msg);
            }
            catch (Exception e)
            {
                logger.severe("Error en llamada gRPC: " + e.getMessage());
                msg.setAnswer("[Error: no se pudo obtener respuesta]");
                messageDAO.updateMessage(msg);
            }
            finally
            {
                d.setStatus("READY");
                dialogueDAO.updateDialogue(d);
            }
        });

        return PromptSubmitStatus.OK;
    }

    public boolean endDialogue(String userId, String dname)
    {
        return dialogueDAO.getDialogueByUserAndName(userId, dname)
            .map(d -> {
                d.setStatus("FINISHED");
                return dialogueDAO.updateDialogue(d);
            })
            .orElse(false);
    }
}

package es.um.sisdist.backend.dao.message;

import java.util.List;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.Message;

public interface IMessageDAO
{
    Optional<Message> getMessageById(String id);

    List<Message> getMessagesByDialogue(String dialogueId);

    boolean createMessage(Message message);

    boolean updateMessage(Message message);
}

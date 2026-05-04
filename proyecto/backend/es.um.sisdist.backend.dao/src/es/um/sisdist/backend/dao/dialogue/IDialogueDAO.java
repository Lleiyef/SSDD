package es.um.sisdist.backend.dao.dialogue;

import java.util.List;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.Dialogue;

public interface IDialogueDAO
{
    Optional<Dialogue> getDialogueById(String id);

    Optional<Dialogue> getDialogueByUserAndName(String userId, String name);

    List<Dialogue> getDialoguesByUser(String userId);

    boolean createDialogue(Dialogue dialogue);

    boolean updateDialogue(Dialogue dialogue);

    boolean deleteDialogue(String id);
}

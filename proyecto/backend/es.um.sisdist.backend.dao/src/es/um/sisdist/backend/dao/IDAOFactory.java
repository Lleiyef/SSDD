/**
 *
 */
package es.um.sisdist.backend.dao;

import es.um.sisdist.backend.dao.dialogue.IDialogueDAO;
import es.um.sisdist.backend.dao.message.IMessageDAO;
import es.um.sisdist.backend.dao.user.IUserDAO;

/**
 * @author dsevilla
 *
 */
public interface IDAOFactory
{
    public IUserDAO createSQLUserDAO();

    public IUserDAO createMongoUserDAO();

    public IDialogueDAO createSQLDialogueDAO();

    public IMessageDAO createSQLMessageDAO();
}

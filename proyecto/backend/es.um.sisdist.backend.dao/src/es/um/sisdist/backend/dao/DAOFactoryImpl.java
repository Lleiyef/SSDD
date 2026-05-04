/**
 *
 */
package es.um.sisdist.backend.dao;

import es.um.sisdist.backend.dao.dialogue.IDialogueDAO;
import es.um.sisdist.backend.dao.dialogue.SQLDialogueDAO;
import es.um.sisdist.backend.dao.message.IMessageDAO;
import es.um.sisdist.backend.dao.message.SQLMessageDAO;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.dao.user.MongoUserDAO;
import es.um.sisdist.backend.dao.user.SQLUserDAO;

/**
 * @author dsevilla
 *
 */
public class DAOFactoryImpl implements IDAOFactory
{
    @Override
    public IUserDAO createSQLUserDAO()
    {
        return new SQLUserDAO();
    }

    @Override
    public IUserDAO createMongoUserDAO()
    {
        return new MongoUserDAO();
    }

    @Override
    public IDialogueDAO createSQLDialogueDAO()
    {
        return new SQLDialogueDAO();
    }

    @Override
    public IMessageDAO createSQLMessageDAO()
    {
        return new SQLMessageDAO();
    }
}

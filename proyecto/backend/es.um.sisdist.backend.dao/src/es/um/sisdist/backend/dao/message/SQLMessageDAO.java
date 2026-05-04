package es.um.sisdist.backend.dao.message;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.Message;

public class SQLMessageDAO implements IMessageDAO
{
    Optional<Connection> conn;

    public SQLMessageDAO()
    {
        Connection connection = null;
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
            String sqlServerName = Optional.ofNullable(System.getenv("SQL_SERVER")).orElse("localhost");
            String dbName = Optional.ofNullable(System.getenv("DB_NAME")).orElse("ssdd");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + sqlServerName + "/" + dbName + "?user=root&password=root");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        conn = Optional.ofNullable(connection);
    }

    @Override
    public Optional<Message> getMessageById(String id)
    {
        if (conn.isEmpty()) return Optional.empty();
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "SELECT * FROM messages WHERE id = ?");
            stm.setString(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) return fromResultSet(rs);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public List<Message> getMessagesByDialogue(String dialogueId)
    {
        List<Message> result = new ArrayList<>();
        if (conn.isEmpty()) return result;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "SELECT * FROM messages WHERE dialogue_id = ? ORDER BY timestamp ASC");
            stm.setString(1, dialogueId);
            ResultSet rs = stm.executeQuery();
            while (rs.next())
                fromResultSet(rs).ifPresent(result::add);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public boolean createMessage(Message message)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "INSERT INTO messages(id, dialogue_id, prompt, answer, timestamp) VALUES(?,?,?,?,?)");
            stm.setString(1, message.getId());
            stm.setString(2, message.getDialogueId());
            stm.setString(3, message.getPrompt());
            stm.setString(4, message.getAnswer());
            stm.setLong(5, message.getTimestamp());
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public boolean updateMessage(Message message)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "UPDATE messages SET answer=? WHERE id=?");
            stm.setString(1, message.getAnswer());
            stm.setString(2, message.getId());
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Optional<Message> fromResultSet(ResultSet rs)
    {
        try
        {
            return Optional.of(new Message(
                rs.getString("id"),
                rs.getString("dialogue_id"),
                rs.getString("prompt"),
                rs.getString("answer"),
                rs.getLong("timestamp")));
        }
        catch (SQLException e) { return Optional.empty(); }
    }
}

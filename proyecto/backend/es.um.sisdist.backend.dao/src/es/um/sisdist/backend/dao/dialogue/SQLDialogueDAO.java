package es.um.sisdist.backend.dao.dialogue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.Dialogue;

public class SQLDialogueDAO implements IDialogueDAO
{
    Optional<Connection> conn;

    public SQLDialogueDAO()
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
    public Optional<Dialogue> getDialogueById(String id)
    {
        if (conn.isEmpty()) return Optional.empty();
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "SELECT * FROM dialogues WHERE id = ?");
            stm.setString(1, id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) return fromResultSet(rs);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public Optional<Dialogue> getDialogueByUserAndName(String userId, String name)
    {
        if (conn.isEmpty()) return Optional.empty();
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "SELECT * FROM dialogues WHERE user_id = ? AND name = ?");
            stm.setString(1, userId);
            stm.setString(2, name);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) return fromResultSet(rs);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public List<Dialogue> getDialoguesByUser(String userId)
    {
        List<Dialogue> result = new ArrayList<>();
        if (conn.isEmpty()) return result;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "SELECT * FROM dialogues WHERE user_id = ? ORDER BY created_at ASC");
            stm.setString(1, userId);
            ResultSet rs = stm.executeQuery();
            while (rs.next())
                fromResultSet(rs).ifPresent(result::add);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public boolean createDialogue(Dialogue dialogue)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "INSERT INTO dialogues(id, user_id, name, status, next_token, created_at) VALUES(?,?,?,?,?,?)");
            stm.setString(1, dialogue.getId());
            stm.setString(2, dialogue.getUserId());
            stm.setString(3, dialogue.getName());
            stm.setString(4, dialogue.getStatus());
            stm.setString(5, dialogue.getNextToken());
            stm.setLong(6, dialogue.getCreatedAt());
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public boolean updateDialogue(Dialogue dialogue)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "UPDATE dialogues SET status=?, next_token=? WHERE id=?");
            stm.setString(1, dialogue.getStatus());
            stm.setString(2, dialogue.getNextToken());
            stm.setString(3, dialogue.getId());
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public boolean deleteDialogue(String id)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "DELETE FROM dialogues WHERE id=?");
            stm.setString(1, id);
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Optional<Dialogue> fromResultSet(ResultSet rs)
    {
        try
        {
            return Optional.of(new Dialogue(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("status"),
                rs.getString("next_token"),
                rs.getLong("created_at")));
        }
        catch (SQLException e) { return Optional.empty(); }
    }
}

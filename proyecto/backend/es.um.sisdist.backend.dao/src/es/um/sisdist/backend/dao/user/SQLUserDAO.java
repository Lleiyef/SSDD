/**
 *
 */
package es.um.sisdist.backend.dao.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import es.um.sisdist.backend.dao.models.User;

/**
 * @author dsevilla
 *
 */
public class SQLUserDAO implements IUserDAO
{
    Optional<Connection> conn;

    public SQLUserDAO()
    {
        // Generate an optional from a direct connection attempt

        Connection connection = null;
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();

            // Si el nombre del host se pasa por environment, se usa aquí.
            // Si no, se usa localhost. Esto permite configurarlo de forma
            // sencilla para cuando se ejecute en el contenedor, y a la vez
            // se pueden hacer pruebas locales
            String sqlServerName = Optional.ofNullable(System.getenv("SQL_SERVER")).orElse("localhost");
            String dbName = Optional.ofNullable(System.getenv("DB_NAME")).orElse("ssdd");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + sqlServerName + "/" + dbName + "?user=root&password=root");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        conn = Optional.ofNullable(connection);
    }


    @Override
    public Optional<User> getUserById(String id)
    {
        if (conn.isEmpty()) return Optional.empty();
        try
        {
            PreparedStatement stm = conn.get().prepareStatement("SELECT * FROM users WHERE id = ?");
            stm.setString(1, id);
            ResultSet result = stm.executeQuery();
            if (result.next()) return fromResultSet(result);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public Optional<User> getUserByEmail(String email)
    {
        if (conn.isEmpty()) return Optional.empty();
        try
        {
            PreparedStatement stm = conn.get().prepareStatement("SELECT * FROM users WHERE email = ?");
            stm.setString(1, email);
            ResultSet result = stm.executeQuery();
            if (result.next()) return fromResultSet(result);
        }
        catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public boolean createUser(User user)
    {
        if (conn.isEmpty()) return false;
        try
        {
            PreparedStatement stm = conn.get().prepareStatement(
                "INSERT INTO users(id, email, password_hash, name, token, visits) VALUES(?,?,?,?,?,?)");
            stm.setString(1, user.getId());
            stm.setString(2, user.getEmail());
            stm.setString(3, user.getPassword_hash());
            stm.setString(4, user.getName());
            stm.setString(5, user.getToken() != null ? user.getToken() : "");
            stm.setInt(6, 0);
            return stm.executeUpdate() == 1;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Optional<User> fromResultSet(ResultSet result)
    {
        try
        {
            return Optional.of(new User(
                result.getString(1), // id
                result.getString(2), // email
                result.getString(3), // pwhash
                result.getString(4), // name
                result.getString(5), // token
                result.getInt(6)));  // visits
        }
        catch (SQLException e) { return Optional.empty(); }
    }
}

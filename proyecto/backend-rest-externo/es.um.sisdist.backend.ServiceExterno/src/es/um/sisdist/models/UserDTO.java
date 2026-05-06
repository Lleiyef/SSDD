package es.um.sisdist.models;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class UserDTO
{
    private String id;
    private String email;
    private String password;
    private String name;
    private String token;
    private int visits;
    private List<String> dialogueIds;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getToken() { return token; }
    public void setToken(String tOKEN) { token = tOKEN; }

    public int getVisits() { return visits; }
    public void setVisits(int visits) { this.visits = visits; }

    public List<String> getDialogueIds() { return dialogueIds; }
    public void setDialogueIds(List<String> dialogueIds) { this.dialogueIds = dialogueIds; }

    public UserDTO(String id, String email, String password, String name, String tOKEN, int visits)
    {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        token = tOKEN;
        this.visits = visits;
    }

    public UserDTO() {}
}

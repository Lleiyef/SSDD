package es.um.sisdist.backend.dao.models;

public class Dialogue
{
    private String id;
    private String userId;
    private String name;
    private String status; // READY, BUSY, FINISHED
    private String nextToken;
    private long createdAt;

    public Dialogue() {}

    public Dialogue(String id, String userId, String name, String status, String nextToken, long createdAt)
    {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.nextToken = nextToken;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNextToken() { return nextToken; }
    public void setNextToken(String nextToken) { this.nextToken = nextToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

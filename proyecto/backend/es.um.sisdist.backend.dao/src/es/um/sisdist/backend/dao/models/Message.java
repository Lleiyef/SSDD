package es.um.sisdist.backend.dao.models;

public class Message
{
    private String id;
    private String dialogueId;
    private String prompt;
    private String answer;
    private long timestamp;

    public Message() {}

    public Message(String id, String dialogueId, String prompt, String answer, long timestamp)
    {
        this.id = id;
        this.dialogueId = dialogueId;
        this.prompt = prompt;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDialogueId() { return dialogueId; }
    public void setDialogueId(String dialogueId) { this.dialogueId = dialogueId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

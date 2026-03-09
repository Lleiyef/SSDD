package es.um.sisdist.backend.Service.impl;

public class ChatMessage {
    private String prompt;

    // Constructor vacío obligatorio
    public ChatMessage() {
    }

    // Getters y Setters obligatorios para Yasson (la librería JSON)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
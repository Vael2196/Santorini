package online.protocol;
public record ChatMessage(String id, int seat, String name, String text, long sentAt) {}

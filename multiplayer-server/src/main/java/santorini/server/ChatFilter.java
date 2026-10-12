package santorini.server;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;
final class ChatFilter {
    private static final Pattern LINKS=Pattern.compile("https?://|www\\.|[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|\\b(?:[a-z0-9-]+\\.)+[a-z]{2,63}\\b",Pattern.CASE_INSENSITIVE);
    private static final Pattern ABUSE=Pattern.compile("\\b(?:fuck(?:ing|er)?|shit|cunt|bitch|asshole|arsehole)\\b",Pattern.CASE_INSENSITIVE);
    private static void size(String text) {
        if (text == null || text.codePointCount(0,text.length()) > 280 || text.getBytes(StandardCharsets.UTF_8).length > 1000)
            throw new IllegalArgumentException("Keep messages to 280 characters");
    }
    static String clean(String raw) {
        size(raw);
        if (raw.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c)==Character.FORMAT || Character.getType(c)==Character.SURROGATE))
            throw new IllegalArgumentException("Use plain text without control characters");
        String text=Normalizer.normalize(raw,Normalizer.Form.NFKC).replaceAll("\\s+"," ").strip();
        size(text);
        if (text.isEmpty()) throw new IllegalArgumentException("Write a message first");
        if (LINKS.matcher(text).find()) throw new IllegalArgumentException("Links and email addresses are not allowed in chat");
        return ABUSE.matcher(text).replaceAll("••••");
    }
}

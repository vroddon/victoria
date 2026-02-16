package vroddon.victoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Stateful chat session for DeepSeek (OpenAI-compatible format).
 * Java 8 compatible: avoids lambdas/streams and Java 9+ APIs.
 *
 * Usage:
 *   DeepSeekSession s = new DeepSeekSession("Eres un conversador simpático.");
 *   System.out.println(s.chat("¿Qué tal estás?"));
 *   System.out.println(s.chat("Recuérdame cómo te describí al inicio."));
 *   System.out.println(s.chat("Ahora háblame en inglés."));
 */
public class DeepSeekSession {

    private static final String LLM_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    private final String apiKey;
    private final ObjectMapper mapper;
    private final List<Message> history;
    private String model;

    /**
     * Create a session with an optional system prompt.
     */
    public DeepSeekSession(String systemPrompt) {
        this.apiKey = System.getenv("DEEPSEEK");
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing DEEPSEEK API key in environment variable DEEPSEEK");
        }
        this.model = DEFAULT_MODEL;
        this.mapper = new ObjectMapper();
        this.history = new ArrayList<Message>();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            this.history.add(new Message("system", systemPrompt));
        }
    }

    /**
     * Optionally choose another model (if your account supports it).
     */
    public void setModel(String model) {
        if (model != null && !model.trim().isEmpty()) {
            this.model = model;
        }
    }

    /**
     * Send a user message, get assistant reply, and keep both in history.
     */
    public String chat(String userMessage) throws Exception {
        if (userMessage == null) userMessage = "";
        this.history.add(new Message("user", userMessage));

        // Build payload with full history
        ObjectNode root = mapper.createObjectNode();
        root.put("model", this.model);

        ArrayNode msgs = mapper.createArrayNode();
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            ObjectNode item = mapper.createObjectNode();
            item.put("role", m.role);
            item.put("content", m.content);
            msgs.add(item);
        }
        root.set("messages", msgs);
        root.put("stream", false);

        String responseBody = postJson(LLM_URL, root.toString());

        JsonNode rootNode = mapper.readTree(responseBody);
        JsonNode choicesNode = rootNode.path("choices");
        String assistant = "";
        if (choicesNode.isArray() && choicesNode.size() > 0) {
            assistant = choicesNode.get(0).path("message").path("content").asText();
        }

        // Append assistant message to history
        this.history.add(new Message("assistant", assistant));
        return assistant;
    }

    /**
     * Clear the conversation but keep the original system prompt (if present).
     */
    public void reset() {
        Message system = null;
        for (int i = 0; i < history.size(); i++) {
            Message m = history.get(i);
            if ("system".equals(m.role)) {
                system = m;
                break;
            }
        }
        this.history.clear();
        if (system != null) {
            this.history.add(system);
        }
    }

    /**
     * Replace long history with a short summary to save tokens.
     * Typically, you would ask the model to produce the summary beforehand.
     */
    public void replaceWithSummary(String summary, String newSystemPromptIfAny) {
        this.history.clear();
        if (newSystemPromptIfAny != null && !newSystemPromptIfAny.trim().isEmpty()) {
            this.history.add(new Message("system", newSystemPromptIfAny));
        } else {
            this.history.add(new Message("system", "You are a helpful assistant."));
        }
        this.history.add(new Message("assistant", "Resumen de la conversación hasta ahora: " + (summary == null ? "" : summary)));
    }

    /**
     * Returns a defensive copy of the current history.
     */
    public List<Message> getHistorySnapshot() {
        return new ArrayList<Message>(this.history);
    }

    // ---- HTTP ----
    private String postJson(String endpoint, String json) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000); // 15s
            conn.setReadTimeout(60000);    // 60s
            conn.setRequestProperty("Authorization", "Bearer " + this.apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is;
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
                if (is == null) {
                    // No body—throw with status only
                    throw new RuntimeException("HTTP " + code + " from DeepSeek (no error body)");
                }
            }

            String body = readStreamToString(is);
            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + " from DeepSeek: " + body);
            }
            return body;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readStreamToString(InputStream is) throws Exception {
        try {
            // Using BufferedReader for Java 8 compatibility
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) break;
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } finally {
            if (is != null) is.close();
        }
    }

    // ---- Value object ----
    public static class Message {
        public final String role;
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content == null ? "" : content;
        }
    }

    // ---- Demo main ----
    public static void main(String[] args) throws Exception {
        DeepSeekSession session = new DeepSeekSession("Eres un conversador simpático.");
        System.out.println(session.chat("¿Qué tal estás?"));
        System.out.println(session.chat("Recuérdame cómo te describí al inicio."));
        System.out.println(session.chat("Ahora háblame en inglés."));
    }
}
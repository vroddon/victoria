package vroddon.victoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Interfaz con DeepSeek
 * @author victor
 */
public class DeepSeek {
    private static final String LLM_URL = "https://api.deepseek.com/v1/chat/completions";
    private static String MODEL = "deepseek-chat"; // o deepseek-vl

    public static void main(String[] args) throws Exception {
        DeepSeek ds = new DeepSeek();
        String curso ="";
        String text = DeepSeek.chat("Eres un conversador simpático.", "¿Qué tal estás?");
        System.out.println(text);        
    }    
    public static String chat(String tarea, String pregunta)
    {
        String apiKey = System.getenv("DEEPSEEK");
        String endpoint = LLM_URL;
        String payload = "{"
                + "\"model\":\"deepseek-chat\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\""+tarea+"\"}, "
                + "{\"role\":\"user\",\"content\":\""+pregunta+"\"}"
                + "],"
                + "\"stream\":false"
                + "}";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();

            Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();
            
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.toString());
                String content = rootNode.path("choices").get(0).path("message").path("content").asText();
                return content;
            } catch (Exception e2) {
                System.out.println("Error parseando la respuesta de DeepSeek");
                return "";
            }            
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }    
    public static String recognizeTextFromImage(File imageFile, String apiKey) throws Exception {

        
        // 1. Leer imagen y codificar en Base64
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        System.out.println(base64Image.length());
        

        // 2. Construir JSON de la peticiÃ³n
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        MODEL = "deepseek-vl";
        root.put("model", MODEL);
        root.put("temperature", 0);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");

        ArrayNode content = mapper.createArrayNode();

        // Texto (prompt OCR)
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", "Extract all readable text from this image. Preserve line breaks.");
        content.add(textPart);

        // Imagen
        ObjectNode imagePart = mapper.createObjectNode();
        imagePart.put("type", "image_url");

        ObjectNode imageUrl = mapper.createObjectNode();
        imageUrl.put("url", "data:image/png;base64," + base64Image);

        imagePart.set("image_url", imageUrl);
        content.add(imagePart);

        userMessage.set("content", content);
        messages.add(userMessage);

        root.set("messages", messages);

        // 3. Enviar peticiÃ³n HTTP
        URL url = new URL(LLM_URL); //API_URL
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(mapper.writeValueAsBytes(root));
        }

        // 4. Leer respuesta
        InputStream is = conn.getResponseCode() < 400
                ? conn.getInputStream()
                : conn.getErrorStream();

        JsonNode response = mapper.readTree(is);
        System.out.println(response.toPrettyString());
        // 5. Extraer texto OCR
        return response
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }
    
    
}

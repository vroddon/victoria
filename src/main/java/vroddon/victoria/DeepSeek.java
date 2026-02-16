package vroddon.victoria;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.OutputStream;
import java.util.Scanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Interfaz con DeepSeek
 * @author victor
 */
public class DeepSeek {
    private static final String LLM_URL = "https://api.deepseek.com/v1/chat/completions";
    private static String MODEL = "deepseek-chat"; // o deepseek-vl

    public static void main(String[] args) throws Exception {
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
        System.out.println(payload);
        
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

    
    
}

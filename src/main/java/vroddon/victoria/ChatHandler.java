package vroddon.victoria;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class ChatHandler extends HttpServlet {

    DeepSeekSession deepseek = new DeepSeekSession("Eres un conversador simpático.");
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Leer body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();

        // Extraer el mensaje (ultra simple, sin librerías JSON)
        String message = "";
        int start = body.indexOf("\"message\":\"");
        if (start != -1) {
            start += 11;
            int end = body.indexOf("\"", start);
            if (end != -1) {
                message = body.substring(start, end);
            }
        }

        // Respuesta eco
//        String reply = "Echo: " + message;
        String reply = deepseek.chat(message);

        // Devolver JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"reply\":\"" + reply + "\"}");
    }
}
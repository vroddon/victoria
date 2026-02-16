package vroddon.victoria;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatHandler extends HttpServlet {

    private static String apiKey = null;
    private static String LLM = "https://api.openai.com/v1/chat/completions";

    DeepSeekSession chino = new DeepSeekSession("You are a friendly conversationalist.");

    public ChatHandler() {
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();

        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }
        String question = sb.toString();

        String json = getAnswer(question);

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private String getAnswer(String question) {
        String res = "Leave me alone.";
        try{
            res= chino.chat(question);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return res;

//        return DeepSeek.chat("You are a nice chat", question);
    }

    private String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}

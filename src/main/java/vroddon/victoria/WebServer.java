package vroddon.victoria;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {

    private int port=8078;

    public WebServer(int _port) {
        port =_port;
    }

    public void start() throws Exception {

        Server server = new Server(port);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/victoria");        
        
//vvv        ServletContextHandler handler = new ServletContextHandler();
//vvv        handler.setContextPath("/");

        // Serve static files from src/main/resources/web/
        handler.setResourceBase(
            WebServer.class.getClassLoader().getResource("web").toExternalForm()
        );
        handler.addServlet(new ServletHolder("default", new org.eclipse.jetty.servlet.DefaultServlet()), "/");

        // Chat API servlet
        handler.addServlet(new ServletHolder(new ChatHandler()), "/chat");

        server.setHandler(handler);

        System.out.println("Server started on port " + port);
        server.start();
        server.join();
    }
    
    
}
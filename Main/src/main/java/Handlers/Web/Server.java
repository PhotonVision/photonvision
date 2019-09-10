package Handlers.Web;

import io.javalin.Javalin;
public class Server {
    public static void main(int port) {
        Javalin app = Javalin.create().start(port);
        app.get("/", ctx -> ctx.result("Hello World"));
    }
}

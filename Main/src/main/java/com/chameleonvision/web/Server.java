package com.chameleonvision.web;

import com.chameleonvision.settings.SettingsManager;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static List<WsContext> users = new ArrayList<WsContext>();
    public static void main(int port) {
        Javalin app = Javalin.create();
        app.config.addStaticFiles("web");
        app.ws("/websocket", ws ->{
            ws.onConnect(ctx -> {
                users.add(ctx);
                System.out.println("Socket Connected");
            });
            ws.onClose(ctx -> {
                users.remove(ctx);
                System.out.println("Socket Disconnected");
                SettingsManager.getInstance().SaveSettings();
            });
            ws.onMessage(ctx -> {
                broadcastMessage(ctx, ctx.message());
            });
        });
        app.start(port);
    }
    private static void broadcastMessage(WsContext sendingUser, String message){
        for (var user : users)
        {
            if (user != sendingUser){
                user.send(message);
            }
        }
    }

}

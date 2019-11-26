package com.chameleonvision.web;

import com.chameleonvision.config.ConfigManager;
import io.javalin.Javalin;


public class Server {
    public static ServerHandler handler;

    public static void main(int port) {
        handler = new ServerHandler();

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner=false);
        app.config.addStaticFiles("web");
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                handler.onConnect(ctx);
                System.out.println("Socket Connected");
            });
            ws.onClose(ctx -> {
                handler.onClose(ctx);
                System.out.println("Socket Disconnected");
                ConfigManager.saveSettings();
            });
            ws.onBinaryMessage(ctx -> {
                handler.onBinaryMessage(ctx);
            });
        });

        app.start(port);
    }
}
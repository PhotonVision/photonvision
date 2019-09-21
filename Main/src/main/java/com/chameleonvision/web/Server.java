package com.chameleonvision.web;

import com.chameleonvision.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import edu.wpi.cscore.VideoException;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.lang.reflect.Field;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;


public class Server {
    public static ServerHandler handler;

    public static void main(int port) {
        handler = new ServerHandler();

        Javalin app = Javalin.create();
        app.config.addStaticFiles("web");
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                handler.onConnect(ctx);
                System.out.println("Socket Connected");
            });
            ws.onClose(ctx -> {
                handler.onClose(ctx);
                System.out.println("Socket Disconnected");
                SettingsManager.saveSettings();
            });
            ws.onMessage(ctx -> {
                handler.onMessage(ctx);
            });
        });
        app.start(port);
    }
}

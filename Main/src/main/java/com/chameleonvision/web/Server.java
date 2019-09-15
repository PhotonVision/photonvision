package com.chameleonvision.web;

import com.chameleonvision.settings.SettingsManager;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;


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
                JSONObject jsonObject = new JSONObject(ctx.message());
                String key =jsonObject.keySet().toArray()[0].toString();
                Object value = jsonObject.get(key);
                System.out.println("Key: "+key+" Value: "+value);
                Field[] fields = Pipeline.class.getFields();
                for (Field f : fields)
                {//TODO: check calibration in output tab for crashes
                    if(f.getName().equals(key))
                    {
                        if(BeanUtils.isSimpleValueType(value.getClass()))
                            f.set(SettingsManager.getInstance().GetCurrentPipeline(),value);
                        else
                        if(value.getClass()==JSONArray.class){
                            f.set(SettingsManager.getInstance().GetCurrentPipeline(),((JSONArray)value).toList());
                        }
                    }
                }
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

package org.photonvision.vision.videoStream;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.opencv.core.MatOfByte;

import edu.wpi.first.math.Pair;
import io.javalin.websocket.WsContext;

public class SocketVideoStreamManager {

    private Map<Integer, SocketVideoStream> streams = new Hashtable<Integer, SocketVideoStream>();

    private static class ThreadSafeSingleton {
        private static final SocketVideoStreamManager INSTANCE = new SocketVideoStreamManager();
    }

    public static SocketVideoStreamManager getInstance() {
        return ThreadSafeSingleton.INSTANCE;
    }

    private SocketVideoStreamManager(){

    }

    // Register a new available camera stream
    public void addStream(SocketVideoStream newStream){
        streams.put(newStream.portID, newStream);
    }

    // Remove a previously-added camera stream, and unsubscribe all users
    public void removeStream(SocketVideoStream oldStream){
        streams.remove(oldStream.portID);
    }

    // Indicate a user would like to subscribe to a camera stream and get frames from it periodically
    public void addSubscription(WsContext user, int streamPortID){
        var stream = streams.get(streamPortID);
        stream.subscribeUser(user);
    }

    // Indicate a user would like to stop receiving one camera stream
    public void removeSubscription(WsContext user, int streamPortID){
        var stream = streams.get(streamPortID);
        stream.unsubscribeUser(user);
    }

    // Indicate a user no longer should get any camera streams
    public void removeAllSubscriptions(WsContext user){
        for (SocketVideoStream stream : streams.values()){
            stream.unsubscribeUser(user);
        }
    }

    // For a given user, return a list of ports and jpeg byte mats to transmit
    public List<Pair<Integer, MatOfByte>> getSendFrames(WsContext user){
        var retList = new ArrayList<Pair<Integer, MatOfByte>>();

        for (SocketVideoStream stream : streams.values()){
            if(stream.userIsSubscribed(user)){
                retList.add(new Pair<Integer, MatOfByte>(stream.portID, stream.getJPEGBytes()));
            }
        }

        return retList;
    }

    // Causes all streams to "re-trigger" and recieve and convert their next mjpeg frame
    // Only invoke this after all returned jpeg MatOfBytes have been used.
    public void allStreamConvertNextFrame(){
        for (SocketVideoStream stream : streams.values()){
            stream.convertNextFrame();
        }
    }
    
}

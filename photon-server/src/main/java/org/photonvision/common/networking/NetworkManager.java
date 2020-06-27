package org.photonvision.common.networking;

public class NetworkManager {
    private NetworkManager() {}

    private static class SingletonHolder {
        private static final NetworkManager INSTANCE = new NetworkManager();
    }

    public static NetworkManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private boolean isManaged = false;

    public void initialize(boolean shouldManage) {
        isManaged = shouldManage;
        if (!isManaged) {
            return;
        }
    }
}

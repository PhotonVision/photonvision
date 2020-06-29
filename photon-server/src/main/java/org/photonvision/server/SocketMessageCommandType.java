package org.photonvision.server;

public enum SocketMessageCommandType {
    SMCT_DELETECURRENTPIPELINE("deleteCurrentPipeline"),
    SMCT_SAVE("save");

    public final String entryValue;

    SocketMessageCommandType(String entryValue) {
        this.entryValue = entryValue;
    }

    public static SocketMessageCommandType fromEntryKey(String entryValue) {
        if(entryValue.equalsIgnoreCase(SMCT_SAVE.entryValue)) return SMCT_SAVE;
        else return SMCT_DELETECURRENTPIPELINE;
    }
}

package org.photonvision.common.scripting;

public enum ScriptEventType {
    kProgramInit("Program Init"),
    kProgramExit("Program Exit"),
    kNTConnected("NT Connected"),
    kLEDOn("LED On"),
    kLEDOff("LED Off"),
    kEnterDriverMode("Enter Driver Mode"),
    kExitDriverMode("Exit Driver Mode"),
    kFoundTarget("Found Target"),
    kFoundMultipleTarget("Found Multiple Target"),
    kLostTarget("Lost Target"),
    kPipelineLag("Pipeline Lag");

    public final String value;

    ScriptEventType(String value) {
        this.value = value;
    }
}

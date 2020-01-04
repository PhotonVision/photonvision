package com.chameleonvision.scripting;

public enum ScriptCommandType {
    kDefault(""),
    kBashScript("bash"),
    kPythonScript("python"),
    kPython3Script("python3");

    public final String value;

    ScriptCommandType(String value) {
        this.value = value;
    }
}

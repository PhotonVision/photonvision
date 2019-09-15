package com.chameleonvision;

public class NoCameraException extends Exception {
    public NoCameraException(){
        super("No Camera Connected");
    }
}

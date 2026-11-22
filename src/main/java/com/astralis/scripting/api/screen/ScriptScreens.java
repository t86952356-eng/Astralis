package com.astralis.scripting.api.screen;

public interface ScriptScreens {
    boolean isOpen();

    void close();

    void open(String title, ScriptScreen screen);

    interface Managed {}
}

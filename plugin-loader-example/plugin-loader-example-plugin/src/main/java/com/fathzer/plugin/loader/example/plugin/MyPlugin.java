package com.fathzer.plugin.loader.example.plugin;

import com.fathzer.plugin.loader.example.api.AppPlugin;

public class MyPlugin implements AppPlugin {

    @Override
    public String getGreeting() {
        return "Hello, I'm a plugin";
    }
}
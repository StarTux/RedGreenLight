package com.cavetale.redgreenlight;

public final class Tag {
    protected boolean started;
    protected String world = "";
    protected Light light = Light.RED;
    protected int cooldown = 0;
    protected int ticks = 0;
}

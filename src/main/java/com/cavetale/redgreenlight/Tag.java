package com.cavetale.redgreenlight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Tag {
    protected boolean started;
    protected String world = "";
    protected Light light = Light.RED;
    protected int totalCooldown = 1;
    protected int cooldown = 0;
    protected int ticks = 0;
    protected boolean event;
    protected Map<UUID, Integer> completions = new HashMap<>();
    protected Set<UUID> playing = new HashSet<>();

    public int getCompletions(UUID uuid) {
        return completions.getOrDefault(uuid, 0);
    }

    public void addCompletions(UUID uuid, int amount) {
        completions.put(uuid, Math.max(0, getCompletions(uuid) + amount));
    }
}

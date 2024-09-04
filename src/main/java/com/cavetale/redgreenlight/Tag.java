package com.cavetale.redgreenlight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Tag {
    boolean started;
    String world = "";
    Light light = Light.RED;
    int totalCooldown = 1;
    int cooldown = 0;
    int ticks = 0;
    boolean event;
    final Map<UUID, Integer> completions = new HashMap<>();
    final Map<UUID, Integer> scores = new HashMap<>();
    final Set<UUID> playing = new HashSet<>();
    final Map<UUID, Integer> checkpoints = new HashMap<>();

    public int getCompletions(UUID uuid) {
        return completions.getOrDefault(uuid, 0);
    }

    public void addCompletions(UUID uuid, int amount) {
        completions.put(uuid, Math.max(0, getCompletions(uuid) + amount));
    }

    public int getScores(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public void addScores(UUID uuid, int amount) {
        scores.put(uuid, Math.max(0, getScores(uuid) + amount));
    }
}

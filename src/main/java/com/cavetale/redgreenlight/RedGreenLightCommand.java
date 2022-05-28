package com.cavetale.redgreenlight;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class RedGreenLightCommand implements TabExecutor {
    private final RedGreenLightPlugin plugin;

    public void enable() {
        plugin.getCommand("redgreenlight").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "load": {
            plugin.load();
            sender.sendMessage("RedGreenLight loaded");
            return true;
        }
        case "setworld": {
            if (args.length != 2) return false;
            plugin.tag.world = args[1];
            plugin.saveTag();
            sender.sendMessage("World set to " + plugin.tag.world);
            return true;
        }
        case "start": {
            plugin.startTicking();
            plugin.tag.started = true;
            plugin.tag.ticks = 0;
            plugin.tag.light = Light.RED;
            plugin.tag.cooldown = 200;
            plugin.saveTag();
            sender.sendMessage("RedGreenLight started");
            return true;
        }
        case "stop": {
            plugin.stopTicking();
            plugin.tag.started = false;
            plugin.saveTag();
            plugin.cleanUp();
            plugin.tag.playing.clear();
            sender.sendMessage("RedGreenLight stopped");
            return true;
        }
        case "event": {
            if (args.length > 2) return false;
            if (args.length == 2) {
                try {
                    plugin.tag.event = Boolean.parseBoolean(args[1]);
                } catch (IllegalArgumentException iae) {
                    return false;
                }
                plugin.saveTag();
            }
            sender.sendMessage("Event mode: " + plugin.tag.event);
            return true;
        }
        case "reset": {
            plugin.tag.completions.clear();
            plugin.saveTag();
            plugin.computeHighscore();
            sender.sendMessage(text("Completions were reset!", AQUA));
            return true;
        }
        case "add": {
            if (args.length != 3) return false;
            Player target = Bukkit.getPlayerExact(args[1]);
            int value = Integer.parseInt(args[2]);
            UUID uuid = target.getUniqueId();
            plugin.tag.addCompletions(uuid, value);
            plugin.saveTag();
            plugin.computeHighscore();
            sender.sendMessage(text("Score of " + target.getName() + " is now " + plugin.tag.getCompletions(uuid), AQUA));
            return true;
        }
        case "reward": {
            int count = plugin.rewardHighscore();
            sender.sendMessage(text("Rewarded " + count + " highscores", AQUA));
            return true;
        }
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return List.of("load", "start", "stop", "setworld", "event", "reset", "reward", "add", "reward");
    }
}

package com.cavetale.redgreenlight;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class RedGreenLightCommand implements TabExecutor {
    private final RedGreenLightPlugin plugin;

    public void enable() {
        plugin.getCommand("redgreenlight").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        Player player = (Player) sender;
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
            sender.sendMessage("RedGreenLight stopped");
            return true;
        }
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return List.of("load", "start", "stop", "setworld");
    }
}

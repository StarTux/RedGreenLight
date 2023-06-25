package com.cavetale.redgreenlight;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.playercache.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.World;
import static java.util.stream.Collectors.toList;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class RedGreenLightCommand extends AbstractCommand<RedGreenLightPlugin> {
    protected RedGreenLightCommand(final RedGreenLightPlugin plugin) {
        super(plugin, "redgreenlightadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("load").denyTabCompletion()
            .description("Reload the state")
            .senderCaller(sender -> {
                    plugin.load();
                    sender.sendMessage("RedGreenLight loaded");
                });

        rootNode.addChild("setworld").arguments("<world>")
            .description("Set the game world")
            .completers(CommandArgCompleter.supplyList(() -> Bukkit.getWorlds().stream()
                                                       .map(World::getName)
                                                       .collect(toList())))
            .senderCaller((sender, args) -> {
                    if (args.length != 1) return false;
                    plugin.tag.world = args[0];
                    plugin.saveTag();
                    sender.sendMessage("World set to " + plugin.tag.world);
                    return true;
                });

        rootNode.addChild("start").denyTabCompletion()
            .description("Start the game")
            .senderCaller(sender -> {
                    plugin.startTicking();
                    plugin.tag.started = true;
                    plugin.tag.ticks = 0;
                    plugin.tag.light = Light.RED;
                    plugin.tag.cooldown = 200;
                    plugin.saveTag();
                    sender.sendMessage("RedGreenLight started");
                });

        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop the game")
            .senderCaller(sender -> {
                    plugin.stopTicking();
                    plugin.tag.started = false;
                    plugin.saveTag();
                    plugin.cleanUp();
                    plugin.tag.playing.clear();
                    sender.sendMessage("RedGreenLight stopped");
                });

        rootNode.addChild("event").arguments("true|false")
            .description("Set event state")
            .completers(CommandArgCompleter.BOOLEAN)
            .senderCaller((sender, args) -> {
                    if (args.length > 1) return false;
                    if (args.length == 1) {
                        plugin.tag.event = CommandArgCompleter.requireBoolean(args[0]);
                        plugin.saveTag();
                    }
                    sender.sendMessage("Event mode: " + plugin.tag.event);
                    return true;
                });

        rootNode.addChild("reset").denyTabCompletion()
            .description("Reset player progress")
            .senderCaller(sender -> {
                    plugin.tag.completions.clear();
                    plugin.tag.scores.clear();
                    plugin.tag.playing.clear();
                    plugin.tag.checkpoints.clear();
                    plugin.saveTag();
                    plugin.computeHighscore();
                    sender.sendMessage(text("Completions were reset!", AQUA));
                });

        rootNode.addChild("add").arguments("<player> <score>")
            .description("Add player highscore")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller((sender, args) -> {
                    if (args.length != 2) return false;
                    PlayerCache target = PlayerCache.require(args[0]);
                    int value = CommandArgCompleter.requireInt(args[1], i -> i != 0);
                    plugin.tag.addScores(target.uuid, value);
                    plugin.saveTag();
                    plugin.computeHighscore();
                    sender.sendMessage(text("Score of " + target.name + " is now "
                                            + plugin.tag.getCompletions(target.uuid), AQUA));
                    return true;
                });

        rootNode.addChild("reward").denyTabCompletion()
            .description("Give highscore rewards")
            .senderCaller(sender -> {
                    int count = plugin.rewardHighscore();
                    sender.sendMessage(text("Rewarded " + count + " highscores", AQUA));
                });
    }
}

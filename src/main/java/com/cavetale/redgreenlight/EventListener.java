package com.cavetale.redgreenlight;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.mytems.Mytems;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final RedGreenLightPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean playerInGame(Player player, Location loc) {
        if (!plugin.tag.started) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return false;
        }
        if (!plugin.inGameArea(loc)) return false;
        return true;
    }

    @EventHandler
    void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.tag.started) return;
        if (plugin.teleporting) return;
        Player player = event.getPlayer();
        Location loc = event.getTo();
        if (!playerInGame(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        Location loc = event.getTo();
        if (!playerInGame(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        if (player.isGliding() || player.isFlying()) {
            player.sendMessage(text("No flying!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        if (player.getVehicle() != null) {
            player.sendMessage(text("No riding!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        for (PotionEffectType pot : PotionEffectType.values()) {
            if (player.hasPotionEffect(pot)) {
                player.sendMessage(text("No potion effects!", DARK_RED));
                plugin.teleportToSpawn(player);
                return;
            }
        }
        if (!isEmpty(player.getEquipment().getHelmet())) {
            player.sendMessage(text("No armor!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        if (!isEmpty(player.getEquipment().getChestplate())) {
            player.sendMessage(text("No armor!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        if (!isEmpty(player.getEquipment().getLeggings())) {
            player.sendMessage(text("No armor!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        if (!isEmpty(player.getEquipment().getBoots())) {
            player.sendMessage(text("No armor!", DARK_RED));
            plugin.teleportToSpawn(player);
            return;
        }
        if (plugin.tag.light == Light.RED) {
            if (plugin.inGoalArea(loc)) return;
            plugin.teleportToSpawn(player);
            player.sendMessage(text("You moved! Back to the start!", DARK_RED));
        } else {
            if (plugin.inGoalArea(loc)) {
                plugin.teleportToSpawn(player);
                for (Player other : plugin.getPlayers()) {
                    other.sendMessage(text(player.getName() + " crossed the finish line!", GREEN));
                }
                player.showTitle(title(text("Winner!", GREEN),
                                       text("You win the game!", GREEN)));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.MASTER, 0.5f, 2.0f);
                if (plugin.tag.event) {
                    List<String> titles = List.of("GreenLit",
                                                  "RedGreenLight",
                                                  "TrafficLight");
                    String cmd = "titles unlockset " + player.getName() + " " + String.join(" ", titles);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    Mytems.KITTY_COIN.giveItemStack(player, 1);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                }
            }
        }
    }

    private void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!playerInGame(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        if (plugin.inGoalArea(loc)) return;
        plugin.teleportToSpawn(player);
        player.sendMessage(text("You moved! Back to the start!", DARK_RED));
    }

    protected void onTick() {
        World w = plugin.getWorld();
        List<Player> players = plugin.getPlayers();
        if (w == null) return;
        if (plugin.tag.cooldown > 1) {
            plugin.tag.cooldown -= 1;
        } else {
            plugin.tag.cooldown = 0;
            if (plugin.tag.light == Light.GREEN) {
                plugin.tag.light = Light.YELLOW;
                plugin.tag.cooldown = 20;
                Title title = title(Light.YELLOW.toComponent(),
                                    Component.empty(),
                                    times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
                for (Player player : players) {
                    player.showTitle(title);
                }
            } else if (plugin.tag.light == Light.YELLOW) {
                plugin.tag.light = Light.RED;
                plugin.tag.cooldown = 100;
                Title title = title(Light.RED.toComponent(),
                                    text("Stop Moving", DARK_RED),
                                    times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
                for (Player player : players) {
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.MASTER, 0.5f, 2.0f);
                }
            } else {
                plugin.tag.light = Light.GREEN;
                plugin.tag.cooldown = 40 + plugin.random.nextInt(160);
                Title title = title(Light.GREEN.toComponent(),
                                    text("Go!", GREEN),
                                    times(Duration.ZERO, Duration.ofMillis(1), Duration.ofSeconds(1)));
                for (Player player : players) {
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.MASTER, 0.5f, 2.0f);
                }
            }
        }
        for (Player player : players) {
            Location loc = player.getLocation();
            if (!playerInGame(player, loc)) continue;
            if (player.isGliding() || player.isFlying()) {
                player.sendMessage(text("No flying!", DARK_RED));
                plugin.teleportToSpawn(player);
            }
            if (player.getVehicle() != null && !plugin.inSpawnArea(loc)) {
                player.sendMessage(text("No riding!", DARK_RED));
                plugin.teleportToSpawn(player);
            }
        }
        if ((plugin.tag.ticks % 100) == 0) {
            for (Cuboid creeperArea : plugin.creeperAreas) {
                Vec3i vec = creeperArea.min;
                if (!w.isChunkLoaded(vec.x >> 4, vec.z >> 4)) continue;
                Creeper creeper = plugin.creeperMap.get(vec);
                if (creeper != null && !creeper.isDead()) continue;
                creeper = w.spawn(vec.toLocation(w).add(0.5, 1.0, 0.5), Creeper.class, c -> {
                        c.setPersistent(false);
                        c.setRemoveWhenFarAway(false);
                        c.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
                    });
                plugin.creeperMap.put(vec, creeper);
            }
        }
        if ((plugin.tag.ticks % 20) == 0) {
            for (Cuboid dispenserArea : plugin.dispenserAreas) {
                Vec3i vec = dispenserArea.min;
                if (!w.isChunkLoaded(vec.x >> 4, vec.z >> 4)) continue;
                Block block = vec.toBlock(w);
                BlockData bdata = block.getBlockData();
                if (!(bdata instanceof Dispenser)) continue;
                Dispenser dispenser = (Dispenser) bdata;
                BlockFace facing = dispenser.getFacing();
                Location location = block.getLocation().add(facing.getModX() + 0.5,
                                                            facing.getModY() + 0.5,
                                                            facing.getModZ() + 0.5);
                Vector velocity = facing.getDirection().multiply(2.0);
                Arrow arrow = w.spawn(location, Arrow.class, a -> {
                        a.setVelocity(velocity);
                        a.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    });
                w.playSound(location, Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1.0f, 1.0f);
                if (arrow != null && !arrow.isDead()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> arrow.remove(), 20L);
                }
            }
        }
        final int campfireInterval = 40;
        if ((plugin.tag.ticks % campfireInterval) == 0) {
            int interval = plugin.tag.ticks / campfireInterval;
            for (Cuboid campfireArea : plugin.campfireAreas) {
                int litz = interval % 4;
                for (Vec3i vec : campfireArea.enumerate()) {
                    Block block = vec.toBlock(w);
                    BlockData bdata = block.getBlockData();
                    if (!(bdata instanceof Campfire)) continue;
                    Campfire campfire = (Campfire) bdata;
                    int cinterval = vec.z % 4;
                    if (cinterval < 0) cinterval += 4;
                    boolean shouldBeLit = cinterval == litz;
                    campfire.setLit(shouldBeLit);
                    block.setBlockData(campfire);
                }
            }
        }
        plugin.tag.ticks += 1;
    }

    @EventHandler
    void onPluginPlayer(PluginPlayerEvent event) {
        if (!plugin.tag.started) return;
        if (event.getName() == PluginPlayerEvent.Name.START_FLYING && event.isCancellable()) {
            if (playerInGame(event.getPlayer(), event.getPlayer().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!plugin.tag.started) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.isGliding()) return;
        Player player = (Player) event.getEntity();
        Location loc = player.getLocation();
        if (!playerInGame(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No flying!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!plugin.tag.started) return;
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!playerInGame(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No flying!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.tag.started) return;
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) return;
        Player player = (Player) projectile.getShooter();
        Location loc = player.getLocation();
        if (!playerInGame(player, loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No projectiles!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    void onPlayerRiptide(PlayerRiptideEvent event) {
        if (!plugin.tag.started) return;
        Player player = (Player) event.getPlayer();
        Location loc = player.getLocation();
        if (!playerInGame(player, loc)) return;
        player.sendMessage(text("No riptide!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        if (!plugin.inGameArea(player.getLocation())) return;
        event.add(plugin, Priority.HIGHEST,
                  List.of(Component.join(noSeparators(),
                                         Mytems.TRAFFIC_LIGHT.component,
                                         text(" Light ", AQUA),
                                         plugin.tag.light.toComponent().decorate(BOLD))));
    }
}

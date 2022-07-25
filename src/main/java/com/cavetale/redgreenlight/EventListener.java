package com.cavetale.redgreenlight;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.WardrobeItem;
import com.cavetale.tutor.event.PetSpawnEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
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
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
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

    /**
     * Determine if a player is ready to play right now.
     */
    private boolean isReadyToPlay(Player player, Location loc) {
        if (!plugin.tag.started) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return false;
        }
        if (!plugin.inGameArea(loc)) return false;
        return true;
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.tag.started) return;
        if (plugin.teleporting) return;
        Player player = event.getPlayer();
        Location loc = event.getTo();
        if (!isReadyToPlay(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
    }

    private boolean isEmpty(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        Mytems mytems = Mytems.forItem(item);
        if (mytems != null) {
            return mytems.getMytem() instanceof WardrobeItem;
        }
        return false;
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.tag.started) return;
        if (plugin.teleporting) return;
        Player player = event.getPlayer();
        Location loc = event.getTo();
        if (!isReadyToPlay(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        if (!plugin.tag.playing.contains(player.getUniqueId())) {
            // We catch them on the next tick
            return;
        }
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
            if (plugin.isAtCheckpoint(player)) return;
            plugin.teleportToCheckpoint(player);
            player.sendMessage(text("You moved! Back to your checkpoint!", DARK_RED));
        } else if (plugin.inGoalArea(loc)) {
            plugin.getLogger().info(player.getName() + " crossed the finish line");
            plugin.tag.playing.remove(player.getUniqueId());
            plugin.tag.checkpoints.remove(player.getUniqueId());
            for (Player other : plugin.getPresentPlayers()) {
                other.sendMessage(join(noSeparators(),
                                       newline(),
                                       text(player.getName() + " crossed the finish line!", GREEN),
                                       newline()));
            }
            player.showTitle(title(text("Winner!", GREEN),
                                   text("You win the game!", GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.MASTER, 0.5f, 2.0f);
            plugin.tag.addCompletions(player.getUniqueId(), 1);
            plugin.computeHighscore();
            plugin.saveTag();
            if (plugin.tag.event) {
                List<String> titles = List.of("GreenLit",
                                              "RedGreenLight",
                                              "TrafficLight");
                String cmd = "titles unlockset " + player.getName() + " " + String.join(" ", titles);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    @EventHandler
    private void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!plugin.tag.started) return;
        if (plugin.tag.light != Light.RED) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!isReadyToPlay(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        if (plugin.inGoalArea(loc)) return;
        if (plugin.isAtCheckpoint(player)) return;
        plugin.teleportToCheckpoint(player);
        player.sendMessage(text("You moved! Back to your checkpoint!", DARK_RED));
    }

    protected void onTick() {
        World w = plugin.getWorld();
        if (w == null) return;
        // Prune players who left
        for (UUID uuid : List.copyOf(plugin.tag.playing)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                plugin.tag.playing.remove(uuid);
                plugin.tag.checkpoints.remove(uuid);
            } else if (!plugin.isGameWorld(player.getWorld())) {
                plugin.tag.playing.remove(uuid);
                plugin.tag.checkpoints.remove(uuid);
            }
        }
        // Tick players
        List<Player> players = plugin.getPresentPlayers();
        if (plugin.tag.cooldown > 1) {
            plugin.tag.cooldown -= 1;
            if (plugin.tag.light == Light.YELLOW) {
                float prog = (float) plugin.tag.cooldown / (float) plugin.tag.totalCooldown;
                plugin.bossBar.progress(Math.max(0.0f, Math.min(1.0f, prog)));
            } else if (plugin.tag.light == Light.RED) {
                float prog = (float) plugin.tag.cooldown / (float) plugin.tag.totalCooldown;
                plugin.bossBar.progress(Math.max(0.0f, Math.min(1.0f, 1.0f - prog)));
            }
        } else {
            plugin.tag.cooldown = 0;
            if (plugin.tag.light == Light.GREEN) {
                plugin.tag.light = Light.YELLOW;
                plugin.tag.totalCooldown = 20;
                plugin.tag.cooldown = 20;
            } else if (plugin.tag.light == Light.YELLOW) {
                plugin.tag.light = Light.RED;
                plugin.tag.totalCooldown = 100;
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
                plugin.tag.totalCooldown = 40 + plugin.random.nextInt(160);
                plugin.tag.cooldown = plugin.tag.totalCooldown;
                Title title = title(Light.GREEN.toComponent(),
                                    text("Go!", GREEN),
                                    times(Duration.ZERO, Duration.ofMillis(1), Duration.ofSeconds(1)));
                for (Player player : players) {
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.MASTER, 0.5f, 2.0f);
                }
            }
            plugin.bossBar.color(plugin.tag.light.bossBarColor);
            plugin.bossBar.name(join(noSeparators(), Mytems.TRAFFIC_LIGHT.component, plugin.tag.light.toComponent()));
            if (plugin.tag.light == Light.RED) {
                plugin.bossBar.flags(Set.of(BossBar.Flag.CREATE_WORLD_FOG, BossBar.Flag.DARKEN_SCREEN));
                plugin.bossBar.progress(0.0f);
            } else {
                plugin.bossBar.flags(Set.of());
                plugin.bossBar.progress(1.0f);
            }
        }
        for (Player player : players) {
            Location loc = player.getLocation();
            if (!isReadyToPlay(player, loc)) {
                continue;
            }
            if (!plugin.tag.playing.contains(player.getUniqueId())) {
                if (plugin.inSpawnArea(player.getLocation())) {
                    plugin.addPlaying(player);
                } else if (!plugin.inGoalArea(player.getLocation())) {
                    plugin.teleportToSpawn(player);
                }
                continue;
            }
            if (player.isGliding() || player.isFlying()) {
                player.sendMessage(text("No flying!", DARK_RED));
                plugin.teleportToSpawn(player);
            }
            if (player.getVehicle() != null && !plugin.inSpawnArea(loc)) {
                player.sendMessage(text("No riding!", DARK_RED));
                plugin.teleportToSpawn(player);
            }
        }
        if ((plugin.tag.ticks % 60) == 0) {
            for (Cuboid creeperArea : plugin.creeperAreas) {
                Vec3i vec = creeperArea.getMin();
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
            for (Cuboid snowmanArea : plugin.snowmanAreas) {
                Vec3i vec = snowmanArea.getMin();
                if (!w.isChunkLoaded(vec.x >> 4, vec.z >> 4)) continue;
                Snowman snowman = plugin.snowmanMap.get(vec);
                if (snowman != null && !snowman.isDead()) {
                    Location sloc = snowman.getLocation();
                    List<Player> targets = new ArrayList<>();
                    for (Player player : players) {
                        if (!plugin.tag.playing.contains(player.getUniqueId())) continue;
                        Location ploc = player.getLocation();
                        if (!ploc.getWorld().equals(sloc.getWorld())) continue;
                        if (ploc.distanceSquared(sloc) > 100.0) continue;
                        if (!snowman.hasLineOfSight(player)) continue;
                        targets.add(player);
                    }
                    if (targets.isEmpty()) continue;
                    Player target = targets.get(plugin.random.nextInt(targets.size()));
                    Vector v = target.getEyeLocation().subtract(snowman.getEyeLocation()).toVector().normalize();
                    sloc.setDirection(v);
                    snowman.teleport(sloc);
                    snowman.launchProjectile(Arrow.class, v.multiply(2.0));
                    continue;
                }
                snowman = w.spawn(vec.toLocation(w).add(0.5, 1.0, 0.5), Snowman.class, c -> {
                        c.setPersistent(false);
                        c.setRemoveWhenFarAway(false);
                        c.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
                    });
                plugin.snowmanMap.put(vec, snowman);
            }
        }
        if ((plugin.tag.ticks % 20) == 0) {
            for (Cuboid dispenserArea : plugin.dispenserAreas) {
                Vec3i vec = dispenserArea.getMin();
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
                        a.setPersistent(false);
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
    private void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!plugin.tag.started) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.isGliding()) return;
        Player player = (Player) event.getEntity();
        Location loc = player.getLocation();
        if (!isReadyToPlay(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No flying!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    private void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!plugin.tag.started) return;
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!isReadyToPlay(player, loc)) return;
        if (plugin.inSpawnArea(loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No flying!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.tag.started) return;
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) return;
        Player player = (Player) projectile.getShooter();
        Location loc = player.getLocation();
        if (!isReadyToPlay(player, loc)) return;
        event.setCancelled(true);
        player.sendMessage(text("No projectiles!", DARK_RED));
        plugin.teleportToSpawn(player);
    }

    @EventHandler
    private void onPlayerRiptide(PlayerRiptideEvent event) {
        if (!plugin.tag.started) return;
        Player player = (Player) event.getPlayer();
        Location loc = player.getLocation();
        if (!isReadyToPlay(player, loc)) return;
        player.sendMessage(text("No riptide!", DARK_RED));
        plugin.teleportToSpawn(player);
        Bukkit.getScheduler().runTask(plugin, () -> player.setVelocity(new Vector().zero()));
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        if (!plugin.isGameWorld(player.getWorld())) return;
        List<Component> lines = new ArrayList<>();
        lines.add(plugin.TITLE);
        lines.add(join(noSeparators(), text(tiny("light "), GRAY), plugin.tag.light.toComponent().decorate(BOLD)));
        lines.add(join(noSeparators(), text(tiny("lives "), GRAY), Mytems.HEART, text(plugin.tag.lives.getOrDefault(player.getUniqueId(), 0), RED)));
        lines.add(join(noSeparators(), text(tiny("players "), GRAY), text(plugin.tag.playing.size(), GREEN)));
        int wins = plugin.tag.getCompletions(player.getUniqueId());
        if (wins > 0) {
            lines.add(join(noSeparators(), text(tiny("wins "), GRAY), text(wins, GOLD)));
        }
        if (plugin.tag.event) {
            lines.addAll(Highscore.sidebar(plugin.highscore));
        }
        event.sidebar(PlayerHudPriority.HIGHEST, lines);
        event.bossbar(PlayerHudPriority.HIGH,
                      plugin.bossBar.name(), plugin.bossBar.color(), plugin.bossBar.overlay(),
                      plugin.bossBar.progress());
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        if (!plugin.tag.playing.contains(player.getUniqueId())) return;
        if (!plugin.isGameWorld(player.getWorld())) return;
        event.setRespawnLocation(plugin.randomSpawnLocation());
    }

    @EventHandler(ignoreCancelled = true)
    private void onPetSpawn(PetSpawnEvent event) {
        if (plugin.inGameArea(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK: case LEFT_CLICK_BLOCK: break;
        default: return;
        }
        if (!plugin.tag.started) return;
        Player player = event.getPlayer();
        if (!plugin.tag.playing.contains(player.getUniqueId())) return;
        if (!plugin.inGameArea(player.getLocation())) return;
        if (!event.hasBlock()) return;
        if (event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) return;
        event.setCancelled(true);
        Vec3i vec = Vec3i.of(event.getClickedBlock());
        if (vec.equals(plugin.tag.checkpoints.get(player.getUniqueId()))) return;
        plugin.tag.checkpoints.put(player.getUniqueId(), vec);
        plugin.tag.lives.put(player.getUniqueId(), 10);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.MASTER, 1.0f, 2.0f);
        player.sendMessage(text("Checkpoint set!", GREEN));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.tag.started) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.isGameWorld(player.getWorld())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        plugin.getLogger().info(player.getName() + " void damage");
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.teleportToCheckpoint(player));
    }
}

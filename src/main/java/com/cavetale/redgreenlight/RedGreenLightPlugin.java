package com.cavetale.redgreenlight;

import com.cavetale.area.struct.AreasFile;
import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;

public final class RedGreenLightPlugin extends JavaPlugin {
    protected RedGreenLightCommand redgreenlightCommand = new RedGreenLightCommand(this);
    protected EventListener eventListener = new EventListener(this);
    protected Tag tag;
    protected Random random = new Random();
    protected List<Cuboid> gameAreas;
    protected List<Cuboid> spawnAreas;
    protected List<Cuboid> goalAreas;
    protected List<Cuboid> warpAreas;
    protected List<Cuboid> creeperAreas;
    protected List<Cuboid> dispenserAreas;
    protected List<Cuboid> campfireAreas;
    protected BukkitTask task;
    protected boolean teleporting;
    protected final Map<Vec3i, Creeper> creeperMap = new HashMap<>();
    protected List<Highscore> highscore = List.of();
    public static final Component TITLE = join(noSeparators(),
                                               Mytems.TRAFFIC_LIGHT.component,
                                               text(tiny("Red Light "), color(0xFF0000)),
                                               Mytems.TRAFFIC_LIGHT.component,
                                               text(tiny("Green Light"), color(0x00FF00)));
    protected final BossBar bossBar = BossBar.bossBar(TITLE, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    @Override
    public void onEnable() {
        redgreenlightCommand.enable();
        eventListener.enable();
        load();
    }

    @Override
    public void onDisable() {
        cleanUp();
        saveTag();
    }

    protected void load() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
        loadTag();
        loadAreas();
        for (UUID uuid : List.copyOf(tag.playing)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !inGameArea(player.getLocation())) {
                tag.playing.remove(uuid);
            } else {
                player.showBossBar(bossBar);
            }
        }
    }

    protected void loadTag() {
        File file = new File(getDataFolder(), "save.json");
        tag = Json.load(file, Tag.class, Tag::new);
        if (tag.started) {
            startTicking();
        } else {
            stopTicking();
        }
        computeHighscore();
    }

    protected void cleanUp() {
        for (Creeper creeper : creeperMap.values()) {
            creeper.remove();
        }
        creeperMap.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    protected void saveTag() {
        File file = new File(getDataFolder(), "save.json");
        Json.save(file, tag);
    }

    protected void loadAreas() {
        gameAreas = List.of();
        spawnAreas = List.of();
        goalAreas = List.of();
        warpAreas = List.of();
        creeperAreas = List.of();
        dispenserAreas = List.of();
        campfireAreas = List.of();
        World w = getWorld();
        if (w == null) {
            getLogger().warning("World not found: " + tag.world);
            return;
        }
        AreasFile areasFile = AreasFile.load(w, "RedGreenLight");
        if (areasFile == null) {
            getLogger().warning("Areas not found: " + tag.world + "/RedGreenLight");
            return;
        }
        this.gameAreas = areasFile.find("area");
        this.spawnAreas = areasFile.find("spawn");
        this.goalAreas = areasFile.find("goal");
        this.warpAreas = areasFile.find("warp");
        this.creeperAreas = areasFile.find("creeper");
        this.dispenserAreas = areasFile.find("dispenser");
        this.campfireAreas = areasFile.find("campfire");
    }

    public boolean inArea(List<Cuboid> areaList, Location location) {
        if (!tag.world.equals(location.getWorld().getName())) return false;
        for (Cuboid area : areaList) {
            if (area.contains(location)) return true;
        }
        return false;
    }

    public boolean inGameArea(Location location) {
        return inArea(gameAreas, location);
    }

    public boolean inSpawnArea(Location location) {
        return inArea(spawnAreas, location);
    }

    public boolean inGoalArea(Location location) {
        return inArea(goalAreas, location);
    }

    public List<Player> getPresentPlayers() {
        World world = Bukkit.getWorld(tag.world);
        if (world == null) return List.of();
        List<Player> list = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (inGameArea(player.getLocation())) {
                list.add(player);
            }
        }
        return list;
    }

    protected World getWorld() {
        return Bukkit.getWorld(tag.world);
    }

    public Location randomSpawnLocation() {
        Set<Vec3i> options = new HashSet<>();
        for (Cuboid area : warpAreas) {
            options.addAll(area.enumerate());
        }
        if (options.isEmpty()) throw new IllegalStateException("Warp is empty");
        List<Vec3i> list = new ArrayList<>(options);
        Vec3i center = list.get(random.nextInt(list.size()));
        return center.toBlock(getWorld()).getLocation().add(0.5, 0.0, 0.5);
    }

    /**
     * Warp to spawn and mark as playing.
     */
    protected void teleportToSpawn(Player player) {
        if (!inSpawnArea(player.getLocation())) {
            Location location = randomSpawnLocation();
            Location ploc = player.getLocation();
            location.setPitch(ploc.getPitch());
            location.setYaw(ploc.getYaw());
            teleporting = true;
            player.teleport(location, TeleportCause.PLUGIN);
            teleporting = false;
            player.setFallDistance(0);
        }
        if (!tag.playing.contains(player.getUniqueId())) {
            tag.playing.add(player.getUniqueId());
            if (tag.event) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
            }
            player.showBossBar(bossBar);
        }
    }

    protected void startTicking() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(this, eventListener::onTick, 0L, 0L);
    }

    protected void stopTicking() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    protected void computeHighscore() {
        highscore = Highscore.of(tag.completions);
    }

    protected int rewardHighscore() {
        return Highscore.reward(tag.completions,
                                "red_light_green_light",
                                TrophyCategory.RED_GREEN_LIGHT,
                                TITLE,
                                hi -> ("You defeated the Red Light Green Light track "
                                       + hi.score + " time" + (hi.score == 1 ? "" : "s")));
    }
}

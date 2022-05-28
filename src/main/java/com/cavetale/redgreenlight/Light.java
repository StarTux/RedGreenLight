package com.cavetale.redgreenlight;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

@RequiredArgsConstructor
public enum Light {
    RED("Red", color(0xFF0000), BossBar.Color.RED),
    YELLOW("Yellow", color(0xFFFF00), BossBar.Color.YELLOW),
    GREEN("Green", color(0x00FF00), BossBar.Color.GREEN);

    public final String displayName;
    public final TextColor textColor;
    public final BossBar.Color bossBarColor;

    public Component toComponent() {
        return text(displayName, textColor);
    }
}

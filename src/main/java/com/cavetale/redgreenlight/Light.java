package com.cavetale.redgreenlight;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Light {
    RED(Component.text("Red", NamedTextColor.DARK_RED)),
    YELLOW(Component.text("Yellow", NamedTextColor.YELLOW)),
    GREEN(Component.text("Green", NamedTextColor.GREEN));

    public final Component component;

    public Component toComponent() {
        return component;
    }
}

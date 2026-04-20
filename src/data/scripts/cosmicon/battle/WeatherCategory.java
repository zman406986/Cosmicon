package data.scripts.cosmicon.battle;

import data.scripts.Strings;

import java.awt.Color;

public enum WeatherCategory {
    SAFEGUARD(new Color(100, 180, 255)),
    ATTACK(new Color(255, 100, 100)),
    HELP(new Color(100, 255, 150)),
    REVERSAL(new Color(180, 100, 255));

    private final Color color;

    WeatherCategory(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public String getDescription() {
        return Strings.get("weather_category." + name().toLowerCase() + "_desc");
    }
}
package data.scripts.cosmicon.battle;

import data.scripts.Strings;

import java.awt.Color;

public enum WeatherType {
    FROST(WeatherCategory.SAFEGUARD, 2),
    FROG_RAIN(WeatherCategory.HELP, 2),
    FINE_SNOW(WeatherCategory.SAFEGUARD, 2),
    FISH_RAIN(WeatherCategory.HELP, 2),
    PARHELION(WeatherCategory.ATTACK, 2),
    CYCLONIC_SWARM(WeatherCategory.ATTACK, 2),
    SLEET(WeatherCategory.HELP, 2),
    SOLAR_ECLIPSE(WeatherCategory.ATTACK, 2),
    BLIZZARD(WeatherCategory.SAFEGUARD, 4),
    SCORCHING_SUN(WeatherCategory.ATTACK, 4),
    ACID_RAIN(WeatherCategory.HELP, 4),
    HIGH_TEMPERATURE(WeatherCategory.ATTACK, 4),
    STORM(WeatherCategory.REVERSAL, 4),
    MODERATE_SNOW(WeatherCategory.SAFEGUARD, 4),
    HEAVY_SNOW(WeatherCategory.SAFEGUARD, 4),
    DUST(WeatherCategory.ATTACK, 4),
    SEA_OF_CLOUDS(WeatherCategory.HELP, 6),
    RAINBOW(WeatherCategory.ATTACK, 6),
    DROUGHT(WeatherCategory.ATTACK, 6),
    LUNISOLAR_LUMINANCE(WeatherCategory.REVERSAL, 6),
    CREPUSCULAR_RAYS(WeatherCategory.ATTACK, 6),
    TEMPORAL_STORM(WeatherCategory.REVERSAL, 6),
    SUNSHOWER(WeatherCategory.ATTACK, 6),
    SUNNY(WeatherCategory.ATTACK, 8),
    DRY_THUNDERSTORM(WeatherCategory.REVERSAL, 8),
    TOXIC_FOG(WeatherCategory.REVERSAL, 8),
    THE_DECISIVE_MOMENT(WeatherCategory.ATTACK, -1),
    VENOCLLOUD(WeatherCategory.ATTACK, -2),
    DRIZZLE(WeatherCategory.HELP, -2);

    private final WeatherCategory category;
    private final int defaultTurn;
    private final Color color;

    WeatherType(WeatherCategory category, int defaultTurn) {
        this.category = category;
        this.defaultTurn = defaultTurn;
        this.color = category.getColor();
    }

    public WeatherCategory getCategory() {
        return category;
    }

    public int getDefaultTurn() {
        return defaultTurn;
    }

    public String getDescription() {
        return Strings.get("weather." + name().toLowerCase() + ".desc");
    }

    public Color getColor() {
        return color;
    }
}
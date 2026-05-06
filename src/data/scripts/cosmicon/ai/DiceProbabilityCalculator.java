package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;

public final class DiceProbabilityCalculator {

    private DiceProbabilityCalculator() {}

    public static float expectedValue(DiceType type) {
        return switch (type) {
            case BLUE_D4 -> 2.5f;
            case PURPLE_D6, PRISMATIC -> 3.5f;
            case ORANGE_D8 -> 4.5f;
            case RED_D12 -> 6.5f;
        };
    }
}

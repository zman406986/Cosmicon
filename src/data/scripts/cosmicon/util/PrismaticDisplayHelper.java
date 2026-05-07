package data.scripts.cosmicon.util;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticEffect;

import java.util.MissingResourceException;

public final class PrismaticDisplayHelper {
    private PrismaticDisplayHelper() {}

    public static String getDiceDisplayName(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            return Strings.get("prismatic.equip.none");
        }
        String key = "prismatic." + diceId + ".name";
        try {
            return Strings.get(key);
        } catch (MissingResourceException e) {
            return diceId;
        }
    }

    public static String getDiceDisplayName(PrismaticDiceType type) {
        if (type == null) {
            return Strings.get("prismatic.equip.none");
        }
        return getDiceDisplayName(type.getId());
    }

    public static String getEffectDescription(PrismaticEffect effect) {
        if (effect == null) return Strings.get("prismatic.equip.no_effect");
        return switch (effect.getType()) {
            case NONE -> Strings.get("prismatic.equip.no_effect");
            case DOUBLE_VALUE -> Strings.get("prismatic.equip.effect_double");
            case HEAL_HP -> Strings.get("prismatic.equip.effect_heal");
            case GAIN_PRISMATIC_USE -> Strings.get("prismatic.equip.effect_gain_use");
            case INSTANT_DAMAGE -> Strings.format("prismatic.equip.effect_instant_damage", effect.getInstantDamageAmount());
            case GRANT_STATUS -> {
                String statusKey = "status." + effect.getGrantedEffect().name().toLowerCase();
                try {
                    String statusName = Strings.get(statusKey);
                    yield Strings.format("prismatic.equip.effect_status", statusName);
                } catch (MissingResourceException e) {
                    yield Strings.format("prismatic.equip.effect_status", effect.getGrantedEffect().name());
                }
            }
        };
    }

    public static String getEffectDescription(PrismaticDiceType type) {
        if (type == null) {
            return Strings.get("prismatic.equip.no_effect");
        }
        String key = "prismatic." + type.getId() + ".description";
        try {
            return Strings.get(key);
        } catch (MissingResourceException e) {
            return getEffectDescription(type.getEffect());
        }
    }

    public static String getFaceValuesDisplay(PrismaticDiceType type, boolean useTrueVersion) {
        if (type == null) return "";
        int[] faces = type.getFaces(useTrueVersion);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < faces.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(faces[i]);
            if (type.isSpecialFace(i, useTrueVersion)) sb.append("*");
        }
        return sb.toString();
    }

    public static boolean hasDistinctDefaultFaces(PrismaticDiceType type) {
        if (type == null) return false;
        int[] def = type.getFaces(false);
        int[] tru = type.getFaces(true);
        if (def.length != tru.length) return true;
        for (int i = 0; i < def.length; i++) {
            if (def[i] != tru[i]) return true;
        }
        return false;
    }
}
package data.scripts.cosmicon.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

public final class CosmiconLogger {
    
    public static boolean DEBUG = true;
    
    private static final Logger LOG = Global.getLogger(CosmiconLogger.class);
    
    private CosmiconLogger() {}
    
    public static void info(String message) {
        LOG.info(message);
    }
    
    public static void info(String format, Object... args) {
        LOG.info(String.format(format, args));
    }
    
    public static void debug(String message) {
        if (DEBUG) {
            LOG.info("[DEBUG] " + message);
        }
    }
    
    public static void debug(String format, Object... args) {
        if (DEBUG) {
            LOG.info("[DEBUG] " + String.format(format, args));
        }
    }
    
    public static void warn(String message) {
        LOG.warn(message);
    }
    
    public static void warn(String format, Object... args) {
        LOG.warn(String.format(format, args));
    }
    
    public static void error(String message) {
        LOG.error(message);
    }
    
    public static void error(String format, Object... args) {
        LOG.error(String.format(format, args));
    }
    
    public static void error(String message, Throwable t) {
        LOG.error(message, t);
    }
    
    public static void battleStart(String playerChar, String enemyChar) {
        info("========== BATTLE START ==========");
        info("Player: %s vs Enemy: %s", playerChar, enemyChar);
        info("==================================");
    }
    
    public static void turnStart(int turn, boolean isPlayerTurn) {
        debug("--- Turn %d: %s ---", turn, isPlayerTurn ? "Player" : "Enemy");
    }
    
    public static void diceRoll(String character, int[] results) {
        StringBuilder sb = new StringBuilder();
        sb.append(character).append(" rolled: [");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(results[i]);
        }
        sb.append("]");
        debug(sb.toString());
    }
    
    public static void damageDealt(String attacker, String defender, int damage) {
        debug("%s dealt %d damage to %s", attacker, damage, defender);
    }
    
    public static void hpChange(String character, int oldHp, int newHp, int maxHp) {
        debug("%s HP: %d -> %d / %d", character, oldHp, newHp, maxHp);
    }
}
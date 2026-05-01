package data.scripts.cosmicon.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;

public final class CosmiconLogger {
    
    public static final boolean DEBUG = true;
    
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
    
    public static void hpChange(String character, int oldHp, int newHp, int maxHp) {
        debug("%s HP: %d -> %d / %d", character, oldHp, newHp, maxHp);
    }
}
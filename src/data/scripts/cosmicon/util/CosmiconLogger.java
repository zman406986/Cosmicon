package data.scripts.cosmicon.util;

import com.fs.starfarer.api.Global;
import data.scripts.CosmiconConfig;
import org.apache.log4j.Logger;

public final class CosmiconLogger {

    private static final Logger LOG = Global.getLogger(CosmiconLogger.class);

    public static boolean isDebugEnabled() {
        return CosmiconConfig.DEBUG_ENABLED;
    }

    public static boolean isVerboseEnabled() {
        return CosmiconConfig.VERBOSE_ENABLED;
    }

    public static boolean isInfoEnabled() {
        return LOG.isInfoEnabled();
    }

    private CosmiconLogger() {}
    
    public static void info(String message) {
        LOG.info(message);
    }
    
    public static void info(String format, Object... args) {
        if (LOG.isInfoEnabled()) {
            LOG.info(String.format(format, args));
        }
    }
    
    public static void debug(String message) {
        if (isDebugEnabled()) {
            LOG.info("[DEBUG] " + message);
        }
    }
    
    public static void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            LOG.info("[DEBUG] " + String.format(format, args));
        }
    }

    public static void verbose(String message) {
        if (isVerboseEnabled()) {
            LOG.info("[VERBOSE] " + message);
        }
    }

    public static void verbose(String format, Object... args) {
        if (isVerboseEnabled()) {
            LOG.info("[VERBOSE] " + String.format(format, args));
        }
    }

    public static void warn(String message) {
        LOG.warn(message);
    }
    
    public static void warn(String format, Object... args) {
        if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
            LOG.warn(String.format(format, args));
        }
    }
    
    public static void error(String message) {
        LOG.error(message);
    }
    
    public static void error(String format, Object... args) {
        if (LOG.isEnabledFor(org.apache.log4j.Level.ERROR)) {
            LOG.error(String.format(format, args));
        }
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
        verbose("%s HP: %d -> %d / %d", character, oldHp, newHp, maxHp);
    }
}
package data.console.commands;

import data.scripts.CosmiconConfig;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconConfigCmd implements BaseCommand {

    @FunctionalInterface
    private interface IntSetter { void set(int v); }

    private void setInt(String key, String value, IntSetter setter) {
        try {
            int v = Integer.parseInt(value);
            setter.set(v);
            Console.showMessage("Set " + key + " = " + v);
        } catch (NumberFormatException e) {
            Console.showMessage("Error: value must be an integer.");
        }
    }

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String trimmed = args.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("show")) {
            showConfig();
            return CommandResult.SUCCESS;
        }

        String[] parts = trimmed.split(" ", 3);
        if (parts.length >= 3 && "set".equalsIgnoreCase(parts[0])) {
            setConfig(parts[1].trim(), parts[2].trim());
        } else {
            Console.showMessage("Error: use 'cosmicon_config show' or 'cosmicon_config set <key> <value>'");
            return CommandResult.BAD_SYNTAX;
        }

        return CommandResult.SUCCESS;
    }

    private void showConfig() {
        Console.showMessage("=== Cosmicon Config ===");
        Console.showMessage("  cosmiconDiceEnabled: " + CosmiconConfig.COSMICON_ENABLED);
        Console.showMessage("  marketSizeMin: " + CosmiconConfig.MARKET_SIZE_MIN);
        Console.showMessage("  defaultHP: " + CosmiconConfig.DEFAULT_HP);
        Console.showMessage("  defaultRerolls: " + CosmiconConfig.DEFAULT_REROLLS);
        Console.showMessage("  debugEnabled: " + CosmiconConfig.DEBUG_ENABLED);
        Console.showMessage("  verboseEnabled: " + CosmiconConfig.VERBOSE_ENABLED);
        Console.showMessage("  rerollLogEnabled: " + CosmiconConfig.REROLL_LOG_ENABLED);
        Console.showMessage("");
        Console.showMessage("  AI_REVEAL_PER_DICE_DELAY: " + CosmiconConfig.AI_REVEAL_PER_DICE_DELAY);
        Console.showMessage("  AI_REROLL_PREVIEW_DELAY: " + CosmiconConfig.AI_REROLL_PREVIEW_DELAY);
        Console.showMessage("  AI_CONFIRM_DELAY: " + CosmiconConfig.AI_CONFIRM_DELAY);
        Console.showMessage("  DICE_PREVIEW_DELAY: " + CosmiconConfig.DICE_PREVIEW_DELAY);
        Console.showMessage("");
        Console.showMessage("Use 'cosmicon_config set <key> <value>' to change at runtime.");
        Console.showMessage("Changes do NOT persist to file and reset on game restart.");
    }

    private void setConfig(String key, String value) {
        switch (key.toLowerCase()) {
            case "cosmicondiceenabled" -> {
                CosmiconConfig.COSMICON_ENABLED = Boolean.parseBoolean(value);
                Console.showMessage("Set cosmiconDiceEnabled = " + CosmiconConfig.COSMICON_ENABLED);
            }
            case "marketsizemin" -> setInt("marketSizeMin", value, v -> CosmiconConfig.MARKET_SIZE_MIN = v);
            case "defaulthp" -> setInt("defaultHP", value, v -> CosmiconConfig.DEFAULT_HP = v);
            case "defaultrerolls" -> setInt("defaultRerolls", value, v -> CosmiconConfig.DEFAULT_REROLLS = v);
            case "debugenabled" -> {
                CosmiconConfig.DEBUG_ENABLED = Boolean.parseBoolean(value);
                Console.showMessage("Set debugEnabled = " + CosmiconConfig.DEBUG_ENABLED);
            }
            case "verboseenabled" -> {
                CosmiconConfig.VERBOSE_ENABLED = Boolean.parseBoolean(value);
                Console.showMessage("Set verboseEnabled = " + CosmiconConfig.VERBOSE_ENABLED);
            }
            case "rerolllogenabled" -> {
                CosmiconConfig.REROLL_LOG_ENABLED = Boolean.parseBoolean(value);
                Console.showMessage("Set rerollLogEnabled = " + CosmiconConfig.REROLL_LOG_ENABLED);
            }
            default -> Console.showMessage("Error: unknown config key '" + key
                + "'. Valid keys: cosmiconDiceEnabled, marketSizeMin, defaultHP, defaultRerolls, debugEnabled, verboseEnabled, rerollLogEnabled");
        }
    }
}
package data.console.commands;

import data.scripts.CosmiconConfig;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconConfigCmd implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
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
            case "marketsizemin" -> {
                try {
                    CosmiconConfig.MARKET_SIZE_MIN = Integer.parseInt(value);
                    Console.showMessage("Set marketSizeMin = " + CosmiconConfig.MARKET_SIZE_MIN);
                } catch (NumberFormatException e) {
                    Console.showMessage("Error: value must be an integer.");
                }
            }
            case "defaulthp" -> {
                try {
                    CosmiconConfig.DEFAULT_HP = Integer.parseInt(value);
                    Console.showMessage("Set defaultHP = " + CosmiconConfig.DEFAULT_HP);
                } catch (NumberFormatException e) {
                    Console.showMessage("Error: value must be an integer.");
                }
            }
            case "defaultrerolls" -> {
                try {
                    CosmiconConfig.DEFAULT_REROLLS = Integer.parseInt(value);
                    Console.showMessage("Set defaultRerolls = " + CosmiconConfig.DEFAULT_REROLLS);
                } catch (NumberFormatException e) {
                    Console.showMessage("Error: value must be an integer.");
                }
            }
            case "debugenabled" -> {
                CosmiconConfig.DEBUG_ENABLED = Boolean.parseBoolean(value);
                Console.showMessage("Set debugEnabled = " + CosmiconConfig.DEBUG_ENABLED);
            }
            default -> Console.showMessage("Error: unknown config key '" + key
                + "'. Valid keys: cosmiconDiceEnabled, marketSizeMin, defaultHP, defaultRerolls, debugEnabled");
        }
    }
}
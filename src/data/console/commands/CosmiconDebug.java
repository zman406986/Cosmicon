package data.console.commands;

import data.scripts.CosmiconConfig;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconDebug implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String trimmed = args.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            showStatus();
            return CommandResult.SUCCESS;
        }

        switch (trimmed) {
            case "off" -> {
                CosmiconConfig.DEBUG_ENABLED = false;
                CosmiconConfig.VERBOSE_ENABLED = false;
                CosmiconConfig.REROLL_LOG_ENABLED = false;
                Console.showMessage("Debug logging: OFF (all tiers disabled)");
            }
            case "debug" -> {
                CosmiconConfig.DEBUG_ENABLED = true;
                CosmiconConfig.VERBOSE_ENABLED = false;
                Console.showMessage("Debug logging: DEBUG (tier 2 enabled, verbose off)");
            }
            case "verbose" -> {
                CosmiconConfig.DEBUG_ENABLED = true;
                CosmiconConfig.VERBOSE_ENABLED = true;
                Console.showMessage("Debug logging: VERBOSE (all tiers enabled)");
            }
            case "reroll" -> {
                CosmiconConfig.REROLL_LOG_ENABLED = !CosmiconConfig.REROLL_LOG_ENABLED;
                Console.showMessage("AI Reroll logging: " + (CosmiconConfig.REROLL_LOG_ENABLED ? "ON" : "OFF"));
            }
            default -> {
                Console.showMessage("Usage: cosmicon_debug [off|debug|verbose|reroll]");
                Console.showMessage("  off     - Disable all debug logging");
                Console.showMessage("  debug   - Enable tier 2 (battle flow, damage, status effects)");
                Console.showMessage("  verbose - Enable tier 2 + tier 3 (AI internals, dice rest, animations)");
                Console.showMessage("  reroll  - Toggle AI reroll decision logging");
                return CommandResult.BAD_SYNTAX;
            }
        }

        return CommandResult.SUCCESS;
    }

    private void showStatus() {
        String level = "OFF";
        if (CosmiconConfig.DEBUG_ENABLED && CosmiconConfig.VERBOSE_ENABLED) {
            level = "VERBOSE";
        } else if (CosmiconConfig.DEBUG_ENABLED) {
            level = "DEBUG";
        }
        Console.showMessage("Debug logging: " + level);
        Console.showMessage("  debugEnabled: " + CosmiconConfig.DEBUG_ENABLED);
        Console.showMessage("  verboseEnabled: " + CosmiconConfig.VERBOSE_ENABLED);
        Console.showMessage("  rerollLogEnabled: " + CosmiconConfig.REROLL_LOG_ENABLED);
        Console.showMessage("Usage: cosmicon_debug [off|debug|verbose|reroll]");
    }
}

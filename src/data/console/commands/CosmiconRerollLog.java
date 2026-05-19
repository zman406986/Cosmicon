package data.console.commands;

import data.scripts.CosmiconConfig;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconRerollLog implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String trimmed = args.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            Console.showMessage("AI Reroll logging: " + (CosmiconConfig.REROLL_LOG_ENABLED ? "ON" : "OFF"));
            Console.showMessage("Usage: cosmicon_reroll_log [on|off|toggle]");
            return CommandResult.SUCCESS;
        }

        switch (trimmed) {
            case "on", "true", "1" -> {
                CosmiconConfig.REROLL_LOG_ENABLED = true;
                Console.showMessage("AI Reroll logging: ON");
            }
            case "off", "false", "0" -> {
                CosmiconConfig.REROLL_LOG_ENABLED = false;
                Console.showMessage("AI Reroll logging: OFF");
            }
            case "toggle" -> {
                CosmiconConfig.REROLL_LOG_ENABLED = !CosmiconConfig.REROLL_LOG_ENABLED;
                Console.showMessage("AI Reroll logging: " + (CosmiconConfig.REROLL_LOG_ENABLED ? "ON" : "OFF"));
            }
            default -> {
                Console.showMessage("Error: use on, off, or toggle.");
                return CommandResult.BAD_SYNTAX;
            }
        }

        return CommandResult.SUCCESS;
    }
}

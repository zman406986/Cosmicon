package data.console.commands;

import data.scripts.cosmicon.state.CosmiconStats;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconSkipTutorial implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CosmiconStats.forceCompleteTutorial();

        Console.showMessage("Tutorial skipped! Games played: " + CosmiconStats.getGamesPlayed());
        Console.showMessage("Unlocked: Chimera, Acheron, Repeater (non-true).");

        return CommandResult.SUCCESS;
    }
}
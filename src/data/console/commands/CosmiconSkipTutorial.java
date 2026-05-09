package data.console.commands;

import data.scripts.cosmicon.state.CosmiconStats;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconSkipTutorial implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CosmiconStats.forceCompleteTutorial();

        Console.showMessage("Tutorial skipped! All characters and prismatic dice unlocked.");
        Console.showMessage("Games played: " + CosmiconStats.getGamesPlayed());
        Console.showMessage("Characters unlocked: " + CosmiconStats.getUnlockedCharacters().size());
        Console.showMessage("Prismatic dice unlocked: " + CosmiconStats.getUnlockedPrismaticDice().size());

        return CommandResult.SUCCESS;
    }
}
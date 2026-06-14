package data.console.commands;

import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoReset implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String mode = args.isEmpty() ? "all" : args.trim().toLowerCase();

        if (!isValidMode(mode)) {
            Console.showMessage("Error: invalid mode '" + mode + "'. Valid modes: all, legend, battle, tournament");
            return CommandResult.BAD_SYNTAX;
        }

        boolean resetHunter = mode.equals("all") || mode.equals("legend");
        boolean resetBattle = mode.equals("all") || mode.equals("battle");
        boolean resetTournament = mode.equals("all") || mode.equals("tournament");

        if (resetHunter) {
            CosmiconEventState.setLegendLevel(0);
            Console.showMessage("Reset: Master Dicer Level");
        }

        if (resetBattle) {
            CosmiconEventState.clearCasinoBattleState();
            Console.showMessage("Reset: Casino battle state");
        }

        if (resetTournament) {
            CosmiconEventState.clearTournamentAll();
            CosmiconStats.setTournamentUnlocked(false);
            Console.showMessage("Reset: Tournament state");
        }

        Console.showMessage("");
        Console.showMessage("Current state:");
        Console.showMessage("  Master Dicer Level: " + CosmiconEventState.getLegendLevel());
        Console.showMessage("  Casino Battle Active: " + CosmiconEventState.isCasinoBattleMode());
        Console.showMessage("  Casino Battle Type: " + (CosmiconEventState.isCasinoBattleBoss() ? "BOSS" : "CHALLENGE"));
        Console.showMessage("  Tournament Unlocked: " + CasinoIntegrationManager.isTournamentUnlocked());
        Console.showMessage("  Tournament Active: " + CosmiconEventState.isTournamentActive());

        return CommandResult.SUCCESS;
    }

    private static boolean isValidMode(String mode) {
        return mode.equals("all") || mode.equals("legend") || mode.equals("battle") || mode.equals("tournament");
    }
}

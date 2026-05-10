package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.state.CosmiconEventState;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoReset implements BaseCommand {

    private static final String KEY_HUNTER_LEVEL = "$cos_trashcan_hunter_level";

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String mode = args.isEmpty() ? "all" : args.trim().toLowerCase();

        if (!isValidMode(mode)) {
            Console.showMessage("Error: invalid mode '" + mode + "'. Valid modes: all, hunter, battle, tournament");
            return CommandResult.BAD_SYNTAX;
        }

        boolean resetHunter = mode.equals("all") || mode.equals("hunter");
        boolean resetBattle = mode.equals("all") || mode.equals("battle");
        boolean resetTournament = mode.equals("all") || mode.equals("tournament");

        if (resetHunter) {
            MemoryAPI mem = Global.getSector().getPlayerMemoryWithoutUpdate();
            if (mem.contains(KEY_HUNTER_LEVEL)) {
                mem.unset(KEY_HUNTER_LEVEL);
            }
            Console.showMessage("Reset: Master Dicer Level");
        }

        if (resetBattle) {
            CosmiconEventState.clearCasinoBattleState();
            Console.showMessage("Reset: Casino battle state");
        }

        if (resetTournament) {
            CosmiconEventState.clearTournamentAll();
            Console.showMessage("Reset: Tournament state");
        }

        Console.showMessage("");
        Console.showMessage("Current state:");
        Console.showMessage("  Master Dicer Level: " + CosmiconEventState.getTrashcanHunterLevel());
        Console.showMessage("  Casino Battle Active: " + CosmiconEventState.isCasinoBattleMode());
        Console.showMessage("  Casino Battle Type: " + (CosmiconEventState.isCasinoBattleBoss() ? "BOSS" : "CHALLENGE"));
        Console.showMessage("  Tournament Unlocked: " + CosmiconEventState.isTournamentUnlocked());
        Console.showMessage("  Tournament Active: " + CosmiconEventState.isTournamentActive());

        return CommandResult.SUCCESS;
    }

    private static boolean isValidMode(String mode) {
        return mode.equals("all") || mode.equals("hunter") || mode.equals("battle") || mode.equals("tournament");
    }
}

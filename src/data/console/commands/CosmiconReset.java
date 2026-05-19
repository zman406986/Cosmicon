package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.state.CosmiconStats;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconReset implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String mode = args.isEmpty() ? "all" : args.trim().toLowerCase();

        if (!isValidMode(mode)) {
            Console.showMessage("Error: invalid mode '" + mode + "'. Valid modes: all, stats, unlocks, player");
            return CommandResult.BAD_SYNTAX;
        }

        MemoryAPI mem = Global.getSector().getPlayerMemoryWithoutUpdate();

        boolean resetStats = mode.equals("all") || mode.equals("stats");
        boolean resetUnlocks = mode.equals("all") || mode.equals("unlocks");
        boolean resetPlayer = mode.equals("all") || mode.equals("player");

        if (resetStats) {
            mem.set("$cos_games_played", 0f);
            mem.set("$cos_games_won", 0f);
            Console.showMessage("Reset: games played/won");
        }

        if (resetUnlocks) {
            List<String> charKeys = Arrays.asList("$cos_unlocked_characters", "$cos_has_gallery_characters");
            for (String key : charKeys) {
                if (mem.contains(key)) {
                    mem.unset(key);
                }
            }

            if (mem.contains("$cos_unlocked_prismatic")) {
                mem.unset("$cos_unlocked_prismatic");
            }
            if (mem.contains("$cos_prismatic_feature_unlocked")) {
                mem.unset("$cos_prismatic_feature_unlocked");
            }

            Console.showMessage("Reset: character & prismatic dice unlocks");
        }

        if (resetPlayer) {
            List<String> playerKeys = Arrays.asList(
                "$cos_selected_character", "$cos_equipped_prismatic", "$cos_equipped_prismatic_true"
            );
            for (String key : playerKeys) {
                if (mem.contains(key)) {
                    mem.unset(key);
                }
            }

            Console.showMessage("Reset: selected character & prismatic dice");
        }

        Console.showMessage("");
        Console.showMessage("Current state:");
        Console.showMessage("  Games played: " + CosmiconStats.getGamesPlayed()
            + ", won: " + CosmiconStats.getGamesWon());
        Console.showMessage("  Unlocked characters: " + CosmiconStats.getUnlockedCharacters().size());
        Console.showMessage("  Unlocked prismatic: " + CosmiconStats.getUnlockedPrismaticDice().size());
        Console.showMessage("  Tutorial mode: " + CosmiconStats.isInTutorialMode());

        return CommandResult.SUCCESS;
    }

    private static boolean isValidMode(String mode) {
        return mode.equals("all") || mode.equals("stats") || mode.equals("unlocks") || mode.equals("player");
    }
}
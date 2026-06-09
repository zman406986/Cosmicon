package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;
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
        MemoryAPI sectorMem = Global.getSector().getMemory();

        boolean resetStats = mode.equals("all") || mode.equals("stats");
        boolean resetUnlocks = mode.equals("all") || mode.equals("unlocks");
        boolean resetPlayer = mode.equals("all") || mode.equals("player");

        if (resetStats) {
            mem.set("$cos_games_played", 0f);
            mem.set("$cos_games_won", 0f);
            Console.showMessage("Reset: games played/won");
        }

        if (resetUnlocks) {
            List<String> charKeys = List.of("$cos_unlocked_characters");
            for (String key : charKeys) {
                mem.unset(key);
            }

            mem.unset("$cos_unlocked_prismatic");
            mem.unset("$cos_unlocked_prismatic_true");
            mem.unset("$cos_prismatic_feature_unlocked");
            mem.unset("$cos_tutorial_1_completed");
            mem.unset("$cos_tutorial_2_completed");
            mem.unset("$cos_bonus_hp_unlocked");
            mem.unset("$cos_bonus_atk_unlocked");
            mem.unset("$cos_bonus_def_unlocked");
            mem.unset("$cos_tournament_unlocked");
            mem.unset("$cos_gatekeeper_999_unlocked");
            mem.unset("$cos_legend_title_inherited");
            mem.unset("$cos_data_version");
            mem.unset("$cos_migrated_from_prerework");

            Console.showMessage("Reset: character & prismatic dice unlocks, bonuses, progression flags");
        }

        if (resetPlayer) {
            List<String> playerKeys = List.of(
                "$cos_selected_character", "$cos_equipped_prismatic", "$cos_equipped_prismatic_true"
            );
            for (String key : playerKeys) {
                sectorMem.unset(key);
            }

            sectorMem.unset("$cos_credit_bonus_active");
            for (String charId : CharacterIds.getAllIds()) {
                sectorMem.unset("$cos_bonus_" + charId);
            }

            Console.showMessage("Reset: selected character, prismatic dice, bonus selections");
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
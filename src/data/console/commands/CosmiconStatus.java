package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconStatus implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean verbose = "verbose".equalsIgnoreCase(args.trim());

        int gamesPlayed = CosmiconStats.getGamesPlayed();
        int gamesWon = CosmiconStats.getGamesWon();
        boolean inTutorial = CosmiconStats.isInTutorialMode();
        int remaining = CosmiconStats.getRemainingTutorialGames();

        Set<String> unlockedChars = CosmiconStats.getUnlockedCharacters();
        Set<String> unlockedPrismatic = CosmiconStats.getUnlockedPrismaticDice();

        String selectedChar = CosmiconPlayerState.loadCharacter();
        String equippedPrismatic = CosmiconPlayerState.loadPrismaticDice();
        boolean equippedTrue = CosmiconPlayerState.loadPrismaticDiceTrueVersion();

        int totalChars = CharacterRegistry.getAllCards().size();
        int totalPrismatic = PrismaticDiceRegistry.getAll().size();

        Console.showMessage("=== Cosmicon Status ===");
        Console.showMessage("Games: " + gamesPlayed + " played, " + gamesWon + " won");
        Console.showMessage("Tutorial: " + (inTutorial ? "IN PROGRESS (" + remaining + " remaining)" : "COMPLETE"));
        Console.showMessage("Characters: " + unlockedChars.size() + "/" + totalChars + " unlocked");
        Console.showMessage("Prismatic Dice: " + unlockedPrismatic.size() + "/" + totalPrismatic + " unlocked");

        if (selectedChar != null && !selectedChar.isEmpty()) {
            CharacterCard card = CharacterRegistry.getCharacterById(selectedChar);
            String charName = card != null ? card.getName() : selectedChar;
            Console.showMessage("Selected Character: " + charName + " (" + selectedChar + ")");
        } else {
            Console.showMessage("Selected Character: (none)");
        }

        if (equippedPrismatic != null && !equippedPrismatic.isEmpty()) {
            String diceName = PrismaticDisplayHelper.getDiceDisplayName(equippedPrismatic);
            Console.showMessage("Equipped Prismatic: " + diceName + " (" + equippedPrismatic
                + (equippedTrue ? " true" : "") + ")");
        } else {
            Console.showMessage("Equipped Prismatic: (none)");
        }

        if (verbose) {
            Console.showMessage("");
            Console.showMessage("--- Unlocked Characters ---");
            if (unlockedChars.isEmpty()) {
                Console.showMessage("  (none)");
            } else {
                for (CharacterCard card : CharacterRegistry.getAllCards()) {
                    String marker = unlockedChars.contains(card.getId()) ? "[+]" : "[ ]";
                    Console.showMessage("  " + marker + " " + card.getId() + " - " + card.getName()
                        + " (HP:" + card.getMaxHp() + " Atk:" + card.getAtkLevel() + " Def:" + card.getDefLevel() + ")");
                }
            }

            Console.showMessage("");
            Console.showMessage("--- Unlocked Prismatic Dice ---");
            if (unlockedPrismatic.isEmpty()) {
                Console.showMessage("  (none)");
            } else {
                for (java.util.Map.Entry<String, PrismaticDiceType> entry : PrismaticDiceRegistry.getAll().entrySet()) {
                    String marker = unlockedPrismatic.contains(entry.getKey()) ? "[+]" : "[ ]";
                    String name = PrismaticDisplayHelper.getDiceDisplayName(entry.getKey());
                    String hasTrue = entry.getValue().hasTrueVersion() ? " [has true]" : "";
                    Console.showMessage("  " + marker + " " + entry.getKey() + " - " + name + hasTrue);
                }
            }

            Console.showMessage("");
            Console.showMessage("--- Config ---");
            Console.showMessage("  marketSizeMin: " + data.scripts.CosmiconConfig.MARKET_SIZE_MIN);
            Console.showMessage("  defaultHP: " + data.scripts.CosmiconConfig.DEFAULT_HP);
            Console.showMessage("  defaultRerolls: " + data.scripts.CosmiconConfig.DEFAULT_REROLLS);
            Console.showMessage("  debugEnabled: " + data.scripts.CosmiconConfig.DEBUG_ENABLED);
            Console.showMessage("  cosmiconDiceEnabled: " + data.scripts.CosmiconConfig.COSMICON_ENABLED);

            Console.showMessage("");
            Console.showMessage("--- Memory Keys ($cos_*) ---");
            com.fs.starfarer.api.campaign.rules.MemoryAPI mem = Global.getSector().getPlayerMemoryWithoutUpdate();
            java.util.Collection<String> keys = mem.getKeys();
            if (keys != null) {
                for (String key : keys) {
                    if (key.startsWith("$cos_")) {
                        Object val = mem.get(key);
                        String valStr = val != null ? val.toString() : "null";
                        if (valStr.length() > 120) {
                            valStr = valStr.substring(0, 120) + "...";
                        }
                        Console.showMessage("  " + key + " = " + valStr);
                    }
                }
            }
        }

        return CommandResult.SUCCESS;
    }
}
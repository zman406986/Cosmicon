package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconUnlock implements BaseCommand {

    private static final String KEY_PRISMATIC_FEATURE = "$cos_prismatic_feature_unlocked";

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }

        String[] parts = args.split(" ");

        if ("all".equalsIgnoreCase(parts[0])) {
            unlockAll();
            return CommandResult.SUCCESS;
        }

        String category = parts[0].toLowerCase();

        switch (category) {
            case "char" -> {
                if (parts.length < 2) {
                    return CommandResult.BAD_SYNTAX;
                }
                unlockChar(parts[1].toLowerCase());
            }
            case "prismatic" -> {
                if (parts.length >= 2 && "true".equalsIgnoreCase(parts[1])) {
                    String trueId = parts.length >= 3 ? parts[2].toLowerCase() : null;
                    unlockPrismaticTrue(trueId);
                } else {
                    String id = parts.length >= 2 ? parts[1].toLowerCase() : null;
                    unlockPrismatic(id);
                }
            }
            default -> {
                Console.showMessage("Error: first argument must be 'char', 'prismatic', or 'all'.");
                return CommandResult.BAD_SYNTAX;
            }
        }

        return CommandResult.SUCCESS;
    }

    private void unlockChar(String id) {
        if ("all".equals(id)) {
            int count = 0;
            for (CharacterCard card : CharacterRegistry.getAllCards()) {
                String cardId = card.getId();
                if (!CosmiconStats.isCharacterUnlocked(cardId)) {
                    CosmiconStats.unlockCharacter(cardId);
                    count++;
                }
            }
            Console.showMessage("Unlocked " + count + " characters.");
            return;
        }

        CharacterCard card = CharacterRegistry.getCharacterById(id);
        if (card == null) {
            Console.showMessage("Error: unknown character '" + id + "'. Valid IDs:");
            for (CharacterCard c : CharacterRegistry.getAllCards()) {
                String marker = CosmiconStats.isCharacterUnlocked(c.getId()) ? " (already unlocked)" : "";
                Console.showMessage("  " + c.getId() + " - " + c.getName() + marker);
            }
            return;
        }

        if (CosmiconStats.isCharacterUnlocked(id)) {
            Console.showMessage("Character '" + card.getName() + "' is already unlocked.");
            return;
        }

        CosmiconStats.unlockCharacter(id);
        Console.showMessage("Unlocked character: " + card.getName() + " (" + id + ")");
    }

    private void unlockPrismatic(String id) {
        if (id == null) {
            int count = 0;
            for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                if (!type.hasTrueVersion()) {
                    String diceId = type.getId();
                    if (!CosmiconStats.isPrismaticDiceUnlocked(diceId)) {
                        CosmiconStats.unlockPrismaticDice(diceId);
                        count++;
                    }
                }
            }
            Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);
            Console.showMessage("Unlocked " + count + " prismatic dice (non-true only).");
            Console.showMessage("Use 'cosmicon_unlock prismatic all' to unlock ALL prismatic dice.");
            return;
        }

        if ("all".equals(id)) {
            int count = 0;
            int trueCount = 0;
            for (Map.Entry<String, PrismaticDiceType> entry : PrismaticDiceRegistry.getAll().entrySet()) {
                String diceId = entry.getKey();
                if (!CosmiconStats.isPrismaticDiceUnlocked(diceId)) {
                    CosmiconStats.unlockPrismaticDice(diceId);
                    count++;
                }
                if (entry.getValue().hasTrueVersion() && !CosmiconStats.isPrismaticTrueUnlocked(diceId)) {
                    CosmiconStats.unlockPrismaticTrue(diceId);
                    trueCount++;
                }
            }
            Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);
            Console.showMessage("Unlocked " + count + " prismatic dice and " + trueCount + " true versions (all).");
            return;
        }

        if (!PrismaticDiceRegistry.has(id)) {
            Console.showMessage("Error: unknown prismatic dice '" + id + "'. Valid IDs:");
            for (String diceId : PrismaticDiceRegistry.getAll().keySet()) {
                String name = PrismaticDisplayHelper.getDiceDisplayName(diceId);
                String marker = CosmiconStats.isPrismaticDiceUnlocked(diceId) ? " (already unlocked)" : "";
                Console.showMessage("  " + diceId + " - " + name + marker);
            }
            return;
        }

        if (CosmiconStats.isPrismaticDiceUnlocked(id)) {
            String name = PrismaticDisplayHelper.getDiceDisplayName(id);
            Console.showMessage("Prismatic dice '" + name + "' is already unlocked.");
            return;
        }

        CosmiconStats.unlockPrismaticDice(id);
        Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);
        String name = PrismaticDisplayHelper.getDiceDisplayName(id);
        Console.showMessage("Unlocked prismatic dice: " + name + " (" + id + ")");
    }

    private void unlockPrismaticTrue(String id) {
        if (id == null) {
            int count = 0;
            for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                if (type.hasTrueVersion() && CosmiconStats.isPrismaticDiceUnlocked(type.getId())
                    && !CosmiconStats.isPrismaticTrueUnlocked(type.getId())) {
                    CosmiconStats.unlockPrismaticTrue(type.getId());
                    count++;
                }
            }
            Console.showMessage("Unlocked " + count + " true versions (for already-unlocked dice).");
            return;
        }

        if (!PrismaticDiceRegistry.has(id)) {
            Console.showMessage("Error: unknown prismatic dice '" + id + "'.");
            return;
        }

        PrismaticDiceType type = PrismaticDiceRegistry.get(id);
        if (!type.hasTrueVersion()) {
            String name = PrismaticDisplayHelper.getDiceDisplayName(id);
            Console.showMessage("'" + name + "' does not have a true version.");
            return;
        }

        if (!CosmiconStats.isPrismaticDiceUnlocked(id)) {
            String name = PrismaticDisplayHelper.getDiceDisplayName(id);
            Console.showMessage("Unlock the default version of '" + name + "' first.");
            return;
        }

        if (CosmiconStats.isPrismaticTrueUnlocked(id)) {
            String name = PrismaticDisplayHelper.getDiceDisplayName(id);
            Console.showMessage("True version of '" + name + "' is already unlocked.");
            return;
        }

        CosmiconStats.unlockPrismaticTrue(id);
        String name = PrismaticDisplayHelper.getDiceDisplayName(id);
        Console.showMessage("Unlocked true version: " + name + " (" + id + ")");
    }

    private void unlockAll() {
        int charCount = 0;
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            if (!CosmiconStats.isCharacterUnlocked(card.getId())) {
                CosmiconStats.unlockCharacter(card.getId());
                charCount++;
            }
        }

        int diceCount = 0;
        int trueCount = 0;
        for (Map.Entry<String, PrismaticDiceType> entry : PrismaticDiceRegistry.getAll().entrySet()) {
            String diceId = entry.getKey();
            if (!CosmiconStats.isPrismaticDiceUnlocked(diceId)) {
                CosmiconStats.unlockPrismaticDice(diceId);
                diceCount++;
            }
            if (entry.getValue().hasTrueVersion() && !CosmiconStats.isPrismaticTrueUnlocked(diceId)) {
                CosmiconStats.unlockPrismaticTrue(diceId);
                trueCount++;
            }
        }

        Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);

        if (CosmiconStats.isInTutorialMode()) {
            CosmiconStats.forceCompleteTutorial();
        }

        Console.showMessage("Unlocked " + charCount + " characters, " + diceCount + " prismatic dice, and " + trueCount + " true versions.");
    }
}
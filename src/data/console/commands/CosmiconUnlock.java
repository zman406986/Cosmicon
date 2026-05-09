package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconUnlock implements BaseCommand {

    private static final String KEY_PRISMATIC_FEATURE = "$cos_prismatic_feature_unlocked";

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) {
            return CommandResult.BAD_SYNTAX;
        }

        String[] parts = args.split(" ");
        if (parts.length < 2 && !"all".equalsIgnoreCase(parts[0])) {
            return CommandResult.BAD_SYNTAX;
        }

        String category = parts[0].toLowerCase();
        String id = parts.length >= 2 ? parts[1].toLowerCase() : "all";

        if ("all".equalsIgnoreCase(parts[0])) {
            unlockAll();
            return CommandResult.SUCCESS;
        }

        switch (category) {
            case "char" -> unlockChar(id);
            case "prismatic" -> unlockPrismatic(id);
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
        if ("all".equals(id)) {
            int count = 0;
            for (String diceId : PrismaticDiceRegistry.getAll().keySet()) {
                if (!CosmiconStats.isPrismaticDiceUnlocked(diceId)) {
                    CosmiconStats.unlockPrismaticDice(diceId);
                    count++;
                }
            }
            Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);
            Console.showMessage("Unlocked " + count + " prismatic dice.");
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

    private void unlockAll() {
        int charCount = 0;
        for (CharacterCard card : CharacterRegistry.getAllCards()) {
            if (!CosmiconStats.isCharacterUnlocked(card.getId())) {
                CosmiconStats.unlockCharacter(card.getId());
                charCount++;
            }
        }

        int diceCount = 0;
        for (String diceId : PrismaticDiceRegistry.getAll().keySet()) {
            if (!CosmiconStats.isPrismaticDiceUnlocked(diceId)) {
                CosmiconStats.unlockPrismaticDice(diceId);
                diceCount++;
            }
        }

        Global.getSector().getPlayerMemoryWithoutUpdate().set(KEY_PRISMATIC_FEATURE, true);

        if (CosmiconStats.isInTutorialMode()) {
            CosmiconStats.forceCompleteTutorial();
        }

        Console.showMessage("Unlocked " + charCount + " characters and " + diceCount + " prismatic dice.");
    }
}
package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.state.CosmiconEventState;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoBoss implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CosmiconEventState.clearAll();

        String[] parts = args.isEmpty() ? new String[0] : args.split(" ");
        String opponentId = null;
        int bonusHp = 15;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            switch (i) {
                case 0 -> opponentId = part;
                case 1 -> {
                    try {
                        bonusHp = Integer.parseInt(part);
                    } catch (NumberFormatException e) {
                        Console.showMessage("Error: bonus HP must be an integer.");
                        return CommandResult.BAD_SYNTAX;
                    }
                }
            }
        }

        if (opponentId == null || opponentId.isEmpty() || "random".equalsIgnoreCase(opponentId)) {
            CharacterCard randomOpponent = CharacterRegistry.getRandomOpponent();
            if (randomOpponent == null) {
                Console.showMessage("Error: No opponents available.");
                return CommandResult.ERROR;
            }
            opponentId = randomOpponent.getId();
        } else {
            CharacterCard card = CharacterRegistry.getCharacterById(opponentId);
            if (card == null) {
                Console.showMessage("Error: Unknown opponent '" + opponentId + "'. Valid IDs:");
                for (CharacterCard c : CharacterRegistry.getAllCards()) {
                    Console.showMessage("  " + c.getId() + " - " + c.getName());
                }
                return CommandResult.BAD_SYNTAX;
            }
        }

        CharacterCard opponent = CharacterRegistry.getCharacterById(opponentId);
        boolean useTrue = false;
        if (opponent != null && !opponent.getPrismaticDiceIds().isEmpty()) {
            String defaultPrismatic = opponent.getPrismaticDiceIds().keySet().iterator().next();
            CosmiconEventState.setOpponentPrismatic(defaultPrismatic);
            PrismaticDiceType diceType = PrismaticDiceRegistry.get(defaultPrismatic);
            useTrue = diceType != null && diceType.hasTrueVersion();
            CosmiconEventState.setOpponentUsesTrue(useTrue);
        }

        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(true);
        CosmiconEventState.setCasinoBattleOpponent(opponentId);
        CosmiconEventState.setCasinoBattleBonusHp(bonusHp);
        CosmiconEventState.setCasinoBattleUseTrue(useTrue);
        CosmiconEventState.setIsBarEvent(false);
        CosmiconEventState.setIsTutorialMode(false);

        String msg = "Starting casino boss battle vs " + opponentId + " (+" + bonusHp + " HP)";
        if (useTrue) msg += " (true prismatic)";
        Console.showMessage(msg + "...");

        Console.showDialogOnClose(
            new CosmiconInteraction(),
            Global.getSector().getPlayerFleet()
        );

        return CommandResult.SUCCESS;
    }
}
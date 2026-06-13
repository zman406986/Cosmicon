package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconStart implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String[] parts = args.isEmpty() ? new String[0] : args.split(" ");

        CosmiconEventState.clearAll();

        String opponentId = null;
        String prismaticDiceId = null;
        boolean useTrue = false;
        int tutorialGame = -1;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            switch (i) {
                case 0 -> opponentId = part;
                case 1 -> prismaticDiceId = part;
                case 2 -> useTrue = "true".equalsIgnoreCase(part);
                case 3 -> {
                    try {
                        tutorialGame = Integer.parseInt(part);
                    } catch (NumberFormatException e) {
                        Console.showMessage("Error: tutorialGameNum must be 1 or 2.");
                        return CommandResult.BAD_SYNTAX;
                    }
                }
            }
        }

        if (tutorialGame != -1 && (tutorialGame < 1 || tutorialGame > 2)) {
            Console.showMessage("Error: tutorialGameNum must be 1 or 2.");
            return CommandResult.BAD_SYNTAX;
        }

        if (tutorialGame >= 1) {
            CosmiconEventState.setReplayTutorialGame(tutorialGame);
            CosmiconEventState.setIsTutorialMode(true);

            String tutOpponent = tutorialGame == 1 ? CharacterIds.TRASHCAN : "robin";
            CosmiconEventState.setOpponentCharacter(tutOpponent);

            Console.showMessage("Starting Cosmicon Tutorial Game " + tutorialGame + " vs " + tutOpponent + "...");
        } else {
            CosmiconEventState.setIsTutorialMode(CosmiconStats.isInTutorialMode());

            if (opponentId == null || "random".equalsIgnoreCase(opponentId)) {
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
                    List<CharacterCard> all = CharacterRegistry.getAllCards();
                    for (CharacterCard c : all) {
                        Console.showMessage("  " + c.getId() + " - " + c.getName());
                    }
                    return CommandResult.BAD_SYNTAX;
                }
            }

            CosmiconEventState.setOpponentCharacter(opponentId);

            if (prismaticDiceId != null) {
                if (PrismaticDiceRegistry.isNotRegistered(prismaticDiceId)) {
                    Console.showMessage("Error: Unknown prismatic dice '" + prismaticDiceId + "'. Valid IDs:");
                    for (String id : PrismaticDiceRegistry.getAll().keySet()) {
                        Console.showMessage("  " + id);
                    }
                    return CommandResult.BAD_SYNTAX;
                }
                CosmiconEventState.setOpponentPrismatic(prismaticDiceId);
                if (useTrue) {
                    CosmiconEventState.setOpponentUsesTrue(true);
                }
            }

            String msg = "Starting Cosmicon battle vs " + opponentId;
            if (prismaticDiceId != null) {
                msg += " (prismatic: " + prismaticDiceId + (useTrue ? " true" : "") + ")";
            }
            Console.showMessage(msg + "...");
        }

        CosmiconEventState.setIsBarEvent(false);

        Console.showDialogOnClose(
            new CosmiconInteraction(),
            Global.getSector().getPlayerFleet()
        );

        return CommandResult.SUCCESS;
    }
}
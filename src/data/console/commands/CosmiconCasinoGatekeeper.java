package data.console.commands;

import com.fs.starfarer.api.Global;
import data.scripts.cosmicon.CosmiconInteraction;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.util.CharacterIds;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoGatekeeper implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CosmiconEventState.clearAll();

        int bonusHp = 74;
        String trimmed = args.trim();
        if (!trimmed.isEmpty()) {
            try {
                bonusHp = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                Console.showMessage("Error: bonus HP must be an integer.");
                return CommandResult.BAD_SYNTAX;
            }
        }

        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);
        CosmiconEventState.setCasinoBattleOpponent(CharacterIds.TRASHCAN);
        CosmiconEventState.setCasinoBattleBonusHp(bonusHp);
        CosmiconEventState.setCasinoBattleUseTrue(false);
        CosmiconEventState.setIsBarEvent(false);
        CosmiconEventState.setIsTutorialMode(false);

        Console.showMessage("Starting gatekeeper battle vs Trashcan (+" + bonusHp + " HP)...");

        Console.showDialogOnClose(
            new CosmiconInteraction(),
            Global.getSector().getPlayerFleet()
        );

        return CommandResult.SUCCESS;
    }
}

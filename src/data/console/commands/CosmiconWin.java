package data.console.commands;

import data.scripts.cosmicon.battle.BattleController;
import data.scripts.cosmicon.battle.TurnState;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class CosmiconWin implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        BattleController controller = BattleController.getCurrentInstance();
        if (controller == null) {
            Console.showMessage("No active Cosmicon battle.");
            return CommandResult.ERROR;
        }

        if (controller.getState().getCurrentPhase() == TurnState.Phase.ENDED) {
            Console.showMessage("Battle already ended.");
            return CommandResult.ERROR;
        }

        controller.forcePlayerWin();
        Console.showMessage("Forced player victory.");
        return CommandResult.SUCCESS;
    }
}

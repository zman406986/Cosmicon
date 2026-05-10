package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconStats;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class CosmiconCasinoTournament implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CosmiconEventState.clearTournamentState();
        CosmiconStats.setTournamentUnlocked(true);

        Console.showMessage("Starting 8-player tournament...");

        Console.showDialogOnClose(
            new TournamentDialogDelegate(),
            Global.getSector().getPlayerFleet()
        );

        return CommandResult.SUCCESS;
    }

    private static class TournamentDialogDelegate implements InteractionDialogPlugin {

        @Override
        public void init(InteractionDialogAPI dialog) {
            CasinoIntegrationManager.startTournament(dialog, dialog::dismiss);
        }

        @Override public void optionSelected(String optionText, Object optionData) {}
        @Override public void optionMousedOver(String optionText, Object optionData) {}
        @Override public void advance(float amount) {}
        @Override public void backFromEngagement(EngagementResultAPI battleResult) {}
        @Override public Object getContext() { return null; }
        @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
    }
}

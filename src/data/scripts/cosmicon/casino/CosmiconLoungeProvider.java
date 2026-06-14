package data.scripts.cosmicon.casino;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import data.scripts.CosmiconConfig;
import data.scripts.Strings;
import data.scripts.casino.CasinoAPI;
import data.scripts.casino.interaction.CasinoInteraction;
import data.scripts.casino.interaction.LoungeProvider;
import data.scripts.cosmicon.state.CosmiconStats;

public class CosmiconLoungeProvider implements LoungeProvider {

    private List<String> cachedHelpLines;

    @Override
    public String getString(String key) {
        return Strings.get("lounge." + key);
    }

    @Override
    public String formatString(String key, Object... args) {
        return Strings.format("lounge." + key, args);
    }

    @Override
    public String getEnterButtonLabel() {
        return getString("btn_enter");
    }

    @Override
    public List<String> getHelpLines() {
        if (cachedHelpLines == null) {
            cachedHelpLines = List.of(
                getString("help_title"),
                getString("help_1"),
                getString("help_2"),
                getString("help_3")
            );
        }
        return cachedHelpLines;
    }

    @Override
    public String getHelpOptionLabel() {
        return getString("help_enter");
    }

    @Override
    public void showLounge(InteractionDialogAPI dialog) {
        if (dialog.getPlugin() instanceof CasinoInteraction casino) {
            casino.setState(CasinoInteraction.State.COSMICON_LOUNGE);
        }

        dialog.getTextPanel().addPara(getString("welcome"), Color.CYAN);

        int hunterLevel = CasinoIntegrationManager.getLegendLevel();
        if (hunterLevel > 0) {
            if (CosmiconStats.isLegendTitleInherited()) {
                dialog.getTextPanel().addPara(formatString("master_dicer_greeting_legend"), Color.CYAN);
            } else {
                dialog.getTextPanel().addPara(formatString("master_dicer_greeting", hunterLevel), Color.CYAN);
            }
        }

        if (!CasinoIntegrationManager.isTutorialComplete()) {
            dialog.getTextPanel().addPara(getString("tutorial_required"), Color.ORANGE);
        }
    }

    @Override
    public List<MenuOption> getMenuOptions(InteractionDialogAPI dialog) {
        List<MenuOption> options = new ArrayList<>();
        boolean tutorialDone = CasinoIntegrationManager.isTutorialComplete();

        boolean canAffordLegend = CasinoAPI.canAfford(CosmiconConfig.LEGEND_COST);
        boolean tournamentActive = CasinoIntegrationManager.isTournamentActive();
        String legendTooltip = null;
        if (tournamentActive) {
            legendTooltip = getString("legend_tournament_active");
        } else if (!tutorialDone) {
            legendTooltip = getString("tutorial_required");
        } else if (!canAffordLegend) {
            legendTooltip = getString("legend_insufficient");
        }
        options.add(new MenuOption("lounge_legend", getString("legend"),
            tutorialDone && canAffordLegend && !tournamentActive,
            legendTooltip));

        if (tournamentActive) {
            options.add(new MenuOption("lounge_continue_tournament", getString("continue_tournament"),
                true, null));
        } else {
            boolean tournamentUnlocked = CasinoIntegrationManager.isTournamentUnlocked();
            boolean canAffordTournament = CasinoAPI.canAfford(CosmiconConfig.TOURNAMENT_COST);
            String tournamentTooltip = null;
            if (!tournamentUnlocked) {
                tournamentTooltip = getString("tournament_locked");
            } else if (!canAffordTournament) {
                tournamentTooltip = getString("tournament_insufficient");
            }
            options.add(new MenuOption("lounge_tournament", getString("tournament"),
                tutorialDone && tournamentUnlocked && canAffordTournament,
                tournamentTooltip));
        }

        options.add(new MenuOption("lounge_back", getString("back"), true, null));

        return options;
    }

    @Override
    public void handleOption(InteractionDialogAPI dialog, String option,
                             Runnable onReturnToLounge, Runnable onReturnToCasino) {
        switch (option) {
            case "lounge_legend" -> handleLegend(dialog, onReturnToLounge);
            case "lounge_tournament" -> handleTournament(dialog, onReturnToLounge);
            case "lounge_continue_tournament" -> handleContinueTournament(dialog, onReturnToLounge);
            case "lounge_back" -> onReturnToCasino.run();
            default -> onReturnToLounge.run();
        }
    }

    private void handleLegend(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        if (!CasinoAPI.canAfford(CosmiconConfig.LEGEND_COST) || !CasinoIntegrationManager.isTutorialComplete()) {
            dialog.getOptionPanel().clearOptions();
            dialog.getTextPanel().addPara(getString("legend_insufficient"), Color.RED);
            dialog.getOptionPanel().addOption(getString("back"), "lounge_back");
            return;
        }

        CasinoAPI.deduct(CosmiconConfig.LEGEND_COST);
        CasinoIntegrationManager.startLegendBattle(dialog, onReturnToLounge);
    }

    private void handleTournament(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        if (!CasinoAPI.canAfford(CosmiconConfig.TOURNAMENT_COST) || !CasinoIntegrationManager.isTutorialComplete()) {
            dialog.getOptionPanel().clearOptions();
            dialog.getTextPanel().addPara(getString("tournament_insufficient"), Color.RED);
            dialog.getOptionPanel().addOption(getString("back"), "lounge_back");
            return;
        }

        CasinoAPI.deduct(CosmiconConfig.TOURNAMENT_COST);
        CasinoIntegrationManager.startTournament(dialog, onReturnToLounge);
    }

    private void handleContinueTournament(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        CasinoIntegrationManager.continueTournament(dialog, onReturnToLounge);
    }
}

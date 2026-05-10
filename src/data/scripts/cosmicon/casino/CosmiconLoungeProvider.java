package data.scripts.cosmicon.casino;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;

import data.scripts.Strings;
import data.scripts.casino.CasinoAPI;
import data.scripts.casino.interaction.LoungeProvider;

public class CosmiconLoungeProvider implements LoungeProvider {

    private static final int GATEKEEPER_COST = 5000;
    private static final int TOURNAMENT_COST = 15000;

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
        List<String> lines = new ArrayList<>();
        lines.add(getString("help_title"));
        lines.add(getString("help_1"));
        lines.add(getString("help_2"));
        lines.add(getString("help_3"));
        return lines;
    }

    @Override
    public String getHelpOptionLabel() {
        return getString("help_enter");
    }

    @Override
    public void showLounge(InteractionDialogAPI dialog) {
        dialog.getTextPanel().addPara(getString("welcome"), Color.CYAN);

        int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
        if (hunterLevel > 0) {
            dialog.getTextPanel().addPara(formatString("trashcan_hunter_greeting", hunterLevel), Color.CYAN);
        }

        if (!CasinoIntegrationManager.isTutorialComplete()) {
            dialog.getTextPanel().addPara(getString("tutorial_required"), Color.ORANGE);
        }
    }

    @Override
    public List<MenuOption> getMenuOptions(InteractionDialogAPI dialog) {
        List<MenuOption> options = new ArrayList<>();
        boolean tutorialDone = CasinoIntegrationManager.isTutorialComplete();

        boolean canAffordGatekeeper = CasinoAPI.canAfford(GATEKEEPER_COST);
        boolean tournamentActive = CasinoIntegrationManager.isTournamentActive();
        String gatekeeperTooltip = null;
        if (tournamentActive) {
            gatekeeperTooltip = getString("gatekeeper_tournament_active");
        } else if (!tutorialDone) {
            gatekeeperTooltip = getString("tutorial_required");
        } else if (!canAffordGatekeeper) {
            gatekeeperTooltip = getString("gatekeeper_insufficient");
        }
        options.add(new MenuOption("lounge_gatekeeper", getString("gatekeeper"),
            tutorialDone && canAffordGatekeeper && !tournamentActive,
            gatekeeperTooltip));

        if (tournamentActive) {
            options.add(new MenuOption("lounge_continue_tournament", getString("continue_tournament"),
                true, null));
        } else {
            boolean tournamentUnlocked = CasinoIntegrationManager.isTournamentUnlocked();
            boolean canAffordTournament = CasinoAPI.canAfford(TOURNAMENT_COST);
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
            case "lounge_gatekeeper" -> handleGatekeeper(dialog, onReturnToLounge);
            case "lounge_tournament" -> handleTournament(dialog, onReturnToLounge);
            case "lounge_continue_tournament" -> handleContinueTournament(dialog, onReturnToLounge);
            case "lounge_back" -> onReturnToCasino.run();
            default -> onReturnToLounge.run();
        }
    }

    private void handleGatekeeper(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        if (!CasinoAPI.canAfford(GATEKEEPER_COST)) {
            dialog.getOptionPanel().clearOptions();
            dialog.getTextPanel().addPara(getString("gatekeeper_insufficient"), Color.RED);
            dialog.getOptionPanel().addOption(getString("back"), "lounge_back");
            return;
        }

        CasinoAPI.deduct(GATEKEEPER_COST);
        CasinoIntegrationManager.startGatekeeperBattle(dialog, onReturnToLounge);
    }

    private void handleTournament(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        if (!CasinoAPI.canAfford(TOURNAMENT_COST)) {
            dialog.getOptionPanel().clearOptions();
            dialog.getTextPanel().addPara(getString("tournament_insufficient"), Color.RED);
            dialog.getOptionPanel().addOption(getString("back"), "lounge_back");
            return;
        }

        CasinoAPI.deduct(TOURNAMENT_COST);
        CasinoIntegrationManager.startTournament(dialog, onReturnToLounge);
    }

    private void handleContinueTournament(InteractionDialogAPI dialog, Runnable onReturnToLounge) {
        CasinoIntegrationManager.continueTournament(dialog, onReturnToLounge);
    }
}

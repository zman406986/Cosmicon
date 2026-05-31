package data.scripts.cosmicon.battle;

import java.util.Map;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

import data.scripts.cosmicon.state.CosmiconEventState;

public class BattleDialogDelegate implements com.fs.starfarer.api.campaign.CustomVisualDialogDelegate {
    private static final String COMPLETION_STR = "BattleCompleted";

    private final BattlePanelUI battlePanel;
    private final BattleController battleController;
    private final InteractionDialogAPI dialog;
    private final Map<String, MemoryAPI> memoryMap;
    private final Runnable onDismissCallback;
    private final Runnable onVictoryCallback;
    private final Runnable onDefeatCallback;
    private final boolean playerIsAttacker;

    public BattleDialogDelegate(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap,
            Runnable onDismissCallback, boolean playerIsAttacker) {
        this(dialog, memoryMap, onDismissCallback, null, null, playerIsAttacker);
    }

    public BattleDialogDelegate(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap,
            Runnable onDismissCallback, Runnable onVictoryCallback, Runnable onDefeatCallback,
            boolean playerIsAttacker) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        this.onDismissCallback = onDismissCallback;
        this.onVictoryCallback = onVictoryCallback;
        this.onDefeatCallback = onDefeatCallback;
        this.playerIsAttacker = playerIsAttacker;

        this.battleController = new BattleController();
        this.battlePanel = new BattlePanelUI();
        battlePanel.setBattleController(battleController);
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return battlePanel;
    }

    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        battleController.initBattleWithSelection(playerIsAttacker);

        if (CosmiconEventState.isLegendSkipEnabled()) {
            battleController.preApplyOpponentDamage(99);
        }

        battlePanel.init(panel, callbacks);
        battlePanel.wireTutorial(battleController.getTutorialController());
        battlePanel.updateLabelsFromState();
        battleController.startBattle();
    }

    @Override
    public float getNoiseAlpha() {
        return 0.2f;
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void reportDismissed(int option) {
        battlePanel.dispose();

        String winner = battleController.getState().getWinner();
        boolean playerWon = "player".equals(winner);

        if (CosmiconEventState.isCasinoBattleMode()) {
            int damageDealt = battleController.getState().getOpponentTotalDamageTaken();
            CosmiconEventState.setCasinoBattleResultDamage(damageDealt);
            boolean opponentKilled = battleController.getState().getOpponentHp() <= 0;
            CosmiconEventState.setCasinoBattleOpponentKilled(opponentKilled);
        }

        battleController.cleanup();

        if (memoryMap != null) {
            com.fs.starfarer.api.impl.campaign.rulecmd.FireBest.fire(null, dialog, memoryMap, COMPLETION_STR);
        }

        if (playerWon && onVictoryCallback != null) {
            onVictoryCallback.run();
        } else if (!playerWon && onDefeatCallback != null) {
            onDefeatCallback.run();
        }

        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }
}

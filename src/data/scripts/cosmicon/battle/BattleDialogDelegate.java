package data.scripts.cosmicon.battle;

import java.util.Map;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;

public class BattleDialogDelegate implements com.fs.starfarer.api.campaign.CustomVisualDialogDelegate {
    private static final String COMPLETION_STR = "BattleCompleted";

    private final BattlePanelUI battlePanel;
    private final BattleController battleController;
    private final InteractionDialogAPI dialog;
    private final Map<String, MemoryAPI> memoryMap;
    private final Runnable onDismissCallback;
    private final boolean playerIsAttacker;

    public BattleDialogDelegate(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, Runnable onDismissCallback, boolean playerIsAttacker) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        this.onDismissCallback = onDismissCallback;
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
        battlePanel.init(panel, callbacks);
        battleController.initBattleWithSelection(playerIsAttacker);
        battlePanel.updateLabelsFromState();
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
        battlePanel.cleanup();
        battleController.cleanup();

        if (memoryMap != null) {
            com.fs.starfarer.api.impl.campaign.rulecmd.FireBest.fire(null, dialog, memoryMap, COMPLETION_STR);
        }
        onDismissCallback.run();
    }
}

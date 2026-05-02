package data.scripts.cosmicon.setup;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;

public class CharacterSetupDialogDelegate implements com.fs.starfarer.api.campaign.CustomVisualDialogDelegate {
    
    private final CharacterSetupPanelUI panelUI;

    public CharacterSetupDialogDelegate(CharacterSetupPanelUI.CharacterSetupCallback callback) {
        this.panelUI = new CharacterSetupPanelUI(callback);
    }
    
    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return panelUI;
    }
    
    @Override
    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        panelUI.init(panel, callbacks);
        
        String savedChar = data.scripts.cosmicon.state.CosmiconPlayerState.loadCharacter();
        String savedDice = data.scripts.cosmicon.state.CosmiconPlayerState.loadPrismaticDice();
        boolean savedTrueVersion = data.scripts.cosmicon.state.CosmiconPlayerState.loadPrismaticDiceTrueVersion();
        
        if (savedChar != null) {
            panelUI.setSelection(savedChar, savedDice, savedTrueVersion);
        } else {
            panelUI.setDefaultSelection();
        }
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
        panelUI.cleanup();
    }
}
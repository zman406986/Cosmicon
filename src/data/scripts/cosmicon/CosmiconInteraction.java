package data.scripts.cosmicon;

import java.util.Map;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import data.scripts.CosmiconMusicPlugin;
import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleDialogDelegate;
import data.scripts.cosmicon.battle.BattleRenderingUtils;
import data.scripts.cosmicon.battle.CoinFlipPanelUI;
import data.scripts.cosmicon.setup.CharacterSetupDialogDelegate;
import data.scripts.cosmicon.setup.CharacterSetupPanelUI;
import data.scripts.cosmicon.state.CosmiconPlayerState;

import java.awt.Color;

public class CosmiconInteraction implements InteractionDialogPlugin {

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected Map<String, MemoryAPI> memoryMap;

    private State currentState = State.MAIN_MENU;

    public enum State {
        MAIN_MENU,
        PLAY,
        HELP
    }

    public static void startInteraction(InteractionDialogAPI dialog) {
        CosmiconInteraction plugin = new CosmiconInteraction();
        dialog.setPlugin(plugin);
        plugin.init(dialog);
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.textPanel = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target != null) {
            this.memoryMap = new java.util.HashMap<>();
            this.memoryMap.put(MemKeys.LOCAL, target.getMemoryWithoutUpdate());
            this.memoryMap.put(MemKeys.ENTITY, target.getMemoryWithoutUpdate());
        }
        
        showMenu();
    }

    public void showMenu() {
        options.clearOptions();

        if (!CosmiconMusicPlugin.isMusicPlaying()) {
            CosmiconMusicPlugin.startMusic();
        }

        textPanel.addPara(Strings.get("menu.welcome"), Color.CYAN);

        options.addOption(Strings.get("menu.start_game"), "start_game");
        options.addOption(Strings.get("menu.character_setup"), "character_setup");
        options.addOption(Strings.get("menu.help"), "help");
        options.addOption(Strings.get("menu.leave"), "leave");

        setState(State.MAIN_MENU);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) return;

        String data = (String) optionData;

        switch (currentState) {
            case MAIN_MENU:
                switch (data)
                {
                    case "start_game" -> startBattleWithSelection();
                    case "character_setup" -> showCharacterSetup();
                    case "help" -> showHelp();
                    case "leave" ->
                    {
                        CosmiconMusicPlugin.stopMusic();
                        dialog.dismiss();
                    }
                }
                break;

            case HELP:
                if ("back".equals(data)) {
                    showMenu();
                }
                break;

            default:
                showMenu();
        }
    }

    private void showCharacterSetup() {
        CharacterSetupPanelUI.CharacterSetupCallback callback = new CharacterSetupPanelUI.CharacterSetupCallback() {
            @Override
            public void onConfirm(String charId, String prismaticDiceId) {
                CosmiconPlayerState.saveCharacter(charId);
                CosmiconPlayerState.savePrismaticDice(prismaticDiceId);
                showMenu();
            }

            @Override
            public void onCancel() {
                showMenu();
            }
        };

        CharacterSetupDialogDelegate delegate = new CharacterSetupDialogDelegate(callback);
        dialog.showCustomVisualDialog(1000f, 700f, delegate);
    }

    private void startBattleWithSelection() {
        CoinFlipPanelUI coinFlipUI = new CoinFlipPanelUI();

        com.fs.starfarer.api.campaign.CustomVisualDialogDelegate coinDelegate =
            new com.fs.starfarer.api.campaign.CustomVisualDialogDelegate() {
                @Override
                public com.fs.starfarer.api.campaign.CustomUIPanelPlugin getCustomPanelPlugin() {
                    return coinFlipUI;
                }

                @Override
                public void init(com.fs.starfarer.api.ui.CustomPanelAPI panel, DialogCallbacks callbacks) {
                    coinFlipUI.init(panel, callbacks);
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
                    boolean playerIsAttacker = coinFlipUI.isPlayerAttacker();
                    coinFlipUI.cleanup();
                    showBattleDialog(playerIsAttacker);
                }
            };

        dialog.showCustomVisualDialog(
            BattleRenderingUtils.PANEL_WIDTH,
            BattleRenderingUtils.PANEL_HEIGHT,
            coinDelegate
        );

        setState(State.PLAY);
    }

    private void showBattleDialog(boolean playerIsAttacker) {
        BattleDialogDelegate delegate = new BattleDialogDelegate(dialog, memoryMap, this::showMenu, playerIsAttacker);

        dialog.showCustomVisualDialog(
            BattleRenderingUtils.PANEL_WIDTH,
            BattleRenderingUtils.PANEL_HEIGHT,
            delegate
        );
    }

    private void showHelp() {
        options.clearOptions();
        textPanel.addPara(Strings.get("help.title"));
        textPanel.addPara(Strings.get("help.intro"));
        textPanel.addPara(Strings.get("help.rule_1"));
        textPanel.addPara(Strings.get("help.rule_2"));
        textPanel.addPara(Strings.get("help.rule_3"));
        textPanel.addPara(Strings.get("help.rule_4"));
        textPanel.addPara(Strings.get("help.rule_5"));
        options.addOption(Strings.get("help.back"), "back");
        setState(State.HELP);
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }

    public State getState() {
        return currentState;
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public InteractionDialogAPI getDialog() {
        return dialog;
    }

    public TextPanelAPI getTextPanel() {
        return textPanel;
    }

    public OptionPanelAPI getOptions() {
        return options;
    }
}
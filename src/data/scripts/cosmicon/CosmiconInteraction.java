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

        options.addOption(Strings.get("menu.play"), "play");
        options.addOption(Strings.get("menu.help"), "help");
        options.addOption(Strings.get("menu.leave"), "leave");

        setState(State.MAIN_MENU);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (optionData == null) return;

        String option = (String) optionData;

        switch (option) {
            case "leave":
                CosmiconMusicPlugin.stopMusic();
                dialog.dismiss();
                break;
            case "play":
                startBattle();
                break;
            case "help":
                showHelp();
                break;
            case "back":
                showMenu();
                break;
            default:
                showMenu();
        }
    }

    private void startBattle() {
        BattleDialogDelegate delegate = new BattleDialogDelegate(dialog, memoryMap, () -> {
            showMenu();
        });

        dialog.showCustomVisualDialog(
            BattleRenderingUtils.PANEL_WIDTH,
            BattleRenderingUtils.PANEL_HEIGHT,
            delegate
        );

        setState(State.PLAY);
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
package data.scripts.cosmicon;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;

import data.scripts.CosmiconMusicPlugin;
import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleDialogDelegate;
import data.scripts.cosmicon.battle.BattleRenderingUtils;
import data.scripts.cosmicon.battle.CoinFlipPanelUI;
import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.setup.CharacterSetupDialogDelegate;
import data.scripts.cosmicon.setup.CharacterSetupPanelUI;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;

import java.awt.Color;

public class CosmiconInteraction implements InteractionDialogPlugin {

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected Map<String, MemoryAPI> memoryMap;

    private State currentState = State.MAIN_MENU;

    private String pendingRewardCharId;
    private String pendingRewardPrismaticId;

    private int pendingCasinoRewardTier = 0;
    private List<String> pendingCasinoRewardCandidates = null;

    private Runnable onLeaveAction = null;

    public void setOnLeaveAction(Runnable action) {
        this.onLeaveAction = action;
    }

    public enum State {
        MAIN_MENU,
        PLAY,
        HELP,
        REWARD_SELECTION,
        CASINO_BOSS_REWARD
    }

    public static void startInteraction(InteractionDialogAPI dialog) {
        com.fs.starfarer.api.Global.getLogger(CosmiconInteraction.class).info(
            "[DIAG] startInteraction called - dialog=" + dialog +
            " currentPlugin=" + dialog.getPlugin().getClass().getName());
        CosmiconInteraction plugin = new CosmiconInteraction();
        dialog.setPlugin(plugin);
        com.fs.starfarer.api.Global.getLogger(CosmiconInteraction.class).info(
            "[DIAG] setPlugin done. Calling init...");
        plugin.init(dialog);
        com.fs.starfarer.api.Global.getLogger(CosmiconInteraction.class).info(
            "[DIAG] init returned. State=" + plugin.currentState);
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

        if (CasinoIntegrationManager.isCasinoLoaded()) {
            int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
            if (hunterLevel > 0) {
                textPanel.addPara(Strings.format("menu.trashcan_hunter_welcome", hunterLevel), Color.CYAN);
            }
        }

        textPanel.addPara(Strings.get("menu.welcome"), Color.CYAN);

        if (CosmiconStats.isInTutorialMode()) {
            textPanel.addPara(Strings.format("tutorial.games_remaining",
                CosmiconStats.getRemainingTutorialGames()));
            options.addOption("[Debug] Skip Tutorial", "debug_skip_tutorial");
        }

        boolean sessionWon = CosmiconEventState.isBarEvent() && CosmiconEventState.isSessionWon();
        boolean isTutorial = CosmiconStats.isInTutorialMode();
        if (sessionWon && !isTutorial) {
            textPanel.addPara(Strings.get("menu.session_won"));
        } else {
            options.addOption(Strings.get("menu.start_game"), "start_game");
        }

        options.addOption(Strings.get("menu.character_setup"), "character_setup");
        options.addOption("Replay Tutorial Game 1", "replay_tutorial_1");
        options.addOption("Replay Tutorial Game 2", "replay_tutorial_2");
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
                switch (data) {
                    case "start_game" -> startBattleWithSelection();
                    case "replay_tutorial_1" -> {
                        CosmiconEventState.setReplayTutorialGame(1);
                        startBattleWithSelection();
                    }
                    case "replay_tutorial_2" -> {
                        CosmiconEventState.setReplayTutorialGame(2);
                        startBattleWithSelection();
                    }
                    case "character_setup" -> showCharacterSetup();
                    case "help" -> showHelp();
                    case "leave" -> {
                        CosmiconEventState.clearAll();
                        CosmiconMusicPlugin.stopMusic();
                        if (onLeaveAction != null) {
                            onLeaveAction.run();
                        } else {
                            dialog.dismiss();
                        }
                    }
                    case "debug_skip_tutorial" -> {
                        CosmiconStats.forceCompleteTutorial();
                        showMenu();
                    }
                }
                break;

            case HELP:
                if ("back".equals(data)) {
                    showMenu();
                }
                break;

            case REWARD_SELECTION:
                handleRewardSelection(data);
                break;

            case CASINO_BOSS_REWARD:
                handleCasinoRewardSelection(data);
                break;

            default:
                showMenu();
        }
    }

    private void showCharacterSetup() {
        CharacterSetupPanelUI.CharacterSetupCallback callback = new CharacterSetupPanelUI.CharacterSetupCallback() {
            @Override
            public void onConfirm(String charId, String prismaticDiceId, boolean useTrueVersion) {
                CosmiconPlayerState.saveCharacter(charId);
                CosmiconPlayerState.savePrismaticDice(prismaticDiceId);
                CosmiconPlayerState.savePrismaticDiceTrueVersion(useTrueVersion);
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
        Boolean forcedPlayerIsAttacker = null;
        if (TutorialController.shouldActivateTutorial()) {
            TutorialController.TutorialGame tutorialGame = TutorialController.determineTutorialGame();
            forcedPlayerIsAttacker = tutorialGame == TutorialController.TutorialGame.GAME_1_SPARXIE;
        }

        CoinFlipPanelUI coinFlipUI = new CoinFlipPanelUI(forcedPlayerIsAttacker);

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
        BattleDialogDelegate delegate = new BattleDialogDelegate(dialog, memoryMap,
            null,
            this::handleVictory,
            this::handleDefeat,
            playerIsAttacker);

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

        if (CasinoIntegrationManager.isCasinoLoaded()) {
            textPanel.addPara(Strings.get("help.casino_title"), Color.CYAN);
            textPanel.addPara(Strings.get("help.casino_1"));
            textPanel.addPara(Strings.get("help.casino_2"));
            textPanel.addPara(Strings.get("help.casino_3"));
        }

        options.addOption(Strings.get("help.back"), "back");
        setState(State.HELP);
    }

    private void handleVictory() {
        if (CosmiconEventState.isCasinoBattleMode()) {
            handleCasinoVictory();
            return;
        }

        markSessionWonIfBarEvent();

        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (!isReplay) {
            CosmiconStats.incrementGamesPlayed();
            CosmiconStats.incrementGamesWon();
        }

        boolean tutorialGame1 = !isReplay && CosmiconStats.getGamesPlayed() == 1;
        boolean tutorialGame2 = !isReplay && CosmiconStats.getGamesPlayed() == 2;

        pendingRewardCharId = CosmiconEventState.getOpponentCharacter();
        pendingRewardPrismaticId = CosmiconEventState.getOpponentPrismatic();

        if (tutorialGame1) {
            CosmiconStats.completeTutorialGame1();
        } else if (tutorialGame2) {
            CosmiconStats.completeTutorialGame2();
            CosmiconEventState.setIsTutorialMode(false);
            CosmiconEventState.setOpponentCharacter(null);
        }

        if (isReplay) {
            showReplayVictoryMenu();
        } else if (CosmiconStats.isInTutorialMode()) {
            showTutorialReward();
        } else {
            showVictoryRewardMenu();
        }
    }

    private void handleDefeat() {
        if (CosmiconEventState.isCasinoBattleMode()) {
            handleCasinoDefeat();
            return;
        }

        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (!isReplay && !CosmiconStats.isInTutorialMode()) {
            CosmiconStats.incrementGamesPlayed();
        }

        if (isReplay) {
            showReplayDefeatMenu();
        } else {
            showDefeatMenu();
        }
    }

    private void showTutorialReward() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));

        int gamesPlayed = CosmiconStats.getGamesPlayed();
        if (gamesPlayed == 1) {
            textPanel.addPara(Strings.get("tutorial.g1_reward"));
            int remaining = CosmiconStats.getRemainingTutorialGames();
            if (remaining > 0) {
                textPanel.addPara(Strings.format("tutorial.games_remaining", remaining));
            }
            options.addOption(Strings.get("menu.back"), "back");
        } else if (gamesPlayed >= 2) {
            options.addOption(Strings.get("tutorial.g2_unlock_option"), "tutorial_g2_unlock");
        }

        setState(State.REWARD_SELECTION);
    }

    private void showVictoryRewardMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));

        boolean hasCharReward = false;
        boolean hasPrismaticReward = false;

        if (pendingRewardCharId != null && !CosmiconStats.isCharacterUnlocked(pendingRewardCharId)) {
            options.addOption(Strings.format("reward.unlock_character",
                data.scripts.cosmicon.battle.CharacterRegistry.getCharacterById(pendingRewardCharId).getName()),
                "unlock_character");
            hasCharReward = true;
        }

        if (pendingRewardPrismaticId != null && !CosmiconStats.isPrismaticDiceUnlocked(pendingRewardPrismaticId)) {
            options.addOption(Strings.format("reward.unlock_prismatic",
                data.scripts.cosmicon.util.PrismaticDisplayHelper.getDiceDisplayName(pendingRewardPrismaticId)),
                "unlock_prismatic");
            hasPrismaticReward = true;
        }

        if (!hasCharReward && !hasPrismaticReward) {
            int playerLevel = Global.getSector().getPlayerStats().getLevel();
            int credits = CosmiconStats.calculateCreditReward(playerLevel);
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
            AddRemoveCommodity.addCreditsGainText(credits, textPanel);
            options.addOption(Strings.get("menu.back"), "back");
        } else {
            options.addOption(Strings.get("reward.take_credits"), "take_credits");
        }

        setState(State.REWARD_SELECTION);
    }

    private void showDefeatMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_lost"));

        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (!isReplay && !CosmiconStats.isInTutorialMode()) {
            int playerLevel = Global.getSector().getPlayerStats().getLevel();
            int consolationCredits = 500 * Math.max(1, playerLevel);
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(consolationCredits);
            AddRemoveCommodity.addCreditsGainText(consolationCredits, textPanel);
        }

        int remaining = CosmiconStats.getRemainingTutorialGames();
        if (CosmiconStats.isInTutorialMode() && remaining > 0) {
            textPanel.addPara(Strings.format("tutorial.games_remaining", remaining));
        }
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showReplayVictoryMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));
        textPanel.addPara("(Replay tutorial - stats not recorded)");
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showReplayDefeatMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_lost"));
        textPanel.addPara("(Replay tutorial - stats not recorded)");
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void handleRewardSelection(String data) {
        switch (data) {
            case "unlock_character" -> {
                if (pendingRewardCharId != null) {
                    CosmiconStats.unlockCharacter(pendingRewardCharId);
                    textPanel.addPara(Strings.get("reward.character_unlocked"));
                }
                finishReward();
            }
            case "unlock_prismatic" -> {
                if (pendingRewardPrismaticId != null) {
                    CosmiconStats.unlockPrismaticDice(pendingRewardPrismaticId);
                    textPanel.addPara(Strings.get("reward.prismatic_unlocked"));
                }
                finishReward();
            }
            case "take_credits" -> {
                int playerLevel = Global.getSector().getPlayerStats().getLevel();
                int credits = CosmiconStats.calculateCreditReward(playerLevel);
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                finishReward();
            }
            case "tutorial_g2_unlock" -> finishReward();
            default -> finishReward();
        }
    }

    private void markSessionWonIfBarEvent() {
        if (CosmiconEventState.isBarEvent()) {
            CosmiconEventState.setSessionWon(true);
        }
    }

    private void handleCasinoVictory() {
        options.clearOptions();
        int damageDealt = CosmiconEventState.getCasinoBattleResultDamage();

        if (CosmiconEventState.isCasinoBattleBoss()) {
            textPanel.addPara(Strings.get("casino.boss_victory"), Color.GREEN);

            int tier = CasinoIntegrationManager.getBossRewardTier();

            if (tier == 4) {
                int credits = CasinoIntegrationManager.getCreditReward() * 3;
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                textPanel.addPara(Strings.get("casino.boss_all_unlocked"), Color.GRAY);
                options.addOption(Strings.get("casino.back_lounge"), "casino_back");
                setState(State.CASINO_BOSS_REWARD);
            } else {
                pendingCasinoRewardTier = tier;
                pendingCasinoRewardCandidates = CasinoIntegrationManager.getRewardCandidates(tier, 3);

                for (int i = 0; i < pendingCasinoRewardCandidates.size(); i++) {
                    String id = pendingCasinoRewardCandidates.get(i);
                    String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
                    if (tier == 1) {
                        options.addOption(Strings.format("casino.boss_reward_char", displayName), "casino_reward_" + i);
                    } else {
                        options.addOption(Strings.format("casino.boss_reward_prismatic", displayName), "casino_reward_" + i);
                    }
                }

                int credits = CasinoIntegrationManager.getCreditReward() * 3;
                options.addOption(Strings.format("casino.boss_reward_credits", credits), "casino_reward_credits");
                setState(State.CASINO_BOSS_REWARD);
            }
        } else {
            int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
            boolean isNewRecord = damageDealt > hunterLevel;
            CasinoIntegrationManager.updateTrashcanHunterLevel(damageDealt);
            int newLevel = CasinoIntegrationManager.getTrashcanHunterLevel();

            textPanel.addPara(Strings.get("casino.trashcan_won"), Color.GREEN);

            int credits = CasinoIntegrationManager.getCreditReward() * 10;
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
            AddRemoveCommodity.addCreditsGainText(credits, textPanel);

            if (isNewRecord) {
                textPanel.addPara(Strings.format("casino.challenge_new_record", newLevel), Color.CYAN);
            } else {
                textPanel.addPara(Strings.format("casino.challenge_previous_record", newLevel), Color.GRAY);
            }

            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.CASINO_BOSS_REWARD);
        }
    }

    private void handleCasinoDefeat() {
        options.clearOptions();
        int damageDealt = CosmiconEventState.getCasinoBattleResultDamage();

        if (CosmiconEventState.isCasinoBattleBoss()) {
            textPanel.addPara(Strings.get("casino.boss_defeat"), Color.RED);
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.CASINO_BOSS_REWARD);
        } else {
            int hunterLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
            boolean isNewRecord = damageDealt > hunterLevel;
            CasinoIntegrationManager.updateTrashcanHunterLevel(damageDealt);
            int newLevel = CasinoIntegrationManager.getTrashcanHunterLevel();

            textPanel.addPara(Strings.get("battle.you_lost"), Color.RED);

            if (isNewRecord) {
                textPanel.addPara(Strings.format("casino.challenge_new_record", newLevel), Color.CYAN);
            } else {
                textPanel.addPara(Strings.format("casino.challenge_result", damageDealt, newLevel), Color.GRAY);
            }

            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.CASINO_BOSS_REWARD);
        }
    }

    private void handleCasinoRewardSelection(String data) {
        switch (data) {
            case "casino_reward_0" -> applyCasinoReward(0);
            case "casino_reward_1" -> applyCasinoReward(1);
            case "casino_reward_2" -> applyCasinoReward(2);
            case "casino_reward_credits" -> {
                int credits = CasinoIntegrationManager.getCreditReward() * 3;
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                options.clearOptions();
                options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            }
            case "casino_back" -> {
                if (onLeaveAction != null) {
                    onLeaveAction.run();
                }
            }
            default -> {
                options.clearOptions();
                options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            }
        }
    }

    private void applyCasinoReward(int index) {
        if (pendingCasinoRewardCandidates == null || index >= pendingCasinoRewardCandidates.size()) return;

        String id = pendingCasinoRewardCandidates.get(index);
        int tier = pendingCasinoRewardTier;

        switch (tier) {
            case 1 -> CasinoIntegrationManager.unlockCharacterReward(id);
            case 2, 3 -> CasinoIntegrationManager.unlockPrismaticReward(id);
        }

        String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
        if (tier == 1) {
            textPanel.addPara(Strings.format("reward.character_unlocked"), Color.GREEN);
        } else {
            textPanel.addPara(Strings.format("reward.prismatic_unlocked"), Color.GREEN);
        }

        pendingCasinoRewardCandidates = null;
        pendingCasinoRewardTier = 0;

        options.clearOptions();
        options.addOption(Strings.get("casino.back_lounge"), "casino_back");
    }

    private void finishReward() {
        CosmiconEventState.clearBattleState();
        showMenu();
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
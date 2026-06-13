package data.scripts.cosmicon;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;

import data.scripts.CosmiconConfig;
import data.scripts.CosmiconMusicPlugin;
import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleDialogDelegate;
import data.scripts.cosmicon.battle.BattleRenderingUtils;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CoinFlipPanelUI;
import data.scripts.cosmicon.casino.CasinoIntegrationManager;
import data.scripts.cosmicon.casino.TournamentBracketPanel;
import data.scripts.cosmicon.casino.TournamentManager;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.setup.CharacterSetupDialogDelegate;
import data.scripts.cosmicon.setup.CharacterSetupPanelUI;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.BonusState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;

import com.fs.starfarer.api.util.MutableValue;
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
    private int pendingBaseCredits = 0;

    private TournamentManager tournamentManager;
    private int tournamentPendingRewards = 0;
    private int tournamentWins = 0;

    private Runnable onLeaveAction = null;

    public void setOnLeaveAction(Runnable action) {
        this.onLeaveAction = action;
    }

    public enum State {
        MAIN_MENU,
        PLAY,
        HELP,
        REWARD_SELECTION,
        CASINO_BOSS_REWARD,
        GATEKEEPER_REWARD,
        TOURNAMENT_BRACKET,
        TOURNAMENT_REWARD
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

        if (onLeaveAction == null) {
            onLeaveAction = dialog::dismiss;
        }

        SectorEntityToken target = dialog.getInteractionTarget();
        if (target != null) {
            this.memoryMap = new java.util.HashMap<>();
            this.memoryMap.put(MemKeys.LOCAL, target.getMemoryWithoutUpdate());
            this.memoryMap.put(MemKeys.ENTITY, target.getMemoryWithoutUpdate());
        }

        if (CosmiconEventState.isTournamentActive()) {
            String bracketJson = CosmiconEventState.getTournamentBracketData();
            if (bracketJson != null) {
                tournamentManager = TournamentManager.fromJson(bracketJson);
                tournamentWins = CosmiconEventState.getTournamentWins();
                tournamentPendingRewards = CosmiconEventState.getTournamentPendingRewards();
            }
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
                if (CosmiconStats.isLegendTitleInherited()) {
                    textPanel.addPara(Strings.get("menu.trashcan_hunter_welcome_legend"), Color.CYAN);
                } else {
                    textPanel.addPara(Strings.format("menu.trashcan_hunter_welcome", hunterLevel), Color.CYAN);
                }
            }
        }

        textPanel.addPara(Strings.get("menu.welcome"), Color.CYAN);

        if (CosmiconStats.isInTutorialMode()) {
            if (CosmiconStats.isMigratedFromPrerework()) {
                textPanel.addPara(Strings.get("menu.migrated_tutorial_prompt"));
            } else {
                textPanel.addPara(Strings.format("tutorial.games_remaining",
                    CosmiconStats.getRemainingTutorialGames()));
            }
        }

        boolean sessionWon = CosmiconEventState.isSessionWon();
        boolean isTutorial = CosmiconStats.isInTutorialMode();
        if (sessionWon && !isTutorial) {
            textPanel.addPara(Strings.get("menu.session_won"));
        } else {
            options.addOption(Strings.get("menu.start_game"), "start_game");
        }

        options.addOption(Strings.get("menu.character_setup"), "character_setup");
        if (CosmiconStats.isTutorial1Completed() && !CosmiconStats.isInTutorialMode()) {
            options.addOption(Strings.get("menu.replay_tutorial_1"), "replay_tutorial_1");
        }
        if (CosmiconStats.isTutorial2Completed() && !CosmiconStats.isInTutorialMode()) {
            options.addOption(Strings.get("menu.replay_tutorial_2"), "replay_tutorial_2");
        }
        if (CosmiconEventState.isTournamentActive() && CosmiconEventState.isEmbeddedEntry()) {
            options.addOption(Strings.get("menu.view_tournament_standings"), "view_tournament_standings");
            
            if (tournamentManager != null && !tournamentManager.isPlayerChampion() && !tournamentManager.isPlayerEliminated()) {
                String position = getLocalizedBracketPosition(tournamentManager);
                String opponentId = tournamentManager.getNextOpponentId();
                String opponentName = opponentId != null
                    ? data.scripts.cosmicon.battle.CharacterRegistry.getCharacterById(opponentId).getName()
                    : Strings.get("casino.tournament_tbd");
                textPanel.addPara(Strings.format("casino.tournament_status_line",
                    position, tournamentWins, tournamentManager.getBracketData().playerLosses), Color.YELLOW);
                textPanel.addPara(Strings.format("casino.tournament_next_opponent", opponentName));
                options.addOption(Strings.get("casino.forfeit_tournament"), "forfeit_tournament");
            }
        }
        if (CosmiconStats.isInEasyMode()) {
            textPanel.addPara(Strings.format("menu.easy_mode_status",
                CosmiconStats.countUnlockedEasyModeCharacters()));
            if (CosmiconStats.isEasyModeComplete()) {
                options.addOption(Strings.get("menu.start_tutorial_2"), "start_tutorial_2");
            }
        }
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
                    case "start_game" -> {
                        if (CosmiconEventState.isEmbeddedEntry() && CosmiconEventState.isTournamentActive() && tournamentManager != null
                            && !tournamentManager.isPlayerChampion() && !tournamentManager.isPlayerEliminated()) {
                            startCasinoBattleWithSelection();
                        } else {
                            startBattleWithSelection();
                        }
                    }
                    case "replay_tutorial_1" -> {
                        CosmiconEventState.setReplayTutorialGame(1);
                        startBattleWithSelection();
                    }
                    case "replay_tutorial_2" -> {
                        CosmiconEventState.setReplayTutorialGame(2);
                        startBattleWithSelection();
                    }
                    case "start_tutorial_2" -> {
                        CosmiconEventState.setOpponentCharacter(CharacterIds.ROBIN);
                        CosmiconEventState.setIsTutorialMode(true);
                        startBattleWithSelection();
                    }
                    case "character_setup" -> showCharacterSetup();
                    case "view_tournament_standings" -> showTournamentStandingsPanel();
                    case "forfeit_tournament" -> showForfeitTournamentConfirm();
                    case "help" -> showHelp();
                    case "leave" -> {
                        if (CosmiconEventState.isTournamentActive()) {
                            CosmiconEventState.setIsEmbeddedEntry(true);
                            CosmiconEventState.setIsBarEvent(false);
                            CosmiconMusicPlugin.stopMusic();
                        } else {
                            CosmiconEventState.clearAll();
                            CosmiconMusicPlugin.stopMusic();
                        }
                        if (onLeaveAction != null) {
                            onLeaveAction.run();
                        } else {
                            dialog.dismiss();
                        }
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

            case GATEKEEPER_REWARD:
                handleGatekeeperRewardSelection(data);
                break;

            case TOURNAMENT_BRACKET:
                handleTournamentBracketSelection(data);
                break;

            case TOURNAMENT_REWARD:
                handleTournamentRewardSelection(data);
                break;

            default:
                showMenu();
        }
    }

    private void showCharacterSetup() {
        CharacterSetupPanelUI.CharacterSetupCallback callback = new CharacterSetupPanelUI.CharacterSetupCallback() {
            @Override
            public void onConfirm(String charId, String prismaticDiceId, boolean useTrueVersion, BonusState bonus) {
                CosmiconPlayerState.saveCharacter(charId);
                CosmiconPlayerState.savePrismaticDice(prismaticDiceId);
                CosmiconPlayerState.savePrismaticDiceTrueVersion(useTrueVersion);
                CosmiconPlayerState.saveBonusSelection(charId, bonus);
                CosmiconPlayerState.setCreditBonusActive(bonus == BonusState.NONE);
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
            forcedPlayerIsAttacker = tutorialGame == TutorialController.TutorialGame.GAME_1_CHIMERA;
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
        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (isReplay) {
            showReplayVictoryMenu();
            return;
        }

        if (CosmiconEventState.isCasinoBattleMode() && CosmiconEventState.isEmbeddedEntry()) {
            handleCasinoVictory();
            return;
        }

        markSessionWon();

        CosmiconStats.incrementGamesPlayed();
        CosmiconStats.incrementGamesWon();

        boolean tutorialGame1 = !CosmiconStats.isTutorial1Completed();
        boolean tutorialGame2 = CosmiconStats.isTutorial1Completed() && CosmiconEventState.isTutorialMode();

        pendingRewardCharId = CosmiconEventState.getOpponentCharacter();
        pendingRewardPrismaticId = CosmiconEventState.getOpponentPrismatic();

        if (tutorialGame1) {
            CosmiconStats.completeTutorial1();
            CosmiconEventState.setIsTutorialMode(false);
            showTutorialReward();
            return;
        } else if (tutorialGame2) {
            CosmiconStats.completeTutorial2();
            CosmiconEventState.setIsTutorialMode(false);
            String npcChar = CosmiconEventState.getOriginalNpcCharId();
            CosmiconEventState.setOpponentCharacter(npcChar);
            showTutorialReward();
            return;
        }

        if (CosmiconStats.isInTutorialMode()) {
            showTutorialReward();
        } else if (CosmiconStats.isInEasyMode()) {
            showEasyModeVictoryReward();
        } else {
            showVictoryRewardMenu();
        }
    }

    private void handleDefeat() {
        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (isReplay) {
            showReplayDefeatMenu();
            return;
        }

        if (CosmiconEventState.isCasinoBattleMode() && CosmiconEventState.isEmbeddedEntry()) {
            handleCasinoDefeat();
            return;
        }

        if (!CosmiconEventState.isTutorialMode()) {
            CosmiconStats.incrementGamesPlayed();
        }

        showDefeatMenu();
    }

    private void showTutorialReward() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));

        if (CosmiconStats.isTutorial1Completed() && !CosmiconStats.isTutorial2Completed()) {
            textPanel.addPara(Strings.get("tutorial.g1_reward"));
            int remaining = CosmiconStats.getRemainingTutorialGames();
            if (remaining > 0) {
                textPanel.addPara(Strings.format("tutorial.games_remaining", remaining));
            }
            options.addOption(Strings.get("menu.back"), "back");
        } else if (CosmiconStats.isTutorial2Completed()) {
            textPanel.addPara(Strings.get("tutorial.g2_reward"));
            options.addOption(Strings.get("menu.back"), "back");
        }

        setState(State.REWARD_SELECTION);
    }

    private void showVictoryRewardMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));

        boolean hasCharReward = false;
        boolean hasPrismaticReward = false;
        boolean hasPrismaticTrueReward = false;

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

        if (pendingRewardPrismaticId != null && CosmiconStats.isPrismaticDiceUnlocked(pendingRewardPrismaticId)
            && !CosmiconStats.isPrismaticTrueUnlocked(pendingRewardPrismaticId)) {
            PrismaticDiceType diceType = PrismaticDiceRegistry.get(pendingRewardPrismaticId);
            if (diceType != null && diceType.hasTrueVersion()) {
                options.addOption(Strings.format("reward.unlock_prismatic_true",
                    data.scripts.cosmicon.util.PrismaticDisplayHelper.getDiceDisplayName(pendingRewardPrismaticId)),
                    "unlock_prismatic_true");
                hasPrismaticTrueReward = true;
            }
        }

        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        int baseCredits = CosmiconStats.calculateNormalEncounterCreditReward(playerLevel);
        pendingBaseCredits = baseCredits;
        int bonusPercent = CosmiconPlayerState.getCreditBonusPercent();
        if (!hasCharReward && !hasPrismaticReward && !hasPrismaticTrueReward) {
            if (bonusPercent > 0) {
                textPanel.addPara(Strings.format("bonus.credit_bonus_tier", bonusPercent), Color.GREEN);
                int bonusCredits = baseCredits * bonusPercent / 100;
                getCredits().add(bonusCredits);
                AddRemoveCommodity.addCreditsGainText(bonusCredits, textPanel);
            }
            getCredits().add(baseCredits);
            AddRemoveCommodity.addCreditsGainText(baseCredits, textPanel);
            options.addOption(Strings.get("menu.back"), "back");
        } else {
            if (bonusPercent > 0) {
                textPanel.addPara(Strings.format("bonus.credit_bonus_tier", bonusPercent), Color.GREEN);
                int bonusCredits = baseCredits * bonusPercent / 100;
                getCredits().add(bonusCredits);
                AddRemoveCommodity.addCreditsGainText(bonusCredits, textPanel);
            }
            options.addOption(Strings.get("reward.take_credits"), "take_credits");
        }

        setState(State.REWARD_SELECTION);
    }

    private void showEasyModeVictoryReward() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));

        boolean hadHp = CosmiconStats.isHpBonusUnlocked();
        boolean hadAtk = CosmiconStats.isAtkBonusUnlocked();
        boolean hadDef = CosmiconStats.isDefBonusUnlocked();

        String newCharId = CosmiconStats.unlockRandomEasyModeCharacter();
        if (newCharId != null) {
            String charName = CharacterRegistry.getCharacterById(newCharId).getName();
            textPanel.addPara(Strings.format("reward.character_unlocked", charName));
        }

        int collected = CosmiconStats.countUnlockedEasyModeCharacters();
        int total = CharacterIds.EASY_MODE_CHARACTERS.size();
        textPanel.addPara(Strings.format("easy_mode.collection_progress", collected, total), Color.CYAN);

        int winsUntilNext = CosmiconStats.getWinsUntilNextBonus();
        if (winsUntilNext > 0) {
            textPanel.addPara(Strings.format("easy_mode.wins_until_next_bonus", winsUntilNext), Color.CYAN);
        }

        CosmiconStats.checkAndUnlockBonuses();
        if (!hadHp && CosmiconStats.isHpBonusUnlocked()) {
            textPanel.addPara(Strings.get("bonus.unlocked_hp"), Color.CYAN);
        }
        if (!hadAtk && CosmiconStats.isAtkBonusUnlocked()) {
            textPanel.addPara(Strings.get("bonus.unlocked_atk"), Color.CYAN);
        }
        if (!hadDef && CosmiconStats.isDefBonusUnlocked()) {
            textPanel.addPara(Strings.get("bonus.unlocked_def"), Color.CYAN);
        }

        int playerLevel = Global.getSector().getPlayerStats().getLevel();
        int baseCredits = CosmiconStats.calculateNormalEncounterCreditReward(playerLevel);
        pendingBaseCredits = baseCredits;
        int bonusPercent = CosmiconPlayerState.getCreditBonusPercent();
        if (bonusPercent > 0) {
            textPanel.addPara(Strings.format("bonus.credit_bonus_tier", bonusPercent), Color.GREEN);
            int bonusCredits = baseCredits * bonusPercent / 100;
            getCredits().add(bonusCredits);
            AddRemoveCommodity.addCreditsGainText(bonusCredits, textPanel);
        }
        pendingRewardCharId = null;
        pendingRewardPrismaticId = null;
        getCredits().add(baseCredits);
        AddRemoveCommodity.addCreditsGainText(baseCredits, textPanel);

        if (CosmiconStats.isEasyModeComplete()) {
            if (CosmiconStats.isMigratedFromPrerework()) {
                CosmiconStats.completeTutorial2ForMigration();
                textPanel.addPara(Strings.get("easy_mode.migrated_complete"), Color.YELLOW);
            } else {
                textPanel.addPara(Strings.get("easy_mode.all_collected"), Color.YELLOW);
            }
        }

        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showDefeatMenu() {
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_lost"));

        if (!CosmiconStats.isInTutorialMode() && !CosmiconStats.isInEasyMode() && !CosmiconEventState.isTutorialMode()) {
            int playerLevel = Global.getSector().getPlayerStats().getLevel();
            int creditLoss = CosmiconStats.calculateNormalEncounterCreditReward(playerLevel);
            long currentCredits = (long) getCredits().get();
            int actualLoss = Math.min(creditLoss, (int) currentCredits);
            if (actualLoss > 0) {
                getCredits().subtract(actualLoss);
                AddRemoveCommodity.addCreditsLossText(actualLoss, textPanel);
                textPanel.addPara(Strings.format("battle.credit_loss", actualLoss));
            }
        }

        int remaining = CosmiconStats.getRemainingTutorialGames();
        if (CosmiconStats.isInTutorialMode() && remaining > 0) {
            textPanel.addPara(Strings.format("tutorial.games_remaining", remaining));
        }
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showReplayVictoryMenu() {
        CosmiconEventState.setReplayTutorialGame(-1);
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));
        textPanel.addPara(Strings.get("menu.replay_unrecorded"));
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showReplayDefeatMenu() {
        CosmiconEventState.setReplayTutorialGame(-1);
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_lost"));
        textPanel.addPara(Strings.get("menu.replay_unrecorded"));
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void handleRewardSelection(String data) {
        switch (data) {
            case "unlock_character" -> {
                if (pendingRewardCharId != null) {
                    boolean hadHp = CosmiconStats.isHpBonusUnlocked();
                    boolean hadAtk = CosmiconStats.isAtkBonusUnlocked();
                    boolean hadDef = CosmiconStats.isDefBonusUnlocked();

                    CosmiconStats.unlockCharacter(pendingRewardCharId);
                    String charName = CharacterRegistry.getCharacterById(pendingRewardCharId).getName();
                    textPanel.addPara(Strings.format("reward.character_unlocked", charName));

                    CosmiconStats.checkAndUnlockBonuses();
                    if (!hadHp && CosmiconStats.isHpBonusUnlocked()) {
                        textPanel.addPara(Strings.get("bonus.unlocked_hp"), Color.CYAN);
                    }
                    if (!hadAtk && CosmiconStats.isAtkBonusUnlocked()) {
                        textPanel.addPara(Strings.get("bonus.unlocked_atk"), Color.CYAN);
                    }
                    if (!hadDef && CosmiconStats.isDefBonusUnlocked()) {
                        textPanel.addPara(Strings.get("bonus.unlocked_def"), Color.CYAN);
                    }
                }
                finishReward();
            }
            case "unlock_prismatic" -> {
                if (pendingRewardPrismaticId != null) {
                    CosmiconStats.unlockPrismaticDice(pendingRewardPrismaticId);
                    String diceName = PrismaticDisplayHelper.getDiceDisplayName(pendingRewardPrismaticId);
                    textPanel.addPara(Strings.format("reward.prismatic_unlocked", diceName));
                }
                finishReward();
            }
            case "unlock_prismatic_true" -> {
                if (pendingRewardPrismaticId != null) {
                    CosmiconStats.unlockPrismaticTrue(pendingRewardPrismaticId);
                    String diceName = PrismaticDisplayHelper.getDiceDisplayName(pendingRewardPrismaticId);
                    textPanel.addPara(Strings.format("reward.prismatic_true_unlocked", diceName));
                }
                finishReward();
            }
            case "take_credits" -> {
                getCredits().add(pendingBaseCredits);
                AddRemoveCommodity.addCreditsGainText(pendingBaseCredits, textPanel);
                finishReward();
            }
            default -> finishReward();
        }
    }

    private void handleCasinoVictory() {
        options.clearOptions();
        int damageDealt = CosmiconEventState.getCasinoBattleResultDamage();

        if (CosmiconEventState.isCasinoBattleBoss()) {
            textPanel.addPara(Strings.get("casino.boss_victory"), Color.GREEN);

            int tier = CasinoIntegrationManager.getBossRewardTier();

            if (tier == 5) {
                int credits = CasinoIntegrationManager.getCreditReward() * CosmiconConfig.BOSS_CREDIT_MULTIPLIER;
                getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                textPanel.addPara(Strings.get("casino.boss_all_unlocked"), Color.GRAY);
                options.addOption(Strings.get("casino.back_lounge"), "casino_back");
                setState(State.CASINO_BOSS_REWARD);
            } else {
                pendingCasinoRewardTier = tier;
                pendingCasinoRewardCandidates = CasinoIntegrationManager.getRewardCandidates(tier, 3);

                buildRewardOptions(pendingCasinoRewardCandidates, tier, "casino_reward_");

                int credits = CasinoIntegrationManager.getCreditReward() * CosmiconConfig.BOSS_CREDIT_MULTIPLIER;
                options.addOption(Strings.format("casino.boss_reward_credits", credits), "casino_reward_credits");
                setState(State.CASINO_BOSS_REWARD);
            }
        } else if (CosmiconEventState.isTournamentActive()) {
            handleTournamentVictory();
        } else {
            handleGatekeeperVictory(damageDealt);
        }
    }

    private void handleCasinoDefeat() {
        options.clearOptions();
        int damageDealt = CosmiconEventState.getCasinoBattleResultDamage();

        if (CosmiconEventState.isCasinoBattleBoss()) {
            textPanel.addPara(Strings.get("casino.boss_defeat"), Color.RED);
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.CASINO_BOSS_REWARD);
        } else if (CosmiconEventState.isTournamentActive()) {
            handleTournamentDefeat();
        } else {
            handleGatekeeperDefeat(damageDealt);
        }
    }

    private void handleCasinoRewardSelection(String data) {
        switch (data) {
            case "casino_reward_0" -> applyCasinoReward(0);
            case "casino_reward_1" -> applyCasinoReward(1);
            case "casino_reward_2" -> applyCasinoReward(2);
            case "casino_reward_credits" -> {
                int credits = CasinoIntegrationManager.getCreditReward() * CosmiconConfig.BOSS_CREDIT_MULTIPLIER;
                getCredits().add(credits);
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
        applyRewardFromCandidates(index, () -> {
            options.clearOptions();
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
        });
    }

    private void buildRewardOptions(List<String> candidates, int tier, String optionPrefix) {
        for (int i = 0; i < candidates.size(); i++) {
            String id = candidates.get(i);
            String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
            if (tier == 1) {
                options.addOption(Strings.format("casino.boss_reward_char", displayName), optionPrefix + i);
            } else if (tier == 2) {
                options.addOption(Strings.format("casino.boss_reward_prismatic_true", displayName), optionPrefix + i);
            } else {
                options.addOption(Strings.format("casino.boss_reward_prismatic", displayName), optionPrefix + i);
            }
        }
    }

    private void applyRewardFromCandidates(int index, Runnable onDone) {
        if (pendingCasinoRewardCandidates == null || index >= pendingCasinoRewardCandidates.size()) return;

        String id = pendingCasinoRewardCandidates.get(index);
        int tier = pendingCasinoRewardTier;

        switch (tier) {
            case 1 -> CasinoIntegrationManager.unlockCharacterReward(id);
            case 2 -> CasinoIntegrationManager.unlockPrismaticTrueReward(id);
            case 3, 4 -> CasinoIntegrationManager.unlockPrismaticReward(id);
        }

        String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
        if (tier == 1) {
            textPanel.addPara(Strings.format("reward.character_unlocked", displayName), Color.GREEN);
        } else if (tier == 2) {
            textPanel.addPara(Strings.format("reward.prismatic_true_unlocked", displayName), Color.GREEN);
        } else {
            textPanel.addPara(Strings.format("reward.prismatic_unlocked", displayName), Color.GREEN);
        }

        pendingCasinoRewardCandidates = null;
        pendingCasinoRewardTier = 0;

        onDone.run();
    }

    private void handleGatekeeperVictory(int damageDealt) {
        int oldLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
        CasinoIntegrationManager.updateTrashcanHunterLevel(damageDealt);
        int newLevel = CasinoIntegrationManager.getTrashcanHunterLevel();

        boolean is999Battle = CosmiconEventState.getCasinoBattleBonusHp() >= 974;
        boolean opponentKilled = CosmiconEventState.isCasinoBattleOpponentKilled();

        if (is999Battle && opponentKilled && !CosmiconStats.isLegendTitleInherited()) {
            textPanel.addPara(Strings.get("casino.legend_true_kill"), Color.GREEN);
            textPanel.addPara(Strings.get("casino.legend_title_inherited"), Color.CYAN);
            CosmiconStats.setLegendTitleInherited(true);
        } else {
            textPanel.addPara(Strings.get("casino.gatekeeper_victory"), Color.GREEN);
        }

        if (newLevel > oldLevel) {
            textPanel.addPara(Strings.format("casino.gatekeeper_hunter_level_up", newLevel), Color.CYAN);
        }

        int credits = CasinoIntegrationManager.getCreditReward() * CosmiconConfig.GATEKEEPER_WIN_CREDIT_MULTIPLIER;
        getCredits().add(credits);
        AddRemoveCommodity.addCreditsGainText(credits, textPanel);
        textPanel.addPara(Strings.format("casino.gatekeeper_reward_win", credits));

        CasinoIntegrationManager.setTournamentUnlocked(true);
        if (damageDealt >= 99) {
            CosmiconStats.setGatekeeper999Unlocked(true);
        }
        textPanel.addPara(Strings.get("casino.gatekeeper_unlock_tournament"), Color.CYAN);

        options.addOption(Strings.get("casino.back_lounge"), "casino_back");
        setState(State.GATEKEEPER_REWARD);
    }

    private void handleGatekeeperDefeat(int damageDealt) {
        int oldLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
        CasinoIntegrationManager.updateTrashcanHunterLevel(damageDealt);
        int newLevel = CasinoIntegrationManager.getTrashcanHunterLevel();

        boolean dealt99Plus = damageDealt >= 99;

        if (dealt99Plus) {
            textPanel.addPara(Strings.get("casino.gatekeeper_moral_victory"), Color.GREEN);
            CosmiconStats.setGatekeeper999Unlocked(true);
            if (!CasinoIntegrationManager.isTournamentUnlocked()) {
                CasinoIntegrationManager.setTournamentUnlocked(true);
                textPanel.addPara(Strings.get("casino.gatekeeper_unlock_tournament"), Color.CYAN);
            }
        } else {
            textPanel.addPara(Strings.get("casino.gatekeeper_defeat"), Color.RED);
        }

        if (newLevel > oldLevel) {
            textPanel.addPara(Strings.format("casino.gatekeeper_hunter_level_up", newLevel), Color.CYAN);
        }

        int credits = CasinoIntegrationManager.getCreditReward() * CosmiconConfig.GATEKEEPER_LOSS_CREDIT_MULTIPLIER;
        getCredits().add(credits);
        AddRemoveCommodity.addCreditsGainText(credits, textPanel);
        textPanel.addPara(Strings.format("casino.gatekeeper_reward_lose", credits));

        options.addOption(Strings.get("casino.back_lounge"), "casino_back");
        setState(State.GATEKEEPER_REWARD);
    }

    private void handleTournamentVictory() {
        if (tournamentManager == null) {
            textPanel.addPara(Strings.get("casino.tournament_error_load"), Color.RED);
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.TOURNAMENT_BRACKET);
            return;
        }

        tournamentWins++;
        CosmiconEventState.setTournamentWins(tournamentWins);

        if (tournamentManager.isGrandFinal()) {
            tournamentManager.recordGrandFinalGame(true);
            syncTournamentState();
            CosmiconEventState.setTournamentSeriesScore(
                tournamentManager.getBracketData().gfPlayerWins + "-" + tournamentManager.getBracketData().gfOpponentWins);

            if (tournamentManager.isPlayerChampion()) {
                showTournamentReward();
            } else {
                textPanel.addPara(Strings.format("casino.tournament_series_score",
                    tournamentManager.getBracketData().gfPlayerWins,
                    tournamentManager.getBracketData().gfOpponentWins), Color.GREEN);
                showTournamentBracketPanel();
            }
        } else {
            tournamentManager.recordPlayerMatch(true);
            syncTournamentState();

            if (tournamentManager.isPlayerChampion()) {
                showTournamentReward();
            } else {
                showTournamentBracketPanel();
            }
        }
    }

    private void handleTournamentDefeat() {
        if (tournamentManager == null) {
            textPanel.addPara(Strings.get("casino.tournament_error_load"), Color.RED);
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.TOURNAMENT_BRACKET);
            return;
        }

        if (tournamentManager.isGrandFinal()) {
            tournamentManager.recordGrandFinalGame(false);
            syncTournamentState();
            CosmiconEventState.setTournamentSeriesScore(
                tournamentManager.getBracketData().gfPlayerWins + "-" + tournamentManager.getBracketData().gfOpponentWins);

            if (tournamentManager.isPlayerEliminated()) {
                showTournamentReward();
            } else {
                textPanel.addPara(Strings.format("casino.tournament_series_score",
                    tournamentManager.getBracketData().gfPlayerWins,
                    tournamentManager.getBracketData().gfOpponentWins), Color.RED);
                showTournamentBracketPanel();
            }
        } else {
            tournamentManager.recordPlayerMatch(false);
            syncTournamentState();

            if (tournamentManager.isPlayerEliminated()) {
                showTournamentReward();
            } else {
                showTournamentBracketPanel();
            }
        }
    }

    private void syncTournamentState() {
        CosmiconEventState.setTournamentBracketData(tournamentManager.toJson());
        CosmiconEventState.setTournamentWins(tournamentWins);
        CosmiconEventState.setTournamentLosses(tournamentManager.getBracketData().playerLosses);
        CosmiconEventState.setTournamentInLoserBracket(tournamentManager.getBracketData().playerInLoserBracket);
        CosmiconEventState.setTournamentGrandFinal(tournamentManager.isGrandFinal());
    }

    private void showTournamentStandingsPanel() {
        TournamentManager mgr = tournamentManager;
        if (mgr == null) {
            String json = CosmiconEventState.getTournamentBracketData();
            if (json != null) mgr = TournamentManager.fromJson(json);
        }
        if (mgr == null) {
            textPanel.addPara(Strings.get("casino.tournament_error_load"), Color.RED);
            CosmiconEventState.clearTournamentState();
            showMenu();
            return;
        }

        TournamentManager.BracketData bd = mgr.getBracketData();
        String[] displayNames = new String[bd.playerNames.length];
        for (int i = 0; i < bd.playerNames.length; i++) {
            if (i == 0) {
                displayNames[i] = Global.getSector().getPlayerPerson().getNameString();
            } else {
                CharacterCard card = CharacterRegistry.getCharacterById(bd.playerNames[i]);
                displayNames[i] = card != null ? card.getName() : bd.playerNames[i];
            }
        }

        TournamentBracketPanel bracketPanel = new TournamentBracketPanel(bd, displayNames);

        CustomVisualDialogDelegate delegate = createBracketDelegate(bracketPanel);
        dialog.showCustomVisualDialog(1000f, 700f, delegate);
    }

    private CustomVisualDialogDelegate createBracketDelegate(TournamentBracketPanel bracketPanel) {
        return new CustomVisualDialogDelegate() {
            private DialogCallbacks storedCallbacks;

            @Override
            public CustomUIPanelPlugin getCustomPanelPlugin() {
                return bracketPanel;
            }

            @Override
            public void init(com.fs.starfarer.api.ui.CustomPanelAPI panel, DialogCallbacks callbacks) {
                this.storedCallbacks = callbacks;
                bracketPanel.init(panel);
                bracketPanel.setOnDismiss(() -> {
                    if (storedCallbacks != null) {
                        storedCallbacks.dismissDialog();
                    }
                });
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
                bracketPanel.cleanup();
                showMenu();
            }
        };
    }

    private void showForfeitTournamentConfirm() {
        options.clearOptions();
        textPanel.addPara(Strings.get("casino.forfeit_tournament_confirm"), Color.YELLOW);
        options.addOption(Strings.get("casino.forfeit_tournament_yes"), "forfeit_tournament_confirm");
        options.addOption(Strings.get("casino.forfeit_tournament_no"), "forfeit_tournament_cancel");
        setState(State.TOURNAMENT_BRACKET);
    }

    public void showTournamentBracketPanel() {
        showTournamentStandingsPanel();
    }

    private void handleTournamentBracketSelection(String data) {
        switch (data) {
            case "tournament_next_fight", "forfeit_tournament_cancel" -> showMenu();
            case "tournament_back_lounge" -> {
                if (onLeaveAction != null) {
                    onLeaveAction.run();
                }
            }
            case "casino_back" -> {
                if (onLeaveAction != null) {
                    onLeaveAction.run();
                } else {
                    dialog.dismiss();
                }
            }
            case "forfeit_tournament_confirm" -> {
                int totalGames = tournamentWins + CosmiconEventState.getTournamentLosses();
                int participationCredits = CasinoIntegrationManager.getTournamentParticipationCredits(totalGames);
                getCredits().add(participationCredits);
                AddRemoveCommodity.addCreditsGainText(participationCredits, textPanel);
                textPanel.addPara(Strings.format("casino.tournament_participation_credits", participationCredits, totalGames));
                textPanel.addPara(Strings.get("casino.tournament_forfeited"), Color.RED);
                boolean inGrandFinal = tournamentManager != null && tournamentManager.isGrandFinal();
                tournamentPendingRewards = inGrandFinal ? 2 : 1;
                CosmiconEventState.setTournamentPendingRewards(tournamentPendingRewards);
                showTournamentRewardOptions();
            }
        }
    }

    private void showTournamentReward() {
        options.clearOptions();

        boolean champion = tournamentManager != null && tournamentManager.isPlayerChampion();
        boolean runnerUp = tournamentManager != null && !champion && tournamentManager.isGrandFinal();
        int totalGames = tournamentWins + CosmiconEventState.getTournamentLosses();
        int participationCredits = CasinoIntegrationManager.getTournamentParticipationCredits(totalGames);

        if (champion) {
            textPanel.addPara(Strings.get("casino.tournament_victory"), Color.YELLOW);
            tournamentPendingRewards = CosmiconConfig.TOURNAMENT_CHAMPION_REWARDS;
        } else if (runnerUp) {
            textPanel.addPara(Strings.get("casino.tournament_runner_up"), Color.CYAN);
            tournamentPendingRewards = CosmiconConfig.TOURNAMENT_RUNNER_UP_REWARDS;
        } else {
            String position = tournamentManager != null ? tournamentManager.getPlayerBracketPosition() : Strings.get("casino.tournament_position_unknown");
            textPanel.addPara(Strings.format("casino.tournament_eliminated", position), Color.RED);
            tournamentPendingRewards = CosmiconConfig.TOURNAMENT_ELIMINATED_REWARDS;
        }

        getCredits().add(participationCredits);
        AddRemoveCommodity.addCreditsGainText(participationCredits, textPanel);
        textPanel.addPara(Strings.format("casino.tournament_participation_credits", participationCredits, totalGames));

        CosmiconEventState.setTournamentPendingRewards(tournamentPendingRewards);

        showTournamentRewardOptions();
    }

    private void showTournamentRewardOptions() {
        options.clearOptions();

        if (tournamentPendingRewards <= 0) {
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            setState(State.TOURNAMENT_REWARD);
            return;
        }

        textPanel.addPara(Strings.format("casino.tournament_reward_select", tournamentPendingRewards));

        int tier = CasinoIntegrationManager.getBossRewardTier();
        pendingCasinoRewardTier = tier;

        pendingCasinoRewardCandidates = CasinoIntegrationManager.getRewardCandidates(tier, 3);
        buildRewardOptions(pendingCasinoRewardCandidates, tier, "tournament_reward_");
        int credits = CasinoIntegrationManager.getTournamentCreditReward();
        textPanel.addPara(Strings.format("casino.tournament_prize_credits_hint", credits, tournamentWins));
        options.addOption(Strings.format("casino.tournament_prize_credits", credits), "tournament_reward_credits");

        setState(State.TOURNAMENT_REWARD);
    }

    private void handleTournamentRewardSelection(String data) {
        switch (data) {
            case "tournament_reward_0" -> applyTournamentReward(0);
            case "tournament_reward_1" -> applyTournamentReward(1);
            case "tournament_reward_2" -> applyTournamentReward(2);
            case "tournament_reward_credits" -> {
                int credits = CasinoIntegrationManager.getTournamentCreditReward();
                getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                tournamentPendingRewards--;
                if (tournamentPendingRewards > 0) {
                    showTournamentRewardOptions();
                } else {
                    options.clearOptions();
                    options.addOption(Strings.get("casino.back_lounge"), "casino_back");
                }
            }
            case "casino_back" -> {
                clearTournamentState();
                if (onLeaveAction != null) {
                    onLeaveAction.run();
                }
            }
            default -> {
                tournamentPendingRewards--;
                if (tournamentPendingRewards > 0) {
                    showTournamentRewardOptions();
                } else {
                    options.clearOptions();
                    options.addOption(Strings.get("casino.back_lounge"), "casino_back");
                }
            }
        }
    }

    private void applyTournamentReward(int index) {
        applyRewardFromCandidates(index, () -> {
            tournamentPendingRewards--;
            if (tournamentPendingRewards > 0) {
                showTournamentRewardOptions();
            } else {
                options.clearOptions();
                options.addOption(Strings.get("casino.back_lounge"), "casino_back");
            }
        });
    }

    private void clearTournamentState() {
        tournamentManager = null;
        tournamentPendingRewards = 0;
        tournamentWins = 0;
        CosmiconEventState.clearTournamentState();
    }

    private void handleGatekeeperRewardSelection(String data) {
        if ("casino_back".equals(data)) {
            if (onLeaveAction != null) {
                onLeaveAction.run();
            }
        } else {
            options.clearOptions();
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
        }
    }

    private void startCasinoBattleWithSelection() {
        CosmiconEventState.clearCasinoBattleState();
        CosmiconEventState.setCasinoBattleMode(true);
        CosmiconEventState.setCasinoBattleIsBoss(false);

        if (tournamentManager != null) {
            String nextOppId = tournamentManager.getNextOpponentId();
            if (nextOppId != null) {
                CosmiconEventState.setCasinoBattleOpponent(nextOppId);

                CharacterCard oppCard = CharacterRegistry.getCharacterById(nextOppId);
                if (oppCard != null && !oppCard.getPrismaticDiceIds().isEmpty()) {
                    String defaultPrismatic = oppCard.getPrismaticDiceIds().keySet().iterator().next();
                    CosmiconEventState.setOpponentPrismatic(defaultPrismatic);
                    PrismaticDiceType diceType = PrismaticDiceRegistry.get(defaultPrismatic);
                    boolean useTrue = diceType != null && diceType.hasTrueVersion();
                    CosmiconEventState.setCasinoBattleUseTrue(useTrue);
                }
            }
        }
        CosmiconEventState.setCasinoBattleBonusHp(0);

        CoinFlipPanelUI coinFlipUI = new CoinFlipPanelUI(null);

        CustomVisualDialogDelegate coinDelegate = createCoinFlipDelegate(coinFlipUI);

        dialog.showCustomVisualDialog(
            BattleRenderingUtils.PANEL_WIDTH,
            BattleRenderingUtils.PANEL_HEIGHT,
            coinDelegate
        );

        setState(State.PLAY);
    }

    private CustomVisualDialogDelegate createCoinFlipDelegate(CoinFlipPanelUI coinFlipUI) {
        return new CustomVisualDialogDelegate() {
            @Override
            public CustomUIPanelPlugin getCustomPanelPlugin() {
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
    }

    private static MutableValue getCredits() {
        return Global.getSector().getPlayerFleet().getCargo().getCredits();
    }

    private void markSessionWon() {
        CosmiconEventState.setSessionWon(true);
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

    private static String getLocalizedBracketPosition(TournamentManager tm) {
        if (tm.isPlayerChampion()) return Strings.get("casino.tournament_position_champion");
        if (tm.isPlayerEliminated()) return Strings.get("casino.tournament_position_eliminated");
        return switch (tm.getCurrentBracket()) {
            case TournamentManager.BRACKET_WB -> {
                if (tm.getCurrentRound() == 2) yield Strings.get("casino.tournament_position_wb_final");
                yield Strings.format("casino.tournament_position_wb_round", tm.getCurrentRound() + 1);
            }
            case TournamentManager.BRACKET_LB -> {
                if (tm.getCurrentRound() == 3) yield Strings.get("casino.tournament_position_lb_final");
                yield Strings.format("casino.tournament_position_lb_round", tm.getCurrentRound() + 1);
            }
            case TournamentManager.BRACKET_GF -> Strings.get("casino.tournament_position_grand_final");
            default -> Strings.get("casino.tournament_position_unknown");
        };
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
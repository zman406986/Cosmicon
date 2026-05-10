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
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;

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
                textPanel.addPara(Strings.format("menu.trashcan_hunter_welcome", hunterLevel), Color.CYAN);
            }
        }

        textPanel.addPara(Strings.get("menu.welcome"), Color.CYAN);

        if (CosmiconStats.isInTutorialMode()) {
            textPanel.addPara(Strings.format("tutorial.games_remaining",
                CosmiconStats.getRemainingTutorialGames()));
        }

        boolean sessionWon = CosmiconEventState.isBarEvent() && CosmiconEventState.isSessionWon();
        boolean isTutorial = CosmiconStats.isInTutorialMode();
        if (sessionWon && !isTutorial) {
            textPanel.addPara(Strings.get("menu.session_won"));
        } else {
            options.addOption(Strings.get("menu.start_game"), "start_game");
        }

        options.addOption(Strings.get("menu.character_setup"), "character_setup");
        if (CosmiconStats.getGamesPlayed() >= 1 && !CosmiconStats.isInTutorialMode()) {
            options.addOption("Replay Tutorial Game 1", "replay_tutorial_1");
        }
        if (CosmiconStats.getGamesPlayed() >= 2 && !CosmiconStats.isInTutorialMode()) {
            options.addOption("Replay Tutorial Game 2", "replay_tutorial_2");
        }
        if (CosmiconEventState.isTournamentActive()) {
            options.addOption(Strings.get("menu.view_tournament_standings"), "view_tournament_standings");
            
            if (tournamentManager != null && !tournamentManager.isPlayerChampion() && !tournamentManager.isPlayerEliminated()) {
                String position = tournamentManager.getPlayerBracketPosition();
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
                        if (CosmiconEventState.isTournamentActive() && tournamentManager != null
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
                    case "character_setup" -> showCharacterSetup();
                    case "view_tournament_standings" -> showTournamentStandingsPanel();
                    case "forfeit_tournament" -> showForfeitTournamentConfirm();
                    case "help" -> showHelp();
                    case "leave" -> {
                        if (CosmiconEventState.isTournamentActive()) {
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
        boolean isReplay = CosmiconEventState.isReplayTutorial();
        if (isReplay) {
            showReplayVictoryMenu();
            return;
        }

        if (CosmiconEventState.isCasinoBattleMode()) {
            handleCasinoVictory();
            return;
        }

        markSessionWonIfBarEvent();

        CosmiconStats.incrementGamesPlayed();
        CosmiconStats.incrementGamesWon();

        boolean tutorialGame1 = CosmiconStats.getGamesPlayed() == 1;
        boolean tutorialGame2 = CosmiconStats.getGamesPlayed() == 2;

        pendingRewardCharId = CosmiconEventState.getOpponentCharacter();
        pendingRewardPrismaticId = CosmiconEventState.getOpponentPrismatic();

        if (tutorialGame1) {
            CosmiconStats.completeTutorialGame1();
            showTutorialReward();
            return;
        } else if (tutorialGame2) {
            CosmiconStats.completeTutorialGame2();
            CosmiconEventState.setIsTutorialMode(false);
            CosmiconEventState.setOpponentCharacter(null);
            showTutorialReward();
            return;
        }

        if (CosmiconStats.isInTutorialMode()) {
            showTutorialReward();
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

        if (CosmiconEventState.isCasinoBattleMode()) {
            handleCasinoDefeat();
            return;
        }

        if (!CosmiconStats.isInTutorialMode()) {
            CosmiconStats.incrementGamesPlayed();
        }

        showDefeatMenu();
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
        CosmiconEventState.setReplayTutorialGame(-1);
        options.clearOptions();
        textPanel.addPara(Strings.get("battle.you_won"));
        textPanel.addPara("(Replay tutorial - stats not recorded)");
        options.addOption(Strings.get("menu.back"), "back");
        setState(State.REWARD_SELECTION);
    }

    private void showReplayDefeatMenu() {
        CosmiconEventState.setReplayTutorialGame(-1);
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
                    String charName = CharacterRegistry.getCharacterById(pendingRewardCharId).getName();
                    textPanel.addPara(Strings.format("reward.character_unlocked", charName));
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
            case "take_credits" -> {
                int playerLevel = Global.getSector().getPlayerStats().getLevel();
                int credits = CosmiconStats.calculateCreditReward(playerLevel);
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
                AddRemoveCommodity.addCreditsGainText(credits, textPanel);
                finishReward();
            }
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

            if (tier == 5) {
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
                    } else if (tier == 2) {
                        options.addOption(Strings.format("casino.boss_reward_prismatic_true", displayName), "casino_reward_" + i);
                    } else {
                        options.addOption(Strings.format("casino.boss_reward_prismatic", displayName), "casino_reward_" + i);
                    }
                }

                int credits = CasinoIntegrationManager.getCreditReward() * 3;
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

        options.clearOptions();
        options.addOption(Strings.get("casino.back_lounge"), "casino_back");
    }

    private void handleGatekeeperVictory(int damageDealt) {
        int oldLevel = CasinoIntegrationManager.getTrashcanHunterLevel();
        CasinoIntegrationManager.updateTrashcanHunterLevel(damageDealt);
        int newLevel = CasinoIntegrationManager.getTrashcanHunterLevel();

        textPanel.addPara(Strings.get("casino.gatekeeper_victory"), Color.GREEN);

        if (newLevel > oldLevel) {
            textPanel.addPara(Strings.format("casino.gatekeeper_hunter_level_up", newLevel), Color.CYAN);
        }

        int credits = CasinoIntegrationManager.getCreditReward() * 4;
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
        AddRemoveCommodity.addCreditsGainText(credits, textPanel);
        textPanel.addPara(Strings.format("casino.gatekeeper_reward_win", credits));

        CasinoIntegrationManager.setTournamentUnlocked(true);
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

        int credits = CasinoIntegrationManager.getCreditReward() * 2;
        Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
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
                var card = CharacterRegistry.getCharacterById(bd.playerNames[i]);
                displayNames[i] = card != null ? card.getName() : bd.playerNames[i];
            }
        }

        TournamentBracketPanel bracketPanel = new TournamentBracketPanel(bd, displayNames);

        CustomVisualDialogDelegate delegate = new CustomVisualDialogDelegate() {
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

        dialog.showCustomVisualDialog(1000f, 700f, delegate);
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
            case "tournament_next_fight" -> showMenu();
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
                int baseCredits = CasinoIntegrationManager.getCreditReward() * 3;
                int totalCredits = baseCredits * tournamentWins;
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(totalCredits);
                AddRemoveCommodity.addCreditsGainText(totalCredits, textPanel);
                if (tournamentWins > 0) {
                    textPanel.addPara(Strings.format("casino.tournament_credits_earned", totalCredits, tournamentWins, baseCredits));
                }
                textPanel.addPara(Strings.get("casino.tournament_forfeited"), Color.RED);
                boolean inGrandFinal = tournamentManager != null && tournamentManager.isGrandFinal();
                tournamentPendingRewards = inGrandFinal ? 2 : 1;
                CosmiconEventState.setTournamentPendingRewards(tournamentPendingRewards);
                showTournamentRewardOptions();
            }
            case "forfeit_tournament_cancel" -> showMenu();
        }
    }

    private void showTournamentReward() {
        options.clearOptions();

        boolean champion = tournamentManager != null && tournamentManager.isPlayerChampion();
        boolean runnerUp = tournamentManager != null && !champion && tournamentManager.isGrandFinal();
        int baseCredits = CasinoIntegrationManager.getCreditReward() * 3;
        int totalCredits = baseCredits * tournamentWins;

        if (champion) {
            textPanel.addPara(Strings.get("casino.tournament_victory"), Color.YELLOW);
            tournamentPendingRewards = 3;
        } else if (runnerUp) {
            textPanel.addPara(Strings.get("casino.tournament_runner_up"), Color.CYAN);
            tournamentPendingRewards = 2;
        } else {
            String position = tournamentManager != null ? tournamentManager.getPlayerBracketPosition() : "Unknown";
            textPanel.addPara(Strings.format("casino.tournament_eliminated", position), Color.RED);
            tournamentPendingRewards = 1;
        }

        Global.getSector().getPlayerFleet().getCargo().getCredits().add(totalCredits);
        AddRemoveCommodity.addCreditsGainText(totalCredits, textPanel);
        textPanel.addPara(Strings.format("casino.tournament_credits_earned", totalCredits, tournamentWins, baseCredits));

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

        if (tier == 4) {
            int credits = CasinoIntegrationManager.getCreditReward() * 3;
            options.addOption(Strings.format("casino.boss_reward_credits", credits), "tournament_reward_credits");
        } else {
            pendingCasinoRewardCandidates = CasinoIntegrationManager.getRewardCandidates(tier, 3);
            for (int i = 0; i < pendingCasinoRewardCandidates.size(); i++) {
                String id = pendingCasinoRewardCandidates.get(i);
                String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
                if (tier == 1) {
                    options.addOption(Strings.format("casino.boss_reward_char", displayName), "tournament_reward_" + i);
                } else {
                    options.addOption(Strings.format("casino.boss_reward_prismatic", displayName), "tournament_reward_" + i);
                }
            }
            int credits = CasinoIntegrationManager.getCreditReward() * 3;
            options.addOption(Strings.format("casino.boss_reward_credits", credits), "tournament_reward_credits");
        }

        setState(State.TOURNAMENT_REWARD);
    }

    private void handleTournamentRewardSelection(String data) {
        switch (data) {
            case "tournament_reward_0" -> applyTournamentReward(0);
            case "tournament_reward_1" -> applyTournamentReward(1);
            case "tournament_reward_2" -> applyTournamentReward(2);
            case "tournament_reward_credits" -> {
                int credits = CasinoIntegrationManager.getCreditReward() * 3;
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
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
        if (pendingCasinoRewardCandidates == null || index >= pendingCasinoRewardCandidates.size()) return;

        String id = pendingCasinoRewardCandidates.get(index);
        int tier = pendingCasinoRewardTier;

        switch (tier) {
            case 1 -> CasinoIntegrationManager.unlockCharacterReward(id);
            case 2, 3 -> CasinoIntegrationManager.unlockPrismaticReward(id);
        }

        String displayName = CasinoIntegrationManager.getRewardDisplayName(id, tier);
        if (tier == 1) {
            textPanel.addPara(Strings.format("reward.character_unlocked", displayName), Color.GREEN);
        } else {
            textPanel.addPara(Strings.format("reward.prismatic_unlocked", displayName), Color.GREEN);
        }

        tournamentPendingRewards--;
        pendingCasinoRewardCandidates = null;
        pendingCasinoRewardTier = 0;

        if (tournamentPendingRewards > 0) {
            showTournamentRewardOptions();
        } else {
            options.clearOptions();
            options.addOption(Strings.get("casino.back_lounge"), "casino_back");
        }
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

        Boolean forcedPlayerIsAttacker = null;

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
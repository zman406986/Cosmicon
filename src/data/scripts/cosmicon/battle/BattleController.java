package data.scripts.cosmicon.battle;

import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.CosmiconLogger;

public class BattleController implements BattleEventBus.DamageAnimationCallback {

    private static BattleController currentInstance;

    private final BattleState state;
    private final TurnProcessor turnProcessor;
    private TutorialController tutorialController;

    public static BattleController getCurrentInstance() {
        return currentInstance;
    }

    public BattleController() {
        currentInstance = this;
        WeatherController weatherController = new WeatherController();

        AIEngine aiEngine = new AIEngine();
        DiceRoller diceRoller = new DiceRoller(weatherController);
        DamageResolver damageResolver = new DamageResolver();
        
        this.state = new BattleState();
        state.setPrismaticManager(new PrismaticManager(state));
        state.setWeatherController(weatherController);
        state.setDamageAnimationCallback(this);
        
        this.turnProcessor = new TurnProcessor(state);
        turnProcessor.setAIEngine(aiEngine);
        turnProcessor.setWeatherController(weatherController);
        turnProcessor.setDiceRoller(diceRoller);
        turnProcessor.setDamageResolver(damageResolver);
    }
    
    @Override
    public void onDamageAnimationComplete() {
        turnProcessor.onDamageAnimationComplete();
    }

    public void onDamageImpacted() {
        turnProcessor.onDamageImpacted();
    }

    private void configureOpponentPrismaticDefaults(CharacterCard opponentCard) {
        Map<String, Integer> oppPrismatic = opponentCard.getPrismaticDiceIds();
        if (!oppPrismatic.isEmpty()) {
            String defaultPrismaticId = oppPrismatic.keySet().iterator().next();
            CosmiconEventState.setOpponentPrismatic(defaultPrismaticId);

            if (CosmiconStats.isPrismaticDiceUnlocked(defaultPrismaticId)) {
                data.scripts.cosmicon.prismatic.PrismaticDiceType diceType =
                    data.scripts.cosmicon.prismatic.PrismaticDiceRegistry.get(defaultPrismaticId);
                if (diceType != null && diceType.hasTrueVersion()) {
                    CosmiconEventState.setOpponentUsesTrue(true);
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void initBattleWithSelection(boolean playerIsAttacker) {
        CosmiconLogger.info("Initializing battle with player selection");

        CharacterCard playerCard = CosmiconPlayerState.getConfiguredPlayerCard();

        CharacterCard opponentCard;
        int replayGame = CosmiconEventState.getReplayTutorialGame();
        if (replayGame >= 0) {
            String opponentId = replayGame == 1 ? "trashcan" : "robin";
            opponentCard = CharacterRegistry.getCharacterById(opponentId);
            CosmiconEventState.setOpponentCharacter(opponentCard.getId());
            CosmiconLogger.info("Replay tutorial game %d: opponent = %s", replayGame, opponentCard.getName());
        } else if (CosmiconStats.isInTutorialMode()) {
            int gamesPlayed = CosmiconStats.getGamesPlayed();
            if (gamesPlayed == 0) {
                opponentCard = CharacterRegistry.getCharacterById("trashcan");
            } else if (gamesPlayed == 1) {
                opponentCard = CharacterRegistry.getCharacterById("robin");
            } else {
                opponentCard = CharacterRegistry.getRandomOpponent();
            }
            CosmiconEventState.setOpponentCharacter(Objects.requireNonNull(opponentCard).getId());
        } else {
            String oppCharId = CosmiconEventState.getOpponentCharacter();
            if (oppCharId != null) {
                opponentCard = CharacterRegistry.getCharacterById(oppCharId);
            } else {
                opponentCard = CharacterRegistry.getRandomOpponent();
                if (opponentCard != null) {
                    CosmiconEventState.setOpponentCharacter(opponentCard.getId());
                    configureOpponentPrismaticDefaults(opponentCard);
                }
            }
        }

        if (playerCard == null) {
            CosmiconLogger.error("Failed to load character cards for battle");
            throw new IllegalStateException("Failed to load character cards");
        }

        boolean isReplayTutorial = replayGame >= 0;
        boolean isTutorialMode = CosmiconStats.isInTutorialMode();
        if (CosmiconEventState.isCasinoBattleMode() && !isReplayTutorial && !isTutorialMode) {
            String casinoOppId = CosmiconEventState.getCasinoBattleOpponent();
            if (casinoOppId != null) {
                opponentCard = CharacterRegistry.getCharacterById(casinoOppId);
                if (opponentCard == null) {
                    opponentCard = CharacterRegistry.getRandomOpponent();
                }
                CosmiconEventState.setOpponentCharacter(opponentCard.getId());
            }
            int bonusHp = CosmiconEventState.getCasinoBattleBonusHp();
            if (bonusHp > 0 && opponentCard != null) {
                opponentCard = opponentCard.withMaxHp(opponentCard.getMaxHp() + bonusHp);
            }
        }

        if (isGatekeeper999Battle()) {
            state.getWeatherController().getWeatherManager().excludeWeather(WeatherType.TEMPORAL_STORM);
        }

        boolean isTutorial = isTutorialMode || isReplayTutorial;
        TutorialController.TutorialGame tutorialGameType = null;
        if (isTutorial) {
            tutorialGameType = TutorialController.determineTutorialGame();
            String tutorialPlayerId = tutorialGameType == TutorialController.TutorialGame.GAME_1_SPARXIE
                ? "sparxie" : "acheron";
            playerCard = CharacterRegistry.getCharacterById(tutorialPlayerId);
            if (playerCard == null) {
                CosmiconLogger.error("Tutorial character '%s' not found in registry", tutorialPlayerId);
                throw new IllegalStateException("Tutorial character not found: " + tutorialPlayerId);
            }
            CosmiconLogger.info("Tutorial game: forced player character = %s", playerCard.getName());
        }

        if (!isTutorial) {
            String savedPrismaticId = CosmiconPlayerState.loadPrismaticDice();
            String defaultPrismaticId = CosmiconPlayerState.getDefaultPrismaticForCharacter(playerCard.getId());
            boolean useTrueVersion = CosmiconPlayerState.loadPrismaticDiceTrueVersion();
            if (useTrueVersion && savedPrismaticId != null && !CosmiconStats.isPrismaticTrueUnlocked(savedPrismaticId)) {
                useTrueVersion = false;
            }
            boolean playerHasPrismatic = !playerCard.getPrismaticDiceIds().isEmpty();
            boolean playerHasCustomPrismatic = savedPrismaticId != null && !savedPrismaticId.isEmpty();
            boolean repeaterUnlocked = CosmiconStats.isPrismaticDiceUnlocked("repeater");

            if (playerHasPrismatic && playerHasCustomPrismatic
                && !savedPrismaticId.equals(defaultPrismaticId)) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault(savedPrismaticId, 2);
                playerCard = playerCard.withPrismaticDice(savedPrismaticId, uses, useTrueVersion);
                CosmiconLogger.info("Applied custom prismatic dice: %s (true: %b)", savedPrismaticId, useTrueVersion);
            } else if (playerHasPrismatic && repeaterUnlocked && !playerHasCustomPrismatic) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault("repeater", 2);
                playerCard = playerCard.withPrismaticDice("repeater", uses, false);
                CosmiconLogger.info("Applied default repeater prismatic dice (non-true)");
            } else if (playerHasPrismatic && defaultPrismaticId != null) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault(defaultPrismaticId, 2);
                playerCard = playerCard.withPrismaticDice(defaultPrismaticId, uses, useTrueVersion);
            }

            boolean opponentHasPrismatic = !opponentCard.getPrismaticDiceIds().isEmpty();
            boolean opponentUsesTrue = CosmiconEventState.getOpponentUsesTrue();

            if (CosmiconEventState.isCasinoBattleMode() && CosmiconEventState.getCasinoBattleUseTrue()) {
                opponentUsesTrue = true;
            }

            if (opponentHasPrismatic && CosmiconEventState.hasOpponentPrismatic()) {
                String oppPrismId = CosmiconEventState.getOpponentPrismatic();
                int uses = opponentCard.getPrismaticDiceIds().getOrDefault(oppPrismId, 2);
                opponentCard = opponentCard.withPrismaticDice(oppPrismId, uses, opponentUsesTrue);
            } else if (opponentHasPrismatic) {
                Map<String, Integer> oppPrismatic = opponentCard.getPrismaticDiceIds();
                if (!oppPrismatic.isEmpty()) {
                    String oppPrismaticId = oppPrismatic.keySet().iterator().next();
                    opponentCard = opponentCard.withPrismaticDice(oppPrismaticId,
                        oppPrismatic.get(oppPrismaticId), opponentUsesTrue);
                    CosmiconEventState.setOpponentPrismatic(oppPrismaticId);
                }
            }
        } else if (tutorialGameType == TutorialController.TutorialGame.GAME_2_ACHERON) {
            Map<String, Integer> prismaticIds = playerCard.getPrismaticDiceIds();
            if (!prismaticIds.isEmpty()) {
                String primId = prismaticIds.keySet().iterator().next();
                int uses = prismaticIds.get(primId);
                playerCard = playerCard.withPrismaticDice(primId, uses, false);
                CosmiconPlayerState.savePrismaticDice(primId);
                CosmiconPlayerState.savePrismaticDiceTrueVersion(false);
                CosmiconLogger.info("Tutorial Game 2: Enabled prismatic dice %s x%d (basic version)", primId, uses);
            }
        } else {
            CosmiconLogger.info("Tutorial mode: prismatic dice disabled");
        }

        CosmiconLogger.info("Selected characters - Player: %s, Opponent: %s",
            playerCard.getName(), opponentCard.getName());
        CosmiconLogger.info("Player stats - HP: %d, ATK: %d, DEF: %d",
            playerCard.getMaxHp(), playerCard.getAtkLevel(), playerCard.getDefLevel());
        CosmiconLogger.info("Opponent stats - HP: %d, ATK: %d, DEF: %d",
            opponentCard.getMaxHp(), opponentCard.getAtkLevel(), opponentCard.getDefLevel());

        CosmiconLogger.battleStart(playerCard.getName(), opponentCard.getName());

        state.init(playerCard, opponentCard, playerIsAttacker);

        if (isTutorial) {
            tutorialController = new TutorialController(tutorialGameType, state);
            TutorialDiceRoller tutorialDiceRoller = new TutorialDiceRoller(tutorialController);
            turnProcessor.getDiceRoller().setTutorialDiceRoller(tutorialDiceRoller);
            state.setTutorialDiceRoller(tutorialDiceRoller);
            CosmiconLogger.info("Tutorial controller initialized for game: %s", tutorialGameType);

            if (tutorialGameType == TutorialController.TutorialGame.GAME_1_SPARXIE) {
                state.getWeatherController().getWeatherManager().setWeatherDisabled(true);
                CosmiconLogger.info("Tutorial Game 1: Weather disabled");
            } else if (tutorialGameType == TutorialController.TutorialGame.GAME_2_ACHERON) {
                java.util.Map<Integer, WeatherType> forcedWeather = new java.util.HashMap<>();
                forcedWeather.put(2, WeatherType.SLEET);
                state.getWeatherController().getWeatherManager().setForcedWeatherSchedule(forcedWeather);
                CosmiconLogger.info("Tutorial Game 2: Forced SLEET weather at turn 2");
            }
        }

        CosmiconLogger.debug("Battle state initialized");
    }

    public void startBattle() {
        turnProcessor.startBattle();
    }

    public void onPlayerSelectDice(int index) {
        state.selectPlayerDice(index);
    }

    public void onPlayerReroll() {
        turnProcessor.executePlayerReroll();
    }

    public void onPlayerConfirmSelection() {
        turnProcessor.confirmPlayerSelection();
    }

    public void onContinueToNextTurn() {
        turnProcessor.advanceToNextTurn();
    }

    public BattleState getState() {
        return state;
    }

    public TutorialController getTutorialController() {
        return tutorialController;
    }

    public void advanceAiSelection(float amount) {
        turnProcessor.advanceAiSelection(amount);
    }

    public void setDiceRollManager(DiceRollManager manager) {
        turnProcessor.setDiceRollManager(manager);
    }

    public void setOpponentAnimationCompleteChecker(BooleanSupplier checker) {
        turnProcessor.setOpponentAnimationCompleteChecker(checker);
    }

    public void skipAiAnimation() {
        turnProcessor.skipAiAnimation();
    }

    public void advanceToSelectPhase() {
        turnProcessor.advanceToSelectPhase();
    }

    public void advanceToDefenderSelectPhase() {
        turnProcessor.advanceToDefenderSelectPhase();
    }

    public void proceedToModificationPause() {
        turnProcessor.proceedToModificationPause();
    }

    public void proceedFromModificationPause() {
        turnProcessor.proceedFromModificationPause();
    }

    public void advanceFromDiceDisplayAttack() {
        turnProcessor.advanceFromDiceDisplayAttack();
    }

    public void advanceFromDiceDisplayDefense() {
        turnProcessor.advanceFromDiceDisplayDefense();
    }

    public void forcePlayerWin() {
        state.setOpponentHp(0);
        state.setCurrentPhase(TurnState.Phase.ENDED);
        state.setWinner("player");
        state.notifyPhaseChange(TurnState.Phase.ENDED);
        state.notifyBattleEnd("player");
    }

    public boolean isGatekeeperBattle() {
        return CosmiconEventState.isCasinoBattleMode()
            && !CosmiconEventState.isCasinoBattleBoss()
            && CosmiconEventState.getCasinoBattleBonusHp() > 0;
    }

    public boolean isGatekeeper999Battle() {
        return CosmiconEventState.isCasinoBattleMode()
            && !CosmiconEventState.isCasinoBattleBoss()
            && CosmiconEventState.getCasinoBattleBonusHp() == 974;
    }

    public boolean isGatekeeperEarlyExit() {
        return isGatekeeperBattle() && state.getOpponentTotalDamageTaken() >= 99;
    }

    public boolean isGatekeeper999EarlyExit() {
        return isGatekeeper999Battle() && state.getOpponentTotalDamageTaken() >= 99;
    }

    public void preApplyOpponentDamage(int damage) {
        state.preApplyOpponentDamage(damage);
    }

    public void cleanup() {
        currentInstance = null;
        CosmiconLogger.info("========== BATTLE CLEANUP ==========");
        String winner = state.getWinner();
        if (winner != null) {
            CosmiconLogger.info("Final result - Winner: %s", winner);
            CosmiconLogger.info("Final HP - Player: %d, Opponent: %d", 
                state.getPlayerHp(), state.getOpponentHp());
        }
        WeatherController wc = state.getWeatherController();
        state.cleanup();
        if (wc != null) {
            wc.reset();
        }
        CosmiconLogger.info("====================================");
    }
}
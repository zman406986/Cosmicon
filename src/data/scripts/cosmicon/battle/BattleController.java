package data.scripts.cosmicon.battle;

import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.state.BonusState;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.CharacterIds;
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
        CosmiconLogger.debug("Initializing battle with player selection");

        CharacterCard playerCard = CosmiconPlayerState.getConfiguredPlayerCard();

        CharacterCard opponentCard;
        int replayGame = CosmiconEventState.getReplayTutorialGame();
        if (replayGame >= 0) {
            String opponentId = replayGame == 1 ? CharacterIds.TRASHCAN : CharacterIds.ROBIN;
            opponentCard = CharacterRegistry.getCharacterById(opponentId);
            CosmiconEventState.setOpponentCharacter(opponentCard.getId());
            CosmiconLogger.debug("Replay tutorial game %d: opponent = %s", replayGame, opponentCard.getName());
        } else if (CosmiconStats.isInTutorialMode()) {
            opponentCard = CharacterRegistry.getCharacterById(CharacterIds.TRASHCAN);
            CosmiconEventState.setOpponentCharacter(Objects.requireNonNull(opponentCard).getId());
        } else {
            String oppCharId = CosmiconEventState.getOpponentCharacter();
            if (oppCharId != null) {
                opponentCard = CharacterRegistry.getCharacterById(oppCharId);
            } else {
                if (CosmiconStats.isInEasyMode()) {
                    opponentCard = CharacterRegistry.getRandomUnownedTwoStarOpponent(
                        CosmiconStats.getUnlockedCharacters());
                } else {
                    opponentCard = CharacterRegistry.getRandomThreeStarOpponent();
                }
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

        boolean isTutorial = isTutorialMode || isReplayTutorial || CosmiconEventState.isTutorialMode();
        TutorialController.TutorialGame tutorialGameType = null;
        if (isTutorial) {
            tutorialGameType = TutorialController.determineTutorialGame();
            String tutorialPlayerId = tutorialGameType == TutorialController.TutorialGame.GAME_1_CHIMERA
                ? CharacterIds.CHIMERA : "acheron";
            playerCard = CharacterRegistry.getCharacterById(tutorialPlayerId);
            if (playerCard == null) {
                CosmiconLogger.error("Tutorial character '%s' not found in registry", tutorialPlayerId);
                throw new IllegalStateException("Tutorial character not found: " + tutorialPlayerId);
            }
            CosmiconLogger.debug("Tutorial game: forced player character = %s", playerCard.getName());
        }

        if (!isTutorial && (!CosmiconStats.isInEasyMode() || CosmiconEventState.isCasinoBattleMode())) {
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
                CosmiconLogger.debug("Applied custom prismatic dice: %s (true: %b)", savedPrismaticId, useTrueVersion);
            } else if (playerHasPrismatic && repeaterUnlocked && !playerHasCustomPrismatic) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault("repeater", 2);
                playerCard = playerCard.withPrismaticDice("repeater", uses, false);
                CosmiconLogger.debug("Applied default repeater prismatic dice (non-true)");
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
                if (uses <= 0) {
                    uses = 2;
                    CosmiconLogger.warn("Tutorial Game 2: Invalid prismatic uses (%d), defaulting to 2", prismaticIds.get(primId));
                }
                playerCard = playerCard.withPrismaticDice(primId, uses, false);
                CosmiconPlayerState.savePrismaticDice(primId);
                CosmiconPlayerState.savePrismaticDiceTrueVersion(false);
                CosmiconLogger.debug("Tutorial Game 2: Enabled prismatic dice %s x%d (basic version)", primId, uses);
            }
        } else {
            CosmiconLogger.debug("Tutorial mode: prismatic dice disabled");
        }

        CosmiconLogger.info("Selected characters - Player: %s, Opponent: %s",
            playerCard.getName(), opponentCard.getName());
        CosmiconLogger.info("Player stats - HP: %d, ATK: %d, DEF: %d",
            playerCard.getMaxHp(), playerCard.getAtkLevel(), playerCard.getDefLevel());
        CosmiconLogger.info("Opponent stats - HP: %d, ATK: %d, DEF: %d",
            opponentCard.getMaxHp(), opponentCard.getAtkLevel(), opponentCard.getDefLevel());

        CosmiconLogger.battleStart(playerCard.getName(), opponentCard.getName());

        // Apply easy-mode bonus if selected
        if (!isTutorial) {
            BonusState bonus = CosmiconPlayerState.loadBonusSelection(playerCard.getId());
            CosmiconPlayerState.setCreditBonusActive(bonus == BonusState.NONE);

            switch (bonus) {
                case HP_9 -> {
                    int newHp = playerCard.getMaxHp() + 9;
                    playerCard = playerCard.withMaxHp(newHp);
                    CosmiconLogger.debug("Applied HP bonus: +9 HP (now %d)", newHp);
                }
                case ATK_1 -> {
                    int currentAtk = playerCard.getAtkLevel();
                    if (currentAtk < 5) {
                        playerCard = playerCard.withAtkLevel(currentAtk + 1);
                        CosmiconLogger.debug("Applied ATK bonus: %d -> %d", currentAtk, currentAtk + 1);
                    } else {
                        CosmiconLogger.debug("ATK bonus skipped: already at cap 5");
                    }
                }
                case DEF_1 -> {
                    int currentDef = playerCard.getDefLevel();
                    if (currentDef < 5) {
                        playerCard = playerCard.withDefLevel(currentDef + 1);
                        CosmiconLogger.debug("Applied DEF bonus: %d -> %d", currentDef, currentDef + 1);
                    } else {
                        CosmiconLogger.debug("DEF bonus skipped: already at cap 5");
                    }
                }
                case NONE -> CosmiconLogger.debug("No bonus selected - credit bonus will apply on win");
            }
        } else {
            CosmiconPlayerState.setCreditBonusActive(false);
        }

        state.init(playerCard, opponentCard, playerIsAttacker);

        if (isTutorial) {
            tutorialController = new TutorialController(tutorialGameType, state);
            TutorialDiceRoller tutorialDiceRoller = new TutorialDiceRoller(tutorialController);
            turnProcessor.getDiceRoller().setTutorialDiceRoller(tutorialDiceRoller);
            state.setTutorialDiceRoller(tutorialDiceRoller);
            CosmiconLogger.debug("Tutorial controller initialized for game: %s", tutorialGameType);

            if (tutorialGameType == TutorialController.TutorialGame.GAME_1_CHIMERA) {
                state.getWeatherController().getWeatherManager().setWeatherDisabled(true);
                CosmiconLogger.debug("Tutorial Game 1: Weather disabled");
            } else if (tutorialGameType == TutorialController.TutorialGame.GAME_2_ACHERON) {
                java.util.Map<Integer, WeatherType> forcedWeather = new java.util.HashMap<>();
                forcedWeather.put(2, WeatherType.SLEET);
                state.getWeatherController().getWeatherManager().setForcedWeatherSchedule(forcedWeather);
                CosmiconLogger.debug("Tutorial Game 2: Forced SLEET weather at turn 2");
            }
        }

        if (CosmiconStats.isInEasyMode() && !CosmiconEventState.isCasinoBattleMode()) {
            state.getWeatherController().getWeatherManager().setWeatherDisabled(true);
            CosmiconLogger.debug("Easy mode: Weather disabled (normal encounter)");
        }

        CosmiconLogger.verbose("Battle state initialized");
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
package data.scripts.cosmicon.battle;

import java.util.Map;

import data.scripts.cosmicon.ai.DiceProbabilityCalculator;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CosmiconLogger;

public class BattleController implements BattleState.DamageAnimationCallback {

    private final BattleState state;
    private final TurnProcessor turnProcessor;

    public BattleController() {
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

    public void initBattleWithSelection() {
        initBattleWithSelection(true);
    }

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
            CosmiconEventState.setOpponentCharacter(opponentCard.getId());
        } else if (CosmiconEventState.isBarEvent()) {
            opponentCard = CharacterRegistry.getCharacterById(CosmiconEventState.getOpponentCharacter());
        } else {
            opponentCard = CharacterRegistry.getRandomOpponent();
            CosmiconEventState.setOpponentCharacter(opponentCard.getId());
        }

        if (playerCard == null || opponentCard == null) {
            CosmiconLogger.error("Failed to load character cards for battle");
            throw new IllegalStateException("Failed to load character cards");
        }

        boolean isTutorial = CosmiconEventState.isTutorialMode() || CosmiconStats.isInTutorialMode();

        if (!isTutorial) {
            String savedPrismaticId = CosmiconPlayerState.loadPrismaticDice();
            String defaultPrismaticId = CosmiconPlayerState.getDefaultPrismaticForCharacter(playerCard.getId());
            boolean useTrueVersion = CosmiconPlayerState.loadPrismaticDiceTrueVersion();

            if (savedPrismaticId != null && !savedPrismaticId.isEmpty()
                && !savedPrismaticId.equals(defaultPrismaticId)) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault(savedPrismaticId, 2);
                playerCard = playerCard.withPrismaticDice(savedPrismaticId, uses, useTrueVersion);
                CosmiconLogger.info("Applied custom prismatic dice: %s (true: %b)", savedPrismaticId, useTrueVersion);
            } else if (defaultPrismaticId != null) {
                int uses = playerCard.getPrismaticDiceIds().getOrDefault(defaultPrismaticId, 2);
                playerCard = playerCard.withPrismaticDice(defaultPrismaticId, uses, useTrueVersion);
            }

            Map<String, Integer> oppPrismatic = opponentCard.getPrismaticDiceIds();
            if (!oppPrismatic.isEmpty()) {
                String oppPrismaticId = oppPrismatic.keySet().iterator().next();
                CosmiconEventState.setOpponentPrismatic(oppPrismaticId);
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

        CosmiconLogger.debug("Battle state initialized, starting turn processor");
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

    public void advanceAiSelection(float amount) {
        turnProcessor.advanceAiSelection(amount);
    }

    public void advanceToSelectPhase() {
        turnProcessor.advanceToSelectPhase();
    }

    public void advanceToDefenderSelectPhase() {
        turnProcessor.advanceToDefenderSelectPhase();
    }

    public void proceedToClash() {
        turnProcessor.proceedToClash();
    }

    public void cleanup() {
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
        DiceProbabilityCalculator.clearCache();
        CosmiconSprites.clearCache();
        CosmiconLogger.info("====================================");
    }
}
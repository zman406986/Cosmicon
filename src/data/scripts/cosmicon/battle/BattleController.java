package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.ai.DiceProbabilityCalculator;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.util.CosmiconLogger;

public class BattleController {

    private final BattleState state;
    private final TurnProcessor turnProcessor;

    public BattleController() {
        EffectManager effectManager = new EffectManager();
        WeatherController weatherController = new WeatherController();
        PrismaticManager prismaticManager = new PrismaticManager(effectManager);

        AIEngine aiEngine = new AIEngine();
        DiceRoller diceRoller = new DiceRoller(weatherController);
        DamageResolver damageResolver = new DamageResolver();
        
        this.state = new BattleState();
        state.setEffectManager(effectManager);
        state.setPrismaticManager(prismaticManager);
        state.setWeatherController(weatherController);
        
        this.turnProcessor = new TurnProcessor(state);
        turnProcessor.setAIEngine(aiEngine);
        turnProcessor.setWeatherController(weatherController);
        turnProcessor.setDiceRoller(diceRoller);
        turnProcessor.setDamageResolver(damageResolver);
    }

    public void initRandomBattle() {
        CosmiconLogger.info("Initializing random battle");
        
        CharacterCard playerCard = CharacterRegistry.getRandomCharacter();
        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
        
        if (playerCard == null || opponentCard == null) {
            CosmiconLogger.error("Failed to load character cards for battle");
            throw new IllegalStateException("Failed to load character cards");
        }
        
        CosmiconLogger.info("Selected characters - Player: %s, Opponent: %s", 
            playerCard.getName(), opponentCard.getName());
        CosmiconLogger.info("Player stats - HP: %d, ATK: %d, DEF: %d", 
            playerCard.getMaxHp(), playerCard.getAtkLevel(), playerCard.getDefLevel());
        CosmiconLogger.info("Opponent stats - HP: %d, ATK: %d, DEF: %d", 
            opponentCard.getMaxHp(), opponentCard.getAtkLevel(), opponentCard.getDefLevel());
        
        CosmiconLogger.battleStart(playerCard.getName(), opponentCard.getName());
        
        state.init(playerCard, opponentCard);
        
        CosmiconLogger.debug("Battle state initialized, starting turn processor");
        turnProcessor.startBattle();
    }

    public void onPlayerSelectDice(int index) {
        state.selectPlayerDice(index);
    }

    public void onPlayerReroll() {
        turnProcessor.confirmPlayerSelection();
    }

    public void onPlayerSkipReroll() {
        turnProcessor.skipRerollPhase();
    }

    public void onPlayerConfirmSelection() {
        turnProcessor.confirmPlayerSelection();
    }

    public void onContinueToNextTurn() {
        turnProcessor.advanceToNextTurn();
    }

    public void onTogglePrismaticMode() {
        state.togglePlayerPrismaticMode();
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

    public void advanceToAttackPhase() {
        turnProcessor.advanceToAttackPhase();
    }

    public void advanceToDefenderSelectPhase() {
        turnProcessor.advanceToDefenderSelectPhase();
    }

    public void cleanup() {
        CosmiconLogger.info("========== BATTLE CLEANUP ==========");
        String winner = state.getWinner();
        if (winner != null) {
            CosmiconLogger.info("Final result - Winner: %s", winner);
            CosmiconLogger.info("Final HP - Player: %d, Opponent: %d", 
                state.getPlayerHp(), state.getOpponentHp());
        }
        state.cleanup();
        DiceProbabilityCalculator.clearCache();
        CosmiconLogger.info("====================================");
    }
}
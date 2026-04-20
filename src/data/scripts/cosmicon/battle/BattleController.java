package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.ai.DiceProbabilityCalculator;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticManager;

public class BattleController {

    private final BattleState state;
    private final TurnProcessor turnProcessor;
    private final PrismaticManager prismaticManager;

    public BattleController() {
        EffectManager effectManager = new EffectManager();
        WeatherController weatherController = new WeatherController();
        this.prismaticManager = new PrismaticManager(effectManager);

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
        CharacterCard playerCard = CharacterRegistry.getRandomCharacter();
        CharacterCard opponentCard = CharacterRegistry.getRandomOpponent();
        
        state.init(playerCard, opponentCard);
        
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

    public void onSelectPrismaticType(PrismaticDiceType type, boolean trueVersion) {
        state.setPlayerSelectedPrismaticType(type, trueVersion);
        state.rollPrismaticDice(true);
    }

    public BattleState getState() {
        return state;
    }

    public PrismaticManager getPrismaticManager() {
        return prismaticManager;
    }

    public BattleState.Phase getCurrentPhase() {
        return state.getCurrentPhase();
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

    public void cleanup() {
        state.cleanup();
        DiceProbabilityCalculator.clearCache();
    }
}
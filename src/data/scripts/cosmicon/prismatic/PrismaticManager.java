package data.scripts.cosmicon.prismatic;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import data.scripts.cosmicon.util.CosmiconLogger;

public class PrismaticManager {
    
    private final PrismaticDiceProcessor processor;
    private final BattleState battleState;
    
    private final PrismaticState playerPrismatic;
    private final PrismaticState opponentPrismatic;
    
    public PrismaticManager(BattleState battleState) {
        this.battleState = battleState;
        this.processor = new PrismaticDiceProcessor();
        this.playerPrismatic = new PrismaticState();
        this.opponentPrismatic = new PrismaticState();
        initializePrismaticUses();
    }
    
    private void initializePrismaticUses() {
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            playerPrismatic.setUsesByType(type, 2);
            opponentPrismatic.setUsesByType(type, 2);
        }
        playerPrismatic.setUses(2);
        opponentPrismatic.setUses(2);
    }
    
    public List<PrismaticDiceType> getAvailable(boolean forPlayer, BattleState state) {
        PrismaticState ps = getState(forPlayer);
        ConditionContext context = createConditionContext(state, forPlayer);
        
        return PrismaticDiceRegistry.getAll().values().stream()
            .filter(type -> ps.getUsesByType(type) > 0)
            .filter(type -> type.isAvailable(context))
            .collect(Collectors.toList());
    }
    
    public void applyQueuedEffects(BattleState state) {
        applyQueuedEffectsFor(state, true);
        applyQueuedEffectsFor(state, false);
    }
    
    private void applyQueuedEffectsFor(BattleState state, boolean forPlayer) {
        List<PrismaticDiceInstance> selectedDice = state.getSelectedPrismaticDice(forPlayer);
        
        for (PrismaticDiceInstance dice : selectedDice) {
            processor.applyEffect(dice, state, forPlayer);
            consumePrismaticUse(dice.type, forPlayer);
        }
    }
    
    public void consumePrismaticUse(PrismaticDiceType type, boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        int oldUses = ps.getUsesByType(type);
        ps.decrementUsesByType(type);
        ps.decrementUses();
        int newUses = ps.getUsesByType(type);
        CosmiconLogger.debug("Prismatic dice consumed: %s by %s (uses: %d -> %d)", 
            type.getId(), forPlayer ? "Player" : "Opponent", oldUses, newUses);
    }
    
    public void addPrismaticUse(PrismaticDiceType type, boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        ps.incrementUsesByType(type);
        ps.incrementUses();
    }
    
    public int getUses(boolean forPlayer) {
        return getState(forPlayer).getUses();
    }
    
    public int getUsesByType(PrismaticDiceType type, boolean forPlayer) {
        return getState(forPlayer).getUsesByType(type);
    }
    
    public boolean hasMustSelectDiceRemaining(boolean forPlayer) {
        for (PrismaticDiceInstance dice : battleState.getMustSelectPrismaticDice(forPlayer)) {
            if (!dice.isSelected()) return true;
        }
        return false;
    }
    
    public boolean canConfirmPrismaticSelection(boolean forPlayer) {
        return !hasMustSelectDiceRemaining(forPlayer);
    }
    
    public int getPrismaticSelectedSum(boolean forPlayer) {
        int sum = 0;
        for (PrismaticDiceInstance dice : battleState.getSelectedPrismaticDice(forPlayer)) {
            sum += dice.rolledFace;
        }
        return sum;
    }
    
    public int calculateTotalValue(boolean forPlayer, boolean includeDoubleValue) {
        int baseValue = getPrismaticSelectedSum(forPlayer);
        PrismaticState ps = getState(forPlayer);
        
        if (includeDoubleValue && ps.isDoubleValueActive()) {
            return baseValue * 2;
        }
        return baseValue;
    }
    
    public void setDoubleValueActive(boolean forPlayer, boolean active) {
        getState(forPlayer).setDoubleValueActive(active);
    }
    
    public int getInstantDamage(boolean forPlayer) {
        return getState(forPlayer).getInstantDamage();
    }
    
    public void addInstantDamage(boolean forPlayer, int amount) {
        getState(forPlayer).addInstantDamage(amount);
    }
    
    public boolean isModeActive(boolean forPlayer) {
        return getState(forPlayer).isModeActive();
    }
    
    public void clearState() {
        playerPrismatic.clear();
        opponentPrismatic.clear();
    }
    
    public void resetForNewBattle() {
        playerPrismatic.reset();
        opponentPrismatic.reset();
        initializePrismaticUses();
        CosmiconLogger.debug("Prismatic state reset for new battle");
    }
    
    private PrismaticState getState(boolean forPlayer) {
        return forPlayer ? playerPrismatic : opponentPrismatic;
    }
    
    private ConditionContext createConditionContext(BattleState state, boolean forPlayer) {
        int hp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
        CharacterCard card = forPlayer ? state.getPlayerCard() : state.getOpponentCard();
        int maxHp = card != null ? card.getMaxHp() : hp;
        int turnNumber = state.getTurnNumber();
        BattleState.TurnType turnType = state.isPlayerAttacker() 
            ? (forPlayer ? BattleState.TurnType.ATTACK : BattleState.TurnType.DEFENSE)
            : (forPlayer ? BattleState.TurnType.DEFENSE : BattleState.TurnType.ATTACK);
        int dmgTaken = forPlayer ? state.getPlayerTotalDamageTaken() : state.getOpponentTotalDamageTaken();
        Map<Integer, Integer> history = forPlayer ? state.getPlayerFaceSelectionHistory() : state.getOpponentFaceSelectionHistory();
        
        return new ConditionContext(hp, maxHp, turnNumber, turnType, dmgTaken, history);
    }
}
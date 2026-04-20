package data.scripts.cosmicon.prismatic;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.EffectManager;
import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import data.scripts.cosmicon.util.CosmiconRandom;

public class PrismaticManager {
    
    private final PrismaticDiceProcessor processor;
    private final EffectManager effectManager;
    
    private final PrismaticState playerPrismatic;
    private final PrismaticState opponentPrismatic;
    
    public PrismaticManager(EffectManager effectManager) {
        this.effectManager = effectManager;
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
    
    public void rollPrismaticDice(boolean forPlayer, PrismaticDiceType type, boolean trueVersion) {
        PrismaticState ps = getState(forPlayer);
        ps.setSelectedType(type);
        ps.setUseTrueVersion(trueVersion);
        
        PrismaticDiceInstance instance = PrismaticDiceInstance.roll(type, trueVersion, CosmiconRandom.getRandom());
        ps.addRolledDice(instance);
        
        processor.checkDestinedDice(instance);
    }
    
    public void applyQueuedEffects(BattleState state) {
        applyQueuedEffectsFor(state, true);
        applyQueuedEffectsFor(state, false);
    }
    
    private void applyQueuedEffectsFor(BattleState state, boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        List<PrismaticDiceInstance> selectedDice = ps.getSelectedDice();
        
        for (PrismaticDiceInstance dice : selectedDice) {
            processor.applyEffect(dice, state, effectManager, forPlayer);
            consumePrismaticUse(dice.type, forPlayer);
        }
    }
    
    public void consumePrismaticUse(PrismaticDiceType type, boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        ps.decrementUsesByType(type);
        ps.decrementUses();
    }
    
    public void addPrismaticUse(PrismaticDiceType type, boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        ps.incrementUsesByType(type);
        ps.incrementUses();
    }
    
    public int getUses(boolean forPlayer) {
        return getState(forPlayer).getUses();
    }
    
    public List<PrismaticDiceInstance> getRolledDice(boolean forPlayer) {
        return getState(forPlayer).getRolledDice();
    }
    
    public List<PrismaticDiceInstance> getMustSelectDice(boolean forPlayer) {
        return getState(forPlayer).getMustSelectDice();
    }
    
    public boolean hasMustSelectDiceRemaining(boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        for (PrismaticDiceInstance dice : ps.getMustSelectDice()) {
            if (!dice.isSelected()) return true;
        }
        return false;
    }
    
    public boolean canConfirmPrismaticSelection(boolean forPlayer) {
        return !hasMustSelectDiceRemaining(forPlayer);
    }
    
    public int getPrismaticSelectedSum(boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        int sum = 0;
        for (PrismaticDiceInstance dice : ps.getRolledDice()) {
            if (dice.isSelected()) {
                sum += dice.rolledFace;
            }
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
    
    public void toggleMode(boolean forPlayer) {
        PrismaticState ps = getState(forPlayer);
        ps.setModeActive(!ps.isModeActive());
    }
    
    public PrismaticDiceType getSelectedType(boolean forPlayer) {
        return getState(forPlayer).getSelectedType();
    }
    
    public boolean isUseTrueVersion(boolean forPlayer) {
        return getState(forPlayer).isUseTrueVersion();
    }
    
    public void setSelectedType(boolean forPlayer, PrismaticDiceType type, boolean trueVersion) {
        PrismaticState ps = getState(forPlayer);
        ps.setSelectedType(type);
        ps.setUseTrueVersion(trueVersion);
    }
    
    public void clearState() {
        playerPrismatic.clear();
        opponentPrismatic.clear();
    }
    
    public void clearRolledDice() {
        playerPrismatic.clearRolledDice();
        opponentPrismatic.clearRolledDice();
    }
    
    public void resetForNewBattle() {
        playerPrismatic.reset();
        opponentPrismatic.reset();
        initializePrismaticUses();
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
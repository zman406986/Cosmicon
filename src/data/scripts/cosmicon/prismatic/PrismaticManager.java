package data.scripts.cosmicon.prismatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.EffectManager;
import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;

public class PrismaticManager {
    
    private static final Random random = new Random();
    
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
    
    public boolean canUsePrismaticDice(PrismaticDiceType type, boolean forPlayer, BattleState state) {
        PrismaticState ps = getState(forPlayer);
        if (ps.getUsesByType(type) <= 0) return false;
        
        ConditionContext context = createConditionContext(state, forPlayer);
        return type.isAvailable(context);
    }
    
    public void rollPrismaticDice(boolean forPlayer, PrismaticDiceType type, boolean trueVersion) {
        PrismaticState ps = getState(forPlayer);
        ps.setSelectedType(type);
        ps.setUseTrueVersion(trueVersion);
        
        PrismaticDiceInstance instance = PrismaticDiceInstance.roll(type, trueVersion, random);
        ps.addRolledDice(instance);
        
        processor.checkDestinedDice(instance, forPlayer);
    }
    
    public boolean selectPrismaticDice(boolean forPlayer, int index) {
        PrismaticState ps = getState(forPlayer);
        return ps.selectDice(index);
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
    
    public int getUsesByType(PrismaticDiceType type, boolean forPlayer) {
        return getState(forPlayer).getUsesByType(type);
    }
    
    public List<PrismaticDiceInstance> getRolledDice(boolean forPlayer) {
        return getState(forPlayer).getRolledDice();
    }
    
    public Set<Integer> getSelectedIndices(boolean forPlayer) {
        return getState(forPlayer).getSelectedIndices();
    }
    
    public List<PrismaticDiceInstance> getSelectedDice(boolean forPlayer) {
        return getState(forPlayer).getSelectedDice();
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
    
    public boolean isDoubleValueActive(boolean forPlayer) {
        return getState(forPlayer).isDoubleValueActive();
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
    
    public void setModeActive(boolean forPlayer, boolean active) {
        getState(forPlayer).setModeActive(active);
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
    
    public static class PrismaticState {
        private int uses;
        private Map<PrismaticDiceType, Integer> usesByType;
        private List<PrismaticDiceInstance> rolledDice;
        private Set<Integer> selectedIndices;
        private boolean modeActive;
        private PrismaticDiceType selectedType;
        private boolean useTrueVersion;
        private boolean doubleValueActive;
        private int instantDamage;
        private List<PrismaticDiceInstance> mustSelectDice;
        
        public PrismaticState() {
            this.uses = 2;
            this.usesByType = new HashMap<>();
            this.rolledDice = new ArrayList<>();
            this.selectedIndices = new HashSet<>();
            this.modeActive = false;
            this.selectedType = null;
            this.useTrueVersion = false;
            this.doubleValueActive = false;
            this.instantDamage = 0;
            this.mustSelectDice = new ArrayList<>();
        }
        
        public int getUses() { return uses; }
        public void setUses(int uses) { this.uses = uses; }
        public void decrementUses() { this.uses = Math.max(0, uses - 1); }
        public void incrementUses() { this.uses++; }
        
        public int getUsesByType(PrismaticDiceType type) { 
            return usesByType.getOrDefault(type, 0); 
        }
        public void setUsesByType(PrismaticDiceType type, int count) { 
            usesByType.put(type, count); 
        }
        public void decrementUsesByType(PrismaticDiceType type) {
            int current = usesByType.getOrDefault(type, 0);
            if (current > 0) usesByType.put(type, current - 1);
        }
        public void incrementUsesByType(PrismaticDiceType type) {
            usesByType.merge(type, 1, Integer::sum);
        }
        
        public List<PrismaticDiceInstance> getRolledDice() { return rolledDice; }
        public void addRolledDice(PrismaticDiceInstance dice) {
            rolledDice.add(dice);
            if (dice.isMustSelect()) {
                mustSelectDice.add(dice);
            }
        }
        
        public Set<Integer> getSelectedIndices() { return selectedIndices; }
        
        public List<PrismaticDiceInstance> getSelectedDice() {
            List<PrismaticDiceInstance> selected = new ArrayList<>();
            for (int i = 0; i < rolledDice.size(); i++) {
                if (rolledDice.get(i).isSelected()) {
                    selected.add(rolledDice.get(i));
                }
            }
            return selected;
        }
        
        public List<PrismaticDiceInstance> getMustSelectDice() { return mustSelectDice; }
        
        public boolean selectDice(int index) {
            if (index < 0 || index >= rolledDice.size()) return false;
            
            PrismaticDiceInstance dice = rolledDice.get(index);
            boolean newState = !dice.isSelected();
            dice.setSelected(newState);
            
            if (newState) {
                selectedIndices.add(index);
            } else {
                selectedIndices.remove(index);
            }
            
            return true;
        }
        
        public boolean isModeActive() { return modeActive; }
        public void setModeActive(boolean active) { this.modeActive = active; }
        
        public PrismaticDiceType getSelectedType() { return selectedType; }
        public void setSelectedType(PrismaticDiceType type) { this.selectedType = type; }
        
        public boolean isUseTrueVersion() { return useTrueVersion; }
        public void setUseTrueVersion(boolean trueVersion) { this.useTrueVersion = trueVersion; }
        
        public boolean isDoubleValueActive() { return doubleValueActive; }
        public void setDoubleValueActive(boolean active) { this.doubleValueActive = active; }
        
        public int getInstantDamage() { return instantDamage; }
        public void addInstantDamage(int amount) { this.instantDamage += amount; }
        
        public void clear() {
            rolledDice.clear();
            selectedIndices.clear();
            mustSelectDice.clear();
            selectedType = null;
            useTrueVersion = false;
            doubleValueActive = false;
            instantDamage = 0;
        }
        
        public void clearRolledDice() {
            rolledDice.clear();
            selectedIndices.clear();
            mustSelectDice.clear();
        }
        
        public void reset() {
            clear();
            modeActive = false;
        }
    }
}
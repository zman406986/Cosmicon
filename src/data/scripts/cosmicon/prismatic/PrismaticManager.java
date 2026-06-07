package data.scripts.cosmicon.prismatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.TurnState;
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
    }
    
    private void initializePrismaticUses() {
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            playerPrismatic.setUsesByType(type, 0);
            opponentPrismatic.setUsesByType(type, 0);
        }
        playerPrismatic.setUses(0);
        opponentPrismatic.setUses(0);
    }
    
    public void initializeFromCards(CharacterCard playerCard, CharacterCard opponentCard) {
        initializePrismaticUses();
        
        if (playerCard != null) {
            java.util.Map<String, Integer> playerPrismaticDice = playerCard.getPrismaticDiceIds();
            for (java.util.Map.Entry<String, Integer> entry : playerPrismaticDice.entrySet()) {
                PrismaticDiceType type = PrismaticDiceRegistry.get(entry.getKey());
                if (type != null) {
                    int uses = entry.getValue();
                    playerPrismatic.setUsesByType(type, uses);
                    playerPrismatic.setUses(playerPrismatic.getUses() + uses);
                }
            }
        }
        
        if (opponentCard != null) {
            java.util.Map<String, Integer> opponentPrismaticDice = opponentCard.getPrismaticDiceIds();
            for (java.util.Map.Entry<String, Integer> entry : opponentPrismaticDice.entrySet()) {
                PrismaticDiceType type = PrismaticDiceRegistry.get(entry.getKey());
                if (type != null) {
                    int uses = entry.getValue();
                    opponentPrismatic.setUsesByType(type, uses);
                    opponentPrismatic.setUses(opponentPrismatic.getUses() + uses);
                }
            }
        }
        
        CosmiconLogger.debug("Prismatic uses initialized - Player: %d, Opponent: %d", 
            playerPrismatic.getUses(), opponentPrismatic.getUses());
    }
    
    public List<PrismaticDiceType> getAvailable(boolean forPlayer, BattleState state) {
        PrismaticState ps = getState(forPlayer);
        ConditionContext context = createConditionContext(state, forPlayer);
        CharacterCard card = state.getCard(forPlayer);
        boolean useTrueVersion = card != null && card.isUseTruePrismatic();
        
        List<PrismaticDiceType> available = new ArrayList<>();
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            if (ps.getUsesByType(type) > 0 && type.isAvailable(context, useTrueVersion)) {
                available.add(type);
            }
        }
        return available;
    }
    
    public void applyQueuedEffects(BattleState state) {
        applyQueuedEffectsFor(state, true);
        applyQueuedEffectsFor(state, false);
    }
    
    private void applyQueuedEffectsFor(BattleState state, boolean forPlayer) {
        List<PrismaticDiceInstance> selectedDice = state.getSelectedPrismaticDice(forPlayer);
        
        for (PrismaticDiceInstance dice : selectedDice) {
            processor.applyEffect(dice, state, forPlayer);
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
        if (type != null) {
            ps.incrementUsesByType(type);
        }
        ps.incrementUses();
    }
    
    public void addPrismaticUse(PrismaticDiceType type, boolean forPlayer, int amount) {
        if (amount <= 0) return;
        PrismaticState ps = getState(forPlayer);
        if (type != null) {
            ps.incrementUsesByType(type, amount);
        }
        ps.addUses(amount);
    }
    
    public int getUses(boolean forPlayer) {
        return getState(forPlayer).getUses();
    }
    
    public int getUsesByType(PrismaticDiceType type, boolean forPlayer) {
        return getState(forPlayer).getUsesByType(type);
    }
    
    public boolean hasMustSelectDiceRemaining(boolean forPlayer) {
        Map<Integer, PrismaticDiceInstance> map = battleState.getPrismaticDiceMap(forPlayer);
        List<Boolean> selected = battleState.getDiceSelected(forPlayer);
        
        for (Map.Entry<Integer, PrismaticDiceInstance> entry : map.entrySet()) {
            int idx = entry.getKey();
            if (entry.getValue().isMustSelect() && idx < selected.size() && !selected.get(idx)) {
                return true;
            }
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
    
    public void clearState() {
        playerPrismatic.clear();
        opponentPrismatic.clear();
    }
    
    private PrismaticState getState(boolean forPlayer) {
        return forPlayer ? playerPrismatic : opponentPrismatic;
    }
    
    private ConditionContext createConditionContext(BattleState state, boolean forPlayer) {
        int hp = forPlayer ? state.getPlayerHp() : state.getOpponentHp();
        CharacterCard card = forPlayer ? state.getPlayerCard() : state.getOpponentCard();
        int maxHp = card != null ? card.getMaxHp() : hp;
        int turnNumber = state.getTurnNumber();
        TurnState.TurnType turnType = state.isPlayerAttacker() 
            ? (forPlayer ? TurnState.TurnType.ATTACK : TurnState.TurnType.DEFENSE)
            : (forPlayer ? TurnState.TurnType.DEFENSE : TurnState.TurnType.ATTACK);
        int dmgTaken = forPlayer ? state.getPlayerTotalDamageTaken() : state.getOpponentTotalDamageTaken();
        Map<Integer, Integer> history = forPlayer ? state.getPlayerFaceSelectionHistory() : state.getOpponentFaceSelectionHistory();
        
        return new ConditionContext(hp, maxHp, turnNumber, turnType, dmgTaken, history);
    }
}
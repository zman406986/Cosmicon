package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticManager;

public class BattleState {

    public enum Phase {
        ROLLING,
        REROLL_PHASE,
        SELECTING_ATTACK,
        SELECTING_DEFENSE,
        RESOLVING,
        WAITING_NEXT_TURN,
        ENDED
    }

    public enum TurnType {
        ATTACK,
        DEFENSE
    }

    private EffectManager effectManager;
    private PrismaticManager prismaticManager;
    private WeatherController weatherController;
    private DiceRoller diceRoller;

    private CharacterCard playerCard;
    private CharacterCard opponentCard;
    private DicePoolCounts playerDicePoolCounts;
    private DicePoolCounts opponentDicePoolCounts;

    private int playerHp;
    private int opponentHp;

    private int turnNumber;
    private boolean playerIsAttacker;
    private Phase currentPhase;

    private int playerTotalDamageTaken;
    private int opponentTotalDamageTaken;
    private final Map<Integer, Integer> playerFaceSelectionHistory;
    private final Map<Integer, Integer> opponentFaceSelectionHistory;
    
    private int playerCumulativeAtkDef;
    private int opponentCumulativeAtkDef;
    private boolean playerCyreneThresholdMet;
    private boolean opponentCyreneThresholdMet;
    
    private int playerPrismaticTriggerCount;
    private int opponentPrismaticTriggerCount;

    private int playerRemainingRerolls;
    private int opponentRemainingRerolls;
    private int playerRerollsUsedThisTurn;
    private int opponentRerollsUsedThisTurn;

    private List<DiceType> playerDiceTypes;
    private List<Integer> playerDiceValues;
    private List<Boolean> playerDiceSelected;

    private List<DiceType> opponentDiceTypes;
    private List<Integer> opponentDiceValues;
    private List<Boolean> opponentDiceSelected;

    private int attackValue;
    private int defenseValue;
    private int lastDamageDealt;

    private String winner;

    private final List<BattleEventListener> listeners;

    public interface BattleEventListener {
        void onPhaseChange(Phase newPhase);
        void onDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices);
        void onBattleEnd(String winner);
        void onDamageResolved(int damage, int playerHp, int opponentHp);
        void onDiceSelected(boolean isPlayer, int index, boolean selected);
        void onPrismaticDiceRolled(boolean isPlayer, List<PrismaticDiceInstance> dice);
        void onPrismaticDiceSelected(boolean isPlayer, int index, boolean selected);
        void onMustSelectDiceMarked(boolean isPlayer, List<PrismaticDiceInstance> mustSelect);
        void onDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values);
        void onWeatherChange(WeatherType newWeather);
    }

    public BattleState() {
        this.listeners = new ArrayList<>();
        this.turnNumber = 1;
        this.playerIsAttacker = true;
        this.currentPhase = Phase.WAITING_NEXT_TURN;
        this.playerRemainingRerolls = 0;
        this.opponentRemainingRerolls = 0;
        this.playerRerollsUsedThisTurn = 0;
        this.opponentRerollsUsedThisTurn = 0;
        
        this.effectManager = new EffectManager();
        this.prismaticManager = new PrismaticManager(effectManager);
        
        this.playerTotalDamageTaken = 0;
        this.opponentTotalDamageTaken = 0;
        this.playerFaceSelectionHistory = new HashMap<>();
        this.opponentFaceSelectionHistory = new HashMap<>();
        this.playerCumulativeAtkDef = 0;
        this.opponentCumulativeAtkDef = 0;
        this.playerCyreneThresholdMet = false;
        this.opponentCyreneThresholdMet = false;
    }

    
    
    public void init(CharacterCard playerCard, CharacterCard opponentCard) {
        this.playerCard = playerCard;
        this.opponentCard = opponentCard;
        this.playerDicePoolCounts = DicePoolCounts.fromPool(playerCard.getDicePool());
        this.opponentDicePoolCounts = DicePoolCounts.fromPool(opponentCard.getDicePool());
        resetBattleState(playerCard.getMaxHp(), opponentCard.getMaxHp());
        playerCumulativeAtkDef = 0;
        opponentCumulativeAtkDef = 0;
        playerCyreneThresholdMet = false;
        opponentCyreneThresholdMet = false;
        if (prismaticManager != null) prismaticManager.resetForNewBattle();
    }
    
    private void resetBattleState(int playerMaxHp, int opponentMaxHp) {
        playerHp = playerMaxHp;
        opponentHp = opponentMaxHp;
        turnNumber = 1;
        playerIsAttacker = true;
        currentPhase = Phase.WAITING_NEXT_TURN;
        winner = null;
        playerTotalDamageTaken = 0;
        opponentTotalDamageTaken = 0;
        playerFaceSelectionHistory.clear();
        opponentFaceSelectionHistory.clear();
    }
    
    public void setPrismaticManager(PrismaticManager manager) {
        this.prismaticManager = manager;
    }
    
    public void setEffectManager(EffectManager manager) {
        this.effectManager = manager;
    }

    public void setWeatherController(WeatherController controller) {
        this.weatherController = controller;
    }

    public void addListener(BattleEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BattleEventListener listener) {
        listeners.remove(listener);
    }

    public void selectPlayerDice(int index) {
        if (currentPhase != Phase.SELECTING_ATTACK && currentPhase != Phase.SELECTING_DEFENSE && currentPhase != Phase.REROLL_PHASE) return;
        if (index < 0 || index >= playerDiceValues.size()) return;
        playerDiceSelected.set(index, !playerDiceSelected.get(index));
    }

    public int getRemainingRerolls() {
        return playerRemainingRerolls;
    }

    public int getRerollsUsedThisTurn() {
        return playerRerollsUsedThisTurn;
    }

    

    public int getRequiredDiceCount(boolean isPlayer) {
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        return (isPlayer == playerIsAttacker) ? card.getAtkLevel() : card.getDefLevel();
    }

    public int countSelected(List<Boolean> selected) {
        int count = 0;
        for (Boolean b : selected) if (b) count++;
        return count;
    }

    public int calculateSelectedSum(List<Integer> values, List<Boolean> selected) {
        int sum = 0;
        for (int i = 0; i < values.size(); i++) if (selected.get(i)) sum += values.get(i);
        return sum;
    }

    public StatusEffectProcessor.BattleContext createBattleContext(boolean isPlayer) {
        int hp = isPlayer ? playerHp : opponentHp;
        int maxHp = isPlayer ? (playerCard != null ? playerCard.getMaxHp() : hp) : (opponentCard != null ? opponentCard.getMaxHp() : hp);
        StatusEffectProcessor.BattleContext context = new StatusEffectProcessor.BattleContext(hp, maxHp);
        
        List<Integer> values = isPlayer ? playerDiceValues : opponentDiceValues;
        List<Boolean> selected = isPlayer ? playerDiceSelected : opponentDiceSelected;
        List<Boolean> isPrismatic = isPlayer ? getPrismaticFlagsForDice(true) : getPrismaticFlagsForDice(false);
        
        if (values != null && selected != null) {
            context.setDiceValues(values, isPrismatic);
        }
        
        return context;
    }

    public List<Boolean> getPrismaticFlagsForDice(boolean isPlayer) {
        List<Boolean> flags = new ArrayList<>();
        List<DiceType> types = isPlayer ? playerDiceTypes : opponentDiceTypes;
        if (types == null) return flags;
        for (DiceType type : types) {
            flags.add(type == DiceType.PRISMATIC_D12);
        }
        return flags;
    }

    public DicePoolCounts getPlayerDicePoolCounts() {
        return playerDicePoolCounts;
    }

    public DicePoolCounts getOpponentDicePoolCounts() {
        return opponentDicePoolCounts;
    }

    public CharacterCard getPlayerCard() {
        return playerCard;
    }

    public CharacterCard getOpponentCard() {
        return opponentCard;
    }

    public StatusEffectProcessor getPlayerEffects() {
        return effectManager.getEffects(true);
    }

    public StatusEffectProcessor getOpponentEffects() {
        return effectManager.getEffects(false);
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    

    public int getPlayerHp() {
        return playerHp;
    }
    
    public void setPlayerHp(int hp) {
        this.playerHp = Math.max(0, Math.min(hp, playerCard != null ? playerCard.getMaxHp() : hp));
    }

    public int getOpponentHp() {
        return opponentHp;
    }
    
    public void setOpponentHp(int hp) {
        this.opponentHp = Math.max(0, Math.min(hp, opponentCard != null ? opponentCard.getMaxHp() : hp));
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public boolean isPlayerAttacker() {
        return playerIsAttacker;
    }

    public boolean isAttacker(boolean forPlayer) {
        return (forPlayer && playerIsAttacker) || (!forPlayer && !playerIsAttacker);
    }

    public boolean isDefender(boolean forPlayer) {
        return !isAttacker(forPlayer);
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public List<DiceType> getPlayerDiceTypes() {
        return playerDiceTypes;
    }

    public List<Integer> getPlayerDiceValues() {
        return playerDiceValues;
    }

    public List<Boolean> getPlayerDiceSelected() {
        return playerDiceSelected;
    }

    public List<DiceType> getOpponentDiceTypes() {
        return opponentDiceTypes;
    }

    public List<Integer> getOpponentDiceValues() {
        return opponentDiceValues;
    }

    public List<Boolean> getOpponentDiceSelected() {
        return opponentDiceSelected;
    }

    public int getAttackValue() {
        return attackValue;
    }

    public int getDefenseValue() {
        return defenseValue;
    }

    public int getLastDamageDealt() {
        return lastDamageDealt;
    }

    public String getWinner() {
        return winner;
    }

    public int getRequiredPlayerDiceCount() {
        return getRequiredDiceCount(true);
    }

    public int getPlayerPrismaticUses() {
        return prismaticManager != null ? prismaticManager.getUses(true) : 0;
    }

    public boolean isPlayerPrismaticModeActive() {
        return prismaticManager != null && prismaticManager.isModeActive(true);
    }
    
    
    
    public void addPrismaticUse(PrismaticDiceType type, boolean isPlayer) {
        if (prismaticManager != null) prismaticManager.addPrismaticUse(type, isPlayer);
    }
    
    
    
    public void setPlayerSelectedPrismaticType(PrismaticDiceType type, boolean useTrueVersion) {
        if (prismaticManager != null) prismaticManager.setSelectedType(true, type, useTrueVersion);
    }
    
    
    
    
    
    public void rollPrismaticDice(boolean isPlayer) {
        if (prismaticManager == null) return;
        
        prismaticManager.clearRolledDice();
        
        PrismaticDiceType type = isPlayer ? prismaticManager.getSelectedType(true) : null;
        boolean useTrue = isPlayer && prismaticManager.isUseTrueVersion(true);
        
        if (type == null && isPlayer) return;
        if (!isPlayer) return;
        
        prismaticManager.rollPrismaticDice(isPlayer, type, useTrue);
        
        notifyPrismaticDiceRolled(isPlayer, prismaticManager.getRolledDice(isPlayer));
        
        List<PrismaticDiceInstance> mustSelect = prismaticManager.getMustSelectDice(isPlayer);
        if (!mustSelect.isEmpty()) {
            notifyMustSelectDiceMarked(isPlayer, mustSelect);
        }
    }
    
    
    
    public List<Boolean> getPrismaticDiceSelected(boolean isPlayer) {
        if (prismaticManager == null) return new ArrayList<>();
        List<PrismaticDiceInstance> dice = prismaticManager.getRolledDice(isPlayer);
        List<Boolean> selected = new ArrayList<>();
        for (PrismaticDiceInstance d : dice) selected.add(d.isSelected());
        return selected;
    }
    
    public List<Boolean> getPlayerPrismaticDiceSelected() {
        return getPrismaticDiceSelected(true);
    }
    
    public List<Boolean> getOpponentPrismaticDiceSelected() {
        return getPrismaticDiceSelected(false);
    }
    
    public boolean selectPrismaticDice(int index, boolean isPlayer) {
        if (prismaticManager == null) return false;
        return prismaticManager.selectPrismaticDice(isPlayer, index);
    }
    
    public boolean canConfirmPrismaticSelection(boolean isPlayer) {
        if (prismaticManager == null) return true;
        return prismaticManager.canConfirmPrismaticSelection(isPlayer);
    }
    
    
    
    
    
    public int getPlayerTotalDamageTaken() {
        return playerTotalDamageTaken;
    }
    
    public int getOpponentTotalDamageTaken() {
        return opponentTotalDamageTaken;
    }
    
    
    
    public void recordFaceSelection(int faceValue, boolean isPlayer) {
        Map<Integer, Integer> history = isPlayer ? playerFaceSelectionHistory : opponentFaceSelectionHistory;
        history.merge(faceValue, 1, Integer::sum);
    }
    
    public void recordDamageTaken(int damage, boolean isPlayer) {
        if (isPlayer) {
            playerTotalDamageTaken += damage;
        } else {
            opponentTotalDamageTaken += damage;
        }
    }
    
    public void applyDamageTo(boolean isPlayer, int damage) {
        if (isPlayer) {
            playerHp = Math.max(0, playerHp - damage);
        } else {
            opponentHp = Math.max(0, opponentHp - damage);
        }
        recordDamageTaken(damage, isPlayer);
    }
    
    public void applyHealTo(boolean isPlayer, int heal) {
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        int maxHp = card != null ? card.getMaxHp() : Integer.MAX_VALUE;
        if (isPlayer) {
            playerHp = Math.min(playerHp + heal, maxHp);
        } else {
            opponentHp = Math.min(opponentHp + heal, maxHp);
        }
    }
    
    public Map<Integer, Integer> getPlayerFaceSelectionHistory() {
        return playerFaceSelectionHistory;
    }
    
    public Map<Integer, Integer> getOpponentFaceSelectionHistory() {
        return opponentFaceSelectionHistory;
    }
    
    public int getPrismaticTriggerCount(boolean forPlayer) {
        return forPlayer ? playerPrismaticTriggerCount : opponentPrismaticTriggerCount;
    }
    
    public void incrementPrismaticTriggerCount(boolean forPlayer) {
        if (forPlayer) {
            playerPrismaticTriggerCount++;
        } else {
            opponentPrismaticTriggerCount++;
        }
    }
    
    
    
    public void setDoubleValueActive(boolean isPlayer, boolean active) {
        if (prismaticManager != null) prismaticManager.setDoubleValueActive(isPlayer, active);
    }
    
    public void addInstantDamage(boolean isPlayer, int amount) {
        if (prismaticManager != null) prismaticManager.addInstantDamage(isPlayer, amount);
    }
    
    public void clearPrismaticState() {
        if (prismaticManager != null) prismaticManager.clearState();
    }
    
    public void applyPrismaticDiceEffects() {
        if (prismaticManager != null) prismaticManager.applyQueuedEffects(this);
    }
    
    public int getPrismaticDiceTotalValue(boolean isPlayer) {
        if (prismaticManager == null) return 0;
        return prismaticManager.calculateTotalValue(isPlayer, true);
    }
    
    public int getPrismaticInstantDamage(boolean isPlayer) {
        if (prismaticManager == null) return 0;
        return prismaticManager.getInstantDamage(isPlayer);
    }
    
    public void setRemainingRerolls(boolean isPlayer, int count) {
        if (isPlayer) playerRemainingRerolls = count;
        else opponentRemainingRerolls = count;
    }
    
    public void modifyAttackValue(int delta) {
        attackValue += delta;
    }
    
    public void modifyDefenseValue(int delta) {
        defenseValue += delta;
    }
    
    public void multiplyAttackValue(int multiplier) {
        attackValue *= multiplier;
    }
    
    public void addPrismaticUse(int amount) {
        if (prismaticManager != null) {
            for (int i = 0; i < amount; i++) {
                prismaticManager.addPrismaticUse(null, true);
                prismaticManager.addPrismaticUse(null, false);
            }
        }
    }
    
    public void addPrismaticUseByType(PrismaticDiceType type, boolean isPlayer, int amount) {
        if (prismaticManager != null) {
            for (int i = 0; i < amount; i++) {
                prismaticManager.addPrismaticUse(type, isPlayer);
            }
        }
    }

    public void togglePlayerPrismaticMode() {
        if (currentPhase != Phase.SELECTING_ATTACK && currentPhase != Phase.SELECTING_DEFENSE) return;
        if (getPlayerPrismaticUses() <= 0) return;

        boolean playerShouldSelect = (isAttacker(true) && currentPhase == Phase.SELECTING_ATTACK) ||
                                      (isDefender(true) && currentPhase == Phase.SELECTING_DEFENSE);
        if (!playerShouldSelect) return;

        if (prismaticManager != null) prismaticManager.toggleMode(true);
    }

    
    
    public WeatherController getWeatherController() {
        return weatherController;
    }
    
    
    
    public CharacterCard getCard(boolean forPlayer) {
        return forPlayer ? playerCard : opponentCard;
    }
    
    public List<Integer> getDiceValues(boolean forPlayer) {
        return forPlayer ? playerDiceValues : opponentDiceValues;
    }
    
    public List<DiceType> getDiceTypes(boolean forPlayer) {
        return forPlayer ? playerDiceTypes : opponentDiceTypes;
    }
    
    public List<Boolean> getDiceSelected(boolean forPlayer) {
        return forPlayer ? playerDiceSelected : opponentDiceSelected;
    }
    
    public void setDiceValues(boolean forPlayer, List<Integer> values) {
        if (forPlayer) playerDiceValues = values;
        else opponentDiceValues = values;
    }
    
    public void setDiceTypes(boolean forPlayer, List<DiceType> types) {
        if (forPlayer) playerDiceTypes = types;
        else opponentDiceTypes = types;
    }
    
    public void setDiceSelected(boolean forPlayer, List<Boolean> selected) {
        if (forPlayer) playerDiceSelected = selected;
        else opponentDiceSelected = selected;
    }
    
    public int getRemainingRerolls(boolean forPlayer) {
        return forPlayer ? playerRemainingRerolls : opponentRemainingRerolls;
    }
    
    public int getRerollsUsedThisTurn(boolean forPlayer) {
        return forPlayer ? playerRerollsUsedThisTurn : opponentRerollsUsedThisTurn;
    }
    
    public void decrementRerolls(boolean forPlayer) {
        if (forPlayer) playerRemainingRerolls--;
        else opponentRemainingRerolls--;
    }
    
    public void incrementRerollsUsed(boolean forPlayer) {
        if (forPlayer) playerRerollsUsedThisTurn++;
        else opponentRerollsUsedThisTurn++;
    }
    
    public void resetRerollsUsedThisTurn() {
        playerRerollsUsedThisTurn = 0;
        opponentRerollsUsedThisTurn = 0;
    }
    
    public void setCurrentPhase(Phase phase) {
        this.currentPhase = phase;
    }
    
    public void setAttackValue(int value) {
        this.attackValue = value;
    }
    
    public void setDefenseValue(int value) {
        this.defenseValue = value;
    }
    
    public void setLastDamageDealt(int damage) {
        this.lastDamageDealt = damage;
    }
    
    public void incrementTurnNumber() {
        this.turnNumber++;
    }
    
    public void swapAttackerDefender() {
        this.playerIsAttacker = !this.playerIsAttacker;
    }
    
    public void clearDiceSelection(boolean forPlayer) {
        List<Boolean> selected = getDiceSelected(forPlayer);
        if (selected != null) {
            for (int i = 0; i < selected.size(); i++) {
                selected.set(i, false);
            }
        }
    }
    
    public int countSelectedDice(boolean forPlayer) {
        return countSelected(getDiceSelected(forPlayer));
    }
    
    public int calculateSelectedSum(boolean forPlayer) {
        return calculateSelectedSum(getDiceValues(forPlayer), getDiceSelected(forPlayer));
    }
    
    public void recordSelectedFaces(boolean forPlayer) {
        List<Integer> values = getDiceValues(forPlayer);
        List<Boolean> selected = getDiceSelected(forPlayer);
        if (values != null && selected != null) {
            for (int i = 0; i < selected.size(); i++) {
                if (selected.get(i)) {
                    recordFaceSelection(values.get(i), forPlayer);
                }
            }
        }
    }
    
    public void notifyPhaseChange(Phase phase) {
        for (BattleEventListener listener : listeners) {
            listener.onPhaseChange(phase);
        }
    }
    
    public void notifyDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices) {
        for (BattleEventListener listener : listeners) {
            listener.onDiceRerolled(isPlayer, newValues, rerolledIndices);
        }
    }
    
    public void notifyDamageResolved(int damage, int pHp, int oHp) {
        for (BattleEventListener listener : listeners) {
            listener.onDamageResolved(damage, pHp, oHp);
        }
    }
    
    public void notifyBattleEnd(String winner) {
        for (BattleEventListener listener : listeners) {
            listener.onBattleEnd(winner);
        }
    }
    
    public void notifyDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        for (BattleEventListener l : listeners) {
            l.onDiceRolled(isPlayer, types, values);
        }
    }
    
    
    
    public void notifyPrismaticDiceRolled(boolean isPlayer, List<PrismaticDiceInstance> dice) {
        for (BattleEventListener l : listeners) {
            l.onPrismaticDiceRolled(isPlayer, dice);
        }
    }
    
    public void notifyPrismaticDiceSelected(boolean isPlayer, int index, boolean selected) {
        for (BattleEventListener l : listeners) {
            l.onPrismaticDiceSelected(isPlayer, index, selected);
        }
    }
    
    public void notifyMustSelectDiceMarked(boolean isPlayer, List<PrismaticDiceInstance> mustSelect) {
        for (BattleEventListener l : listeners) {
            l.onMustSelectDiceMarked(isPlayer, mustSelect);
        }
    }
    
    public DiceRoller getDiceRoller() {
        return diceRoller;
    }
    
    
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public void notifyWeatherChange(WeatherType newWeather) {
        for (BattleEventListener l : listeners) {
            l.onWeatherChange(newWeather);
        }
    }
    
    public int getCumulativeAtkDef(boolean forPlayer) {
        return forPlayer ? playerCumulativeAtkDef : opponentCumulativeAtkDef;
    }
    
    public void addCumulativeAtkDef(boolean forPlayer, int value) {
        if (forPlayer) {
            playerCumulativeAtkDef += value;
        } else {
            opponentCumulativeAtkDef += value;
        }
    }
    
    public boolean isCyreneThresholdMet(boolean forPlayer) {
        return forPlayer ? playerCyreneThresholdMet : opponentCyreneThresholdMet;
    }
    
    public void setCyreneThresholdMet(boolean forPlayer, boolean met) {
        if (forPlayer) {
            playerCyreneThresholdMet = met;
        } else {
            opponentCyreneThresholdMet = met;
        }
    }
    
    public void recordTurnAtkDef(boolean forPlayer) {
        int value = forPlayer 
            ? (playerIsAttacker ? attackValue : defenseValue)
            : (playerIsAttacker ? defenseValue : attackValue);
        addCumulativeAtkDef(forPlayer, value);
    }
    
    public void cleanup() {
        listeners.clear();
        
        if (effectManager != null) {
            effectManager.clearTemporaryEffects();
            effectManager.resetTurnState();
        }
        
        if (prismaticManager != null) {
            prismaticManager.clearState();
        }
        
        playerDiceTypes = null;
        playerDiceValues = null;
        playerDiceSelected = null;
        opponentDiceTypes = null;
        opponentDiceValues = null;
        opponentDiceSelected = null;
        
        diceRoller = null;
        weatherController = null;
        
        playerFaceSelectionHistory.clear();
        opponentFaceSelectionHistory.clear();
        playerCumulativeAtkDef = 0;
        opponentCumulativeAtkDef = 0;
        playerCyreneThresholdMet = false;
        opponentCyreneThresholdMet = false;
        playerTotalDamageTaken = 0;
        opponentTotalDamageTaken = 0;
        playerPrismaticTriggerCount = 0;
        opponentPrismaticTriggerCount = 0;
    }
}
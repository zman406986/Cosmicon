package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.Strings;

public class BattleState {

    public enum Phase {
        ROLLING,
        SELECTING_ATTACK,
        DICE_DISPLAY_ATTACK,
        SELECTING_DEFENSE,
        DICE_DISPLAY_DEFENSE,
        RESOLVING_PRE_CLASH,
        RESOLVING_MODIFICATION,
        RESOLVING,
        WAITING_NEXT_TURN,
        ENDED
    }

    public enum TurnType {
        ATTACK,
        DEFENSE
    }

    private final StatusEffectProcessor playerEffects;
    private final StatusEffectProcessor opponentEffects;
    private PrismaticManager prismaticManager;
    private WeatherController weatherController;
    private TutorialDiceRoller tutorialDiceRoller;
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

    private int playerPendingDefLevelBoost;
    private int opponentPendingDefLevelBoost;
    private int playerOriginalDefLevel;
    private int opponentOriginalDefLevel;
    private int playerPendingStrength;
    private int opponentPendingStrength;

    private boolean phainonUnyieldingAvailable;
    private boolean opponentPhainonUnyieldingAvailable;

    private int playerWeatherAtkMod;
    private int opponentWeatherAtkMod;
    private int playerWeatherDefMod;
    private int opponentWeatherDefMod;

    private int playerRemainingRerolls;
    private int opponentRemainingRerolls;
    private int playerRerollsUsedThisTurn;
    private int opponentRerollsUsedThisTurn;
    
    private boolean isDefenderRolling;

    private List<DiceType> playerUpgradedDicePool;
    private List<DiceType> opponentUpgradedDicePool;

    private List<DiceType> playerDiceTypes;
    private List<Integer> playerDiceValues;
    private List<Boolean> playerDiceSelected;

    private List<DiceType> opponentDiceTypes;
    private List<Integer> opponentDiceValues;
    private List<Boolean> opponentDiceSelected;

    private final Map<Integer, PrismaticDiceInstance> playerPrismaticDiceByIndex;
    private final Map<Integer, PrismaticDiceInstance> opponentPrismaticDiceByIndex;

    private int attackValue;
    private int defenseValue;
    private String winner;
    
    private String attackerConfirmedSelectionText;
    
    private String defenderConfirmedSelectionText;

    private final List<BattleEventListener> listeners;
    private final AISelectionVisualizer aiSelectionVisualizer;
    private final List<ValueChangeRecord> pendingValueChanges;
    private boolean valueChangeAnimationInProgress;

    private final List<ModificationRecord> modificationOrder;
    private int modificationSequenceCounter;

    private int playerHackDiceIndex = -1;
    private int opponentHackDiceIndex = -1;
    private int playerAriseDiceIndex = -1;
    private int opponentAriseDiceIndex = -1;

    public record ModificationRecord(StatusEffectProcessor.StatusEffect effect, boolean forPlayer, int sequence, int diceIndex) {
        public ModificationRecord(StatusEffectProcessor.StatusEffect effect, boolean forPlayer, int sequence) {
            this(effect, forPlayer, sequence, -1);
        }
    }

    public interface BattleEventListener {
        void onPhaseChange(Phase newPhase);
        void onDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices);
        void onBattleEnd(String winner);
        void onDamageResolved(int damage, int playerHp, int opponentHp);
        void onDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values);
        void onWeatherChange(WeatherType newWeather);
        void onDamageAnimationStart(DamageResolver.DamageResult result);
        void onDamageAnimationComplete();
        void onDamageImpacted();
        void onValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta);
        void onTransitionToDefenderRoll();
    }

    public record ValueChangeRecord(String changeType, int delta, String displayText, boolean isPlayer)
    {
    }
    
    private DamageAnimationCallback damageAnimationCallback;
    
    public interface DamageAnimationCallback {
        void onDamageAnimationComplete();
    }
    
    public void setDamageAnimationCallback(DamageAnimationCallback callback) {
        this.damageAnimationCallback = callback;
    }
    
    public boolean hasCombo() {
        StatusEffectProcessor effects = isPlayerAttacker() ? getPlayerEffects() : getOpponentEffects();
        return effects.hasEffect(StatusEffectProcessor.StatusEffect.COMBO);
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
        
        this.playerEffects = new StatusEffectProcessor();
        this.opponentEffects = new StatusEffectProcessor();
        this.prismaticManager = new PrismaticManager(this);
        
        this.playerTotalDamageTaken = 0;
        this.opponentTotalDamageTaken = 0;
        this.playerFaceSelectionHistory = new HashMap<>();
        this.opponentFaceSelectionHistory = new HashMap<>();
        this.playerCumulativeAtkDef = 0;
        this.opponentCumulativeAtkDef = 0;
        this.playerCyreneThresholdMet = false;
        this.opponentCyreneThresholdMet = false;
        this.playerPrismaticDiceByIndex = new HashMap<>();
        this.opponentPrismaticDiceByIndex = new HashMap<>();
        this.aiSelectionVisualizer = new AISelectionVisualizer();
        this.pendingValueChanges = new ArrayList<>();
        this.valueChangeAnimationInProgress = false;
        this.modificationOrder = new ArrayList<>();
        this.modificationSequenceCounter = 0;
        this.playerPendingDefLevelBoost = 0;
        this.opponentPendingDefLevelBoost = 0;
        this.playerOriginalDefLevel = 0;
        this.opponentOriginalDefLevel = 0;
        this.phainonUnyieldingAvailable = true;
        this.opponentPhainonUnyieldingAvailable = true;
    }

    
    
    public void init(CharacterCard playerCard, CharacterCard opponentCard) {
        init(playerCard, opponentCard, true);
    }

    public void init(CharacterCard playerCard, CharacterCard opponentCard, boolean playerIsAttacker) {
        this.playerCard = playerCard;
        this.opponentCard = opponentCard;
        this.playerDicePoolCounts = DicePoolCounts.fromPool(playerCard.getDicePool());
        this.opponentDicePoolCounts = DicePoolCounts.fromPool(opponentCard.getDicePool());
        clearUpgradedDicePool(true);
        clearUpgradedDicePool(false);
        resetBattleState(playerCard.getMaxHp(), opponentCard.getMaxHp(), playerIsAttacker);
        playerCumulativeAtkDef = 0;
        opponentCumulativeAtkDef = 0;
        playerCyreneThresholdMet = false;
        opponentCyreneThresholdMet = false;
        prismaticManager.initializeFromCards(playerCard, opponentCard);

        CosmiconLogger.debug("BattleState initialized - Player: %s (HP %d), Opponent: %s (HP %d)",
            playerCard.getName(), playerHp, opponentCard.getName(), opponentHp);
        CosmiconLogger.debug("Initial turn: Turn 1, Player is attacker: %s", this.playerIsAttacker);
    }

    private void resetBattleState(int playerMaxHp, int opponentMaxHp, boolean playerIsAttacker) {
        playerHp = playerMaxHp;
        opponentHp = opponentMaxHp;
        turnNumber = 1;
        this.playerIsAttacker = playerIsAttacker;
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
        if (currentPhase != Phase.SELECTING_ATTACK && currentPhase != Phase.SELECTING_DEFENSE) return;
        if (index < 0 || index >= playerDiceValues.size()) return;
        playerDiceSelected.set(index, !playerDiceSelected.get(index));
    }

    public int getRemainingRerolls() {
        return playerRemainingRerolls;
    }

    public boolean canReroll(boolean forPlayer) {
        Phase phase = getCurrentPhase();
        boolean inSelection = phase == Phase.SELECTING_ATTACK || phase == Phase.SELECTING_DEFENSE;
        boolean isCorrectRole = (phase == Phase.SELECTING_ATTACK && isAttacker(forPlayer)) ||
                                (phase == Phase.SELECTING_DEFENSE && isDefender(forPlayer));
        return inSelection && isCorrectRole && getRemainingRerolls(forPlayer) > 0;
    }

    public int getRerollsUsedThisTurn() {
        return playerRerollsUsedThisTurn;
    }

    

    public int getRequiredDiceCount(boolean isPlayer) {
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        if (card == null) return 0;
        int baseLevel = (isPlayer == playerIsAttacker) ? card.getAtkLevel() : card.getDefLevel();
        int weatherMod = (isPlayer == playerIsAttacker) 
            ? (isPlayer ? playerWeatherAtkMod : opponentWeatherAtkMod)
            : (isPlayer ? playerWeatherDefMod : opponentWeatherDefMod);
        return Math.max(1, Math.min(baseLevel + weatherMod, 5));
    }

    public int countSelected(List<Boolean> selected) {
        if (selected == null) return 0;
        int count = 0;
        for (Boolean aBoolean : selected)
        {
            if (aBoolean) count++;
        }
        return count;
    }

    public int calculateSelectedSum(List<Integer> values, List<Boolean> selected) {
        if (values == null || selected == null) return 0;
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
        List<DiceType> types = isPlayer ? playerDiceTypes : opponentDiceTypes;
        
        if (values != null && selected != null) {
            context.setDiceValues(values, isPrismatic);
            context.setDiceSelected(selected);
        }

        if (types != null) {
            context.setDiceTypes(new ArrayList<>(types));
            List<Integer> maxFaces = new ArrayList<>();
            for (DiceType type : types) {
                maxFaces.add(type.getMaxFace());
            }
            context.setDiceMaxFaces(maxFaces);
        }
        
        return context;
    }

    public List<Boolean> getPrismaticFlagsForDice(boolean isPlayer) {
        List<Boolean> flags = new ArrayList<>();
        List<DiceType> types = isPlayer ? playerDiceTypes : opponentDiceTypes;
        if (types == null) return flags;
        for (DiceType type : types) {
            flags.add(type == DiceType.PRISMATIC);
        }
        return flags;
    }

    public void addPrismaticDiceToPool(PrismaticDiceInstance dice, boolean forPlayer) {
        List<DiceType> types = getDiceTypesDirect(forPlayer);
        List<Integer> values = getDiceValuesDirect(forPlayer);
        List<Boolean> selected = getDiceSelectedDirect(forPlayer);
        Map<Integer, PrismaticDiceInstance> map = forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
        
        types.add(DiceType.PRISMATIC);
        values.add(dice.rolledFace);
        selected.add(false);
        map.put(types.size() - 1, dice);
    }

    public PrismaticDiceInstance getPrismaticDiceAt(int index, boolean forPlayer) {
        Map<Integer, PrismaticDiceInstance> map = forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
        return map.get(index);
    }

    public boolean isPrismaticDiceAt(int index, boolean forPlayer) {
        Map<Integer, PrismaticDiceInstance> map = forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
        return map.containsKey(index);
    }

    public void updatePrismaticDiceAt(int index, PrismaticDiceInstance newInstance, boolean forPlayer) {
        Map<Integer, PrismaticDiceInstance> map = forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
        if (map.containsKey(index)) {
            map.put(index, newInstance);
        }
    }
    
    public Map<Integer, PrismaticDiceInstance> getPrismaticDiceMap(boolean forPlayer) {
        return forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
    }
    
    public List<PrismaticDiceInstance> getSelectedPrismaticDice(boolean forPlayer) {
        List<PrismaticDiceInstance> result = new ArrayList<>();
        Map<Integer, PrismaticDiceInstance> map = forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
        List<Boolean> selected = getDiceSelected(forPlayer);
        
        for (Map.Entry<Integer, PrismaticDiceInstance> entry : map.entrySet()) {
            int idx = entry.getKey();
            if (idx < selected.size() && selected.get(idx)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
    
    public DicePoolCounts getPlayerDicePoolCounts() {
        if (playerUpgradedDicePool != null) return DicePoolCounts.fromPool(playerUpgradedDicePool);
        return playerDicePoolCounts;
    }

    public DicePoolCounts getOpponentDicePoolCounts() {
        if (opponentUpgradedDicePool != null) return DicePoolCounts.fromPool(opponentUpgradedDicePool);
        return opponentDicePoolCounts;
    }

    public CharacterCard getPlayerCard() {
        return playerCard;
    }

    public CharacterCard getOpponentCard() {
        return opponentCard;
    }

    public StatusEffectProcessor getPlayerEffects() {
        return playerEffects;
    }

    public StatusEffectProcessor getOpponentEffects() {
        return opponentEffects;
    }

    public StatusEffectProcessor getEffects(boolean forPlayer) {
        return forPlayer ? playerEffects : opponentEffects;
    }
    
    public void applyEffect(StatusEffectProcessor.StatusEffect effect, int layers, boolean toPlayer) {
        getEffects(toPlayer).addEffect(effect, layers);
        if (effect == StatusEffectProcessor.StatusEffect.HACK || effect == StatusEffectProcessor.StatusEffect.ARISE) {
            modificationOrder.add(new ModificationRecord(effect, toPlayer, modificationSequenceCounter++));
        }
        CosmiconLogger.debug("Effect applied to %s (%d layers)", toPlayer ? "Player" : "Opponent", layers);
    }

    public void setHackDiceIndex(boolean forPlayer, int diceIndex) {
        if (forPlayer) {
            playerHackDiceIndex = diceIndex;
        } else {
            opponentHackDiceIndex = diceIndex;
        }
    }

    public void setAriseDiceIndex(boolean forPlayer, int diceIndex) {
        if (forPlayer) {
            playerAriseDiceIndex = diceIndex;
        } else {
            opponentAriseDiceIndex = diceIndex;
        }
    }

    public int getHackDiceIndex(boolean forPlayer) {
        return forPlayer ? playerHackDiceIndex : opponentHackDiceIndex;
    }

    public int getAriseDiceIndex(boolean forPlayer) {
        return forPlayer ? playerAriseDiceIndex : opponentAriseDiceIndex;
    }

    public List<ModificationRecord> getModificationOrder() {
        return new ArrayList<>(modificationOrder);
    }

    public void clearModificationOrder() {
        modificationOrder.clear();
    }

    public boolean hasPendingModification() {
        boolean playerHasHack = playerEffects.hasEffect(StatusEffectProcessor.StatusEffect.HACK);
        boolean opponentHasHack = opponentEffects.hasEffect(StatusEffectProcessor.StatusEffect.HACK);
        boolean playerHasArise = playerEffects.hasEffect(StatusEffectProcessor.StatusEffect.ARISE);
        boolean opponentHasArise = opponentEffects.hasEffect(StatusEffectProcessor.StatusEffect.ARISE);
        return playerHasHack || opponentHasHack || playerHasArise || opponentHasArise;
    }
    
    public void resetEffectTurnState() {
        playerEffects.resetTurnState();
        opponentEffects.resetTurnState();
        modificationOrder.clear();
        modificationSequenceCounter = 0;
        playerHackDiceIndex = -1;
        opponentHackDiceIndex = -1;
        playerAriseDiceIndex = -1;
        opponentAriseDiceIndex = -1;
    }
    
    public void clearTemporaryEffects() {
        playerEffects.clearTemporaryEffects();
        opponentEffects.clearTemporaryEffects();
    }

    public PrismaticManager getPrismaticManager() {
        return prismaticManager;
    }

    public TutorialDiceRoller getTutorialDiceRoller() {
        return tutorialDiceRoller;
    }

    public void setTutorialDiceRoller(TutorialDiceRoller roller) {
        this.tutorialDiceRoller = roller;
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

    public boolean isDefenderRolling() {
        return isDefenderRolling;
    }

    public void setDefenderRolling(boolean isDefenderRolling) {
        this.isDefenderRolling = isDefenderRolling;
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

    public String getWinner() {
        return winner;
    }

    public int getRequiredPlayerDiceCount() {
        return getRequiredDiceCount(true);
    }

    public int getPlayerPrismaticUses() {
        return prismaticManager.getUses(true);
    }

    public int getOpponentPrismaticUses() {
        return prismaticManager.getUses(false);
    }

    public void addPrismaticUse(PrismaticDiceType type, boolean isPlayer) {
        prismaticManager.addPrismaticUse(type, isPlayer);
    }

    @SuppressWarnings("unused")
    public void consumePrismaticUse(PrismaticDiceType type, boolean forPlayer) {
        prismaticManager.consumePrismaticUse(type, forPlayer);
    }
    
    
    
public boolean canConfirmPrismaticSelection(boolean isPlayer) {
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
        int oldHp = isPlayer ? playerHp : opponentHp;
        String characterName = isPlayer ? 
            (playerCard != null ? playerCard.getName() : Strings.get("battle.player")) : 
            (opponentCard != null ? opponentCard.getName() : Strings.get("battle.opponent"));
        
        if (isPlayer) {
            playerHp = Math.max(0, playerHp - damage);
        } else {
            opponentHp = Math.max(0, opponentHp - damage);
        }
        recordDamageTaken(damage, isPlayer);
        
        int newHp = isPlayer ? playerHp : opponentHp;
        
        if (newHp <= 0 && getEffects(isPlayer).hasEffect(StatusEffectProcessor.StatusEffect.UNYIELDING)) {
            newHp = 1;
            if (isPlayer) {
                playerHp = 1;
            } else {
                opponentHp = 1;
            }
            StatusEffectProcessor effects = getEffects(isPlayer);
            effects.removeEffect(StatusEffectProcessor.StatusEffect.UNYIELDING);
            CosmiconLogger.info("%s: UNYIELDING prevented death (HP: 1)", characterName);
            
            CharacterCard card = isPlayer ? playerCard : opponentCard;
            if (card != null && CharacterIds.PHAINON.equals(card.getId())) {
                consumePhainonUnyielding(isPlayer);
            }
        }
        
        int maxHp = isPlayer ? 
            (playerCard != null ? playerCard.getMaxHp() : oldHp) : 
            (opponentCard != null ? opponentCard.getMaxHp() : oldHp);
        
        CosmiconLogger.hpChange(characterName, oldHp, newHp, maxHp);
        if (damage > 0) {
            CosmiconLogger.info("%s took %d damage (HP: %d/%d)", characterName, damage, newHp, maxHp);
        }
    }
    
    public void applyHealTo(boolean isPlayer, int heal) {
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        int maxHp = card != null ? card.getMaxHp() : Integer.MAX_VALUE;
        int oldHp = isPlayer ? playerHp : opponentHp;
        String characterName = isPlayer ? 
            (playerCard != null ? playerCard.getName() : Strings.get("battle.player")) : 
            (opponentCard != null ? opponentCard.getName() : Strings.get("battle.opponent"));
        
        if (isPlayer) {
            playerHp = Math.min(playerHp + heal, maxHp);
        } else {
            opponentHp = Math.min(opponentHp + heal, maxHp);
        }
        
        if (heal > 0) {
            CosmiconLogger.debug("%s healed %d (HP: %d -> %d/%d)", characterName, heal, oldHp, 
                isPlayer ? playerHp : opponentHp, maxHp);
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
        prismaticManager.setDoubleValueActive(isPlayer, active);
    }
    
    public void addInstantDamage(boolean isPlayer, int amount) {
        prismaticManager.addInstantDamage(isPlayer, amount);
    }
    
    public void clearPrismaticState() {
        if (valueChangeAnimationInProgress) {
            CosmiconLogger.warn("[PRISM_DIAG] Cannot clear prismatic state (turn %d): valueChangeAnimationInProgress, playerPrismaticDiceByIndex=%s",
                turnNumber, playerPrismaticDiceByIndex.keySet());
            return;
        }
        if (currentPhase == Phase.RESOLVING || currentPhase == Phase.RESOLVING_PRE_CLASH || currentPhase == Phase.RESOLVING_MODIFICATION) {
            CosmiconLogger.warn("[PRISM_DIAG] Cannot clear prismatic state (turn %d): phase=%s, playerPrismaticDiceByIndex=%s",
                turnNumber, currentPhase, playerPrismaticDiceByIndex.keySet());
            return;
        }
        prismaticManager.clearState();
        playerPrismaticDiceByIndex.clear();
        opponentPrismaticDiceByIndex.clear();
    }
    
    public void applyPrismaticDiceEffects() {
        prismaticManager.applyQueuedEffects(this);
    }
    
    public int getPrismaticDiceTotalValue(boolean isPlayer) {
        return prismaticManager.calculateTotalValue(isPlayer, true);
    }
    
    public int getPrismaticInstantDamage(boolean isPlayer) {
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
        for (int i = 0; i < amount; i++) {
            prismaticManager.addPrismaticUse(null, true);
            prismaticManager.addPrismaticUse(null, false);
        }
    }
    
    public void addPrismaticUseByType(PrismaticDiceType type, boolean isPlayer, int amount) {
        for (int i = 0; i < amount; i++) {
            prismaticManager.addPrismaticUse(type, isPlayer);
        }
    }

    
    
    public WeatherController getWeatherController() {
        return weatherController;
    }
    
    
    
    public CharacterCard getCard(boolean forPlayer) {
        return forPlayer ? playerCard : opponentCard;
    }
    
    public String getCardId(boolean forPlayer) {
        CharacterCard card = getCard(forPlayer);
        return card != null ? card.getId() : null;
    }
    
    public void modifyCardAtkLevel(boolean forPlayer, int delta) {
        CharacterCard card = getCard(forPlayer);
        if (card != null) {
            card.setAtkLevel(card.getAtkLevel() + delta);
        }
    }

    public int getEffectiveAtkLevel(boolean forPlayer) {
        CharacterCard card = getCard(forPlayer);
        if (card == null) return 1;
        int mod = forPlayer ? playerWeatherAtkMod : opponentWeatherAtkMod;
        return Math.max(1, Math.min(card.getAtkLevel() + mod, 5));
    }

    public int getEffectiveDefLevel(boolean forPlayer) {
        CharacterCard card = getCard(forPlayer);
        if (card == null) return 1;
        int mod = forPlayer ? playerWeatherDefMod : opponentWeatherDefMod;
        return Math.max(1, Math.min(card.getDefLevel() + mod, 5));
    }

    public void modifyWeatherAtkMod(boolean forPlayer, int delta) {
        if (forPlayer) {
            playerWeatherAtkMod += delta;
        } else {
            opponentWeatherAtkMod += delta;
        }
    }

    public void modifyWeatherDefMod(boolean forPlayer, int delta) {
        if (forPlayer) {
            playerWeatherDefMod += delta;
        } else {
            opponentWeatherDefMod += delta;
        }
    }

    public void clearWeatherMods() {
        playerWeatherAtkMod = 0;
        opponentWeatherAtkMod = 0;
        playerWeatherDefMod = 0;
        opponentWeatherDefMod = 0;
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
    
    List<Integer> getDiceValuesDirect(boolean forPlayer) {
        return forPlayer ? playerDiceValues : opponentDiceValues;
    }
    
    List<DiceType> getDiceTypesDirect(boolean forPlayer) {
        return forPlayer ? playerDiceTypes : opponentDiceTypes;
    }
    
    List<Boolean> getDiceSelectedDirect(boolean forPlayer) {
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

    public List<DiceType> getUpgradedDicePool(boolean forPlayer) {
        return forPlayer ? playerUpgradedDicePool : opponentUpgradedDicePool;
    }

    public void setUpgradedDicePool(boolean forPlayer, List<DiceType> pool) {
        if (forPlayer) playerUpgradedDicePool = pool;
        else opponentUpgradedDicePool = pool;
    }

    public void clearUpgradedDicePool(boolean forPlayer) {
        if (forPlayer) playerUpgradedDicePool = null;
        else opponentUpgradedDicePool = null;
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
        Phase oldPhase = this.currentPhase;
        this.currentPhase = phase;
        CosmiconLogger.debug("Phase change: %s -> %s", oldPhase, phase);
        if (isMajorPhaseTransition(phase)) {
            CosmiconLogger.info("Phase: %s -> %s", oldPhase, phase);
        }
    }
    
    private boolean isMajorPhaseTransition(Phase newPhase) {
        return newPhase == Phase.SELECTING_ATTACK ||
               newPhase == Phase.DICE_DISPLAY_ATTACK ||
               newPhase == Phase.SELECTING_DEFENSE ||
               newPhase == Phase.DICE_DISPLAY_DEFENSE ||
               newPhase == Phase.RESOLVING_PRE_CLASH ||
               newPhase == Phase.RESOLVING_MODIFICATION ||
               newPhase == Phase.RESOLVING ||
               newPhase == Phase.WAITING_NEXT_TURN ||
               newPhase == Phase.ENDED;
    }
    
    public void setAttackValue(int value) {
        this.attackValue = value;
    }
    
    public void setDefenseValue(int value) {
        this.defenseValue = value;
    }
    
    public void incrementTurnNumber() {
        this.turnNumber++;
    }
    
    public void swapAttackerDefender() {
        this.playerIsAttacker = !this.playerIsAttacker;
    }
    
    public void clearDiceSelection(boolean forPlayer) {
        List<Boolean> selected = getDiceSelectedDirect(forPlayer);
        if (selected != null) {
            Collections.fill(selected, false);
        }
    }
    
    public int countSelectedDice(boolean forPlayer) {
        return countSelected(getDiceSelected(forPlayer));
    }
    
    public int calculateSelectedSum(boolean forPlayer) {
        return calculateSelectedSumExcludingPrismatic(getDiceValues(forPlayer), getDiceSelected(forPlayer), forPlayer);
    }
    
    private int calculateSelectedSumExcludingPrismatic(List<Integer> values, List<Boolean> selected, boolean forPlayer) {
        if (values == null || selected == null) return 0;
        int sum = 0;
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i) && !isPrismaticDiceAt(i, forPlayer)) {
                sum += values.get(i);
            }
        }
        return sum;
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
    
    public String getSelectedDiceValuesFormatted(boolean forPlayer) {
        List<Integer> values = getDiceValues(forPlayer);
        List<Boolean> selected = getDiceSelected(forPlayer);
        if (values == null || selected == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                if (!sb.isEmpty()) sb.append("+");
                if (isPrismaticDiceAt(i, forPlayer)) {
                    sb.append("*").append(values.get(i));
                } else {
                    sb.append(values.get(i));
                }
            }
        }
        return sb.toString();
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
        CosmiconLogger.info("========== BATTLE END ==========");
        CosmiconLogger.info("Winner: %s", winner);
        CosmiconLogger.info("Final HP - Player: %d, Opponent: %d", playerHp, opponentHp);
        CosmiconLogger.info("Total damage taken - Player: %d, Opponent: %d", 
            playerTotalDamageTaken, opponentTotalDamageTaken);
        CosmiconLogger.info("================================");
        
        for (BattleEventListener listener : listeners) {
            listener.onBattleEnd(winner);
        }
    }
    
    public void notifyDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        for (BattleEventListener l : listeners) {
            l.onDiceRolled(isPlayer, types, values);
        }
    }
    
    public void notifyTransitionToDefenderRoll() {
        for (BattleEventListener l : listeners) {
            l.onTransitionToDefenderRoll();
        }
    }
    
    
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public void notifyWeatherChange(WeatherType newWeather) {
        for (BattleEventListener l : listeners) {
            l.onWeatherChange(newWeather);
        }
    }
    
    public void notifyDamageAnimationStart(DamageResolver.DamageResult result) {
        for (BattleEventListener l : listeners) {
            l.onDamageAnimationStart(result);
        }
    }
    
    public void notifyDamageAnimationComplete() {
        for (BattleEventListener l : listeners) {
            l.onDamageAnimationComplete();
        }
        if (damageAnimationCallback != null) {
            damageAnimationCallback.onDamageAnimationComplete();
        }
    }

    public void notifyDamageImpacted() {
        for (BattleEventListener l : listeners) {
            l.onDamageImpacted();
        }
    }

    public void queueValueChange(boolean isPlayer, String changeType, int delta) {
        String displayText = delta >= 0 ? "+" + delta : String.valueOf(delta);
        pendingValueChanges.add(new ValueChangeRecord(changeType, delta, displayText, isPlayer));
    }

    public List<ValueChangeRecord> getPendingValueChanges(boolean isPlayer) {
        List<ValueChangeRecord> result = new ArrayList<>();
        for (ValueChangeRecord record : pendingValueChanges) {
            if (record.isPlayer == isPlayer) {
                result.add(record);
            }
        }
        return result;
    }

    public void clearPendingValueChanges() {
        if (valueChangeAnimationInProgress) {
            CosmiconLogger.warn("Cannot clear pending value changes while animation is in progress");
            return;
        }
        pendingValueChanges.clear();
    }

    public void setValueChangeAnimationInProgress(boolean inProgress) {
        this.valueChangeAnimationInProgress = inProgress;
        if (!inProgress) {
            pendingValueChanges.clear();
        }
    }

    public void notifyValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta) {
        for (BattleEventListener l : listeners) {
            l.onValueChange(isPlayer, changeType, oldValue, newValue, delta);
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

    public int getPendingDefLevelBoost(boolean forPlayer) {
        return forPlayer ? playerPendingDefLevelBoost : opponentPendingDefLevelBoost;
    }

    public void setPendingDefLevelBoost(boolean forPlayer, int boost) {
        if (forPlayer) {
            playerPendingDefLevelBoost = boost;
        } else {
            opponentPendingDefLevelBoost = boost;
        }
    }

    public void clearPendingDefLevelBoost(boolean forPlayer) {
        if (forPlayer) {
            playerPendingDefLevelBoost = 0;
            playerOriginalDefLevel = 0;
        } else {
            opponentPendingDefLevelBoost = 0;
            opponentOriginalDefLevel = 0;
        }
    }

    public int getOriginalDefLevel(boolean forPlayer) {
        return forPlayer ? playerOriginalDefLevel : opponentOriginalDefLevel;
    }

    public void setOriginalDefLevel(boolean forPlayer, int level) {
        if (forPlayer) {
            playerOriginalDefLevel = level;
        } else {
            opponentOriginalDefLevel = level;
        }
    }

    @SuppressWarnings("unused")
    public int getPendingStrength(boolean forPlayer) {
        return forPlayer ? playerPendingStrength : opponentPendingStrength;
    }

    public void setPendingStrength(boolean forPlayer, int strength) {
        if (forPlayer) {
            playerPendingStrength = strength;
        } else {
            opponentPendingStrength = strength;
        }
    }

    public int consumePendingStrength(boolean forPlayer) {
        if (forPlayer) {
            int val = playerPendingStrength;
            playerPendingStrength = 0;
            return val;
        } else {
            int val = opponentPendingStrength;
            opponentPendingStrength = 0;
            return val;
        }
    }

    public boolean isPhainonUnyieldingAvailable(boolean forPlayer) {
        return forPlayer ? !phainonUnyieldingAvailable : !opponentPhainonUnyieldingAvailable;
    }

    public void consumePhainonUnyielding(boolean forPlayer) {
        if (forPlayer) {
            phainonUnyieldingAvailable = false;
        } else {
            opponentPhainonUnyieldingAvailable = false;
        }
    }

    public void recordTurnAtkDef(boolean forPlayer) {
        int value = forPlayer 
            ? (playerIsAttacker ? attackValue : defenseValue)
            : (playerIsAttacker ? defenseValue : attackValue);
        addCumulativeAtkDef(forPlayer, value);
    }
    
    public AISelectionVisualizer getAiSelectionVisualizer() {
        return aiSelectionVisualizer;
    }
    
    public String getAttackerConfirmedSelectionText() {
        return attackerConfirmedSelectionText;
    }
    
    
    
    public void setAttackerConfirmedSelection(String text) {
        this.attackerConfirmedSelectionText = text;
    }
    
    public String getDefenderConfirmedSelectionText() {
        return defenderConfirmedSelectionText;
    }
    
    
    
    public void setDefenderConfirmedSelection(String text) {
        this.defenderConfirmedSelectionText = text;
    }
    
    public void clearConfirmedSelections() {
        attackerConfirmedSelectionText = null;
        defenderConfirmedSelectionText = null;
    }
    
    public void cleanup() {
        CosmiconLogger.debug("BattleState cleanup - clearing listeners and state");
        
        listeners.clear();
        
        clearTemporaryEffects();
        resetEffectTurnState();
        
        prismaticManager.clearState();
        
        playerDiceTypes = null;
        playerDiceValues = null;
        playerDiceSelected = null;
        opponentDiceTypes = null;
        opponentDiceValues = null;
        opponentDiceSelected = null;

        clearUpgradedDicePool(true);
        clearUpgradedDicePool(false);
        
        playerPrismaticDiceByIndex.clear();
        opponentPrismaticDiceByIndex.clear();
        
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
        playerPendingDefLevelBoost = 0;
        opponentPendingDefLevelBoost = 0;
        playerOriginalDefLevel = 0;
        opponentOriginalDefLevel = 0;
        playerWeatherAtkMod = 0;
        opponentWeatherAtkMod = 0;
        playerWeatherDefMod = 0;
        opponentWeatherDefMod = 0;
        phainonUnyieldingAvailable = true;
        opponentPhainonUnyieldingAvailable = true;
        aiSelectionVisualizer.reset();
        
        pendingValueChanges.clear();
        valueChangeAnimationInProgress = false;
        modificationOrder.clear();
        modificationSequenceCounter = 0;
        
        attackValue = 0;
        defenseValue = 0;
        winner = null;
        currentPhase = Phase.WAITING_NEXT_TURN;
        playerHp = 0;
        opponentHp = 0;
        playerRemainingRerolls = 0;
        opponentRemainingRerolls = 0;
        playerRerollsUsedThisTurn = 0;
        opponentRerollsUsedThisTurn = 0;
        isDefenderRolling = false;
        clearConfirmedSelections();
        
        CosmiconLogger.debug("BattleState cleanup complete");
    }
}
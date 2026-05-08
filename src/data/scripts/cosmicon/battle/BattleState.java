package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.CosmiconLogger;

import static data.scripts.cosmicon.battle.EffectState.ModificationRecord;

public class BattleState {

    private final EffectState effectState = new EffectState();
    final DiceState diceState = new DiceState();
    final PrismaticState prismaticState = new PrismaticState();
    private PrismaticManager prismaticManager;
    private WeatherController weatherController;
    private TutorialDiceRoller tutorialDiceRoller;
    private CharacterCard playerCard;
    private CharacterCard opponentCard;

    final HpManager hpManager = new HpManager();

    private final TurnState turnState = new TurnState();
    private final Map<Integer, Integer> playerFaceSelectionHistory;
    private final Map<Integer, Integer> opponentFaceSelectionHistory;
    
    private int playerCumulativeAtkDef;
    private int opponentCumulativeAtkDef;
    private boolean playerCyreneThresholdMet;
    private boolean opponentCyreneThresholdMet;
    
    private int playerPendingDefLevelBoost;
    private int opponentPendingDefLevelBoost;
    private int playerOriginalDefLevel;
    private int opponentOriginalDefLevel;
    private int playerPendingStrength;
    private int opponentPendingStrength;

    private boolean phainonUnyieldingAvailable;
    private boolean opponentPhainonUnyieldingAvailable;

    final BattleEventBus eventBus = new BattleEventBus();
    final RerollState rerollState = new RerollState();
    private final AISelectionVisualizer aiSelectionVisualizer;

    public List<DiceType> getPlayerDiceTypes() {
        return diceState.getPlayerDiceTypes();
    }

    public List<Integer> getPlayerDiceValues() {
        return diceState.getPlayerDiceValues();
    }

    public List<Boolean> getPlayerDiceSelected() {
        return diceState.getPlayerDiceSelected();
    }

    public List<DiceType> getOpponentDiceTypes() {
        return diceState.getOpponentDiceTypes();
    }

    public List<Integer> getOpponentDiceValues() {
        return diceState.getOpponentDiceValues();
    }

    public List<Boolean> getOpponentDiceSelected() {
        return diceState.getOpponentDiceSelected();
    }

    public List<Integer> getDiceValues(boolean forPlayer) {
        return diceState.getDiceValues(forPlayer);
    }

    public List<DiceType> getDiceTypes(boolean forPlayer) {
        return diceState.getDiceTypes(forPlayer);
    }

    public List<Boolean> getDiceSelected(boolean forPlayer) {
        return diceState.getDiceSelected(forPlayer);
    }

    public void setDiceValues(boolean forPlayer, List<Integer> values) {
        diceState.setDiceValues(forPlayer, values);
    }

    public void setDiceTypes(boolean forPlayer, List<DiceType> types) {
        diceState.setDiceTypes(forPlayer, types);
    }

    public void setDiceSelected(boolean forPlayer, List<Boolean> selected) {
        diceState.setDiceSelected(forPlayer, selected);
    }

    public DicePoolCounts getPlayerDicePoolCounts() {
        return diceState.getPlayerDicePoolCounts();
    }

    public DicePoolCounts getOpponentDicePoolCounts() {
        return diceState.getOpponentDicePoolCounts();
    }

    public List<DiceType> getUpgradedDicePool(boolean forPlayer) {
        return diceState.getUpgradedDicePool(forPlayer);
    }

    public void setUpgradedDicePool(boolean forPlayer, List<DiceType> pool) {
        diceState.setUpgradedDicePool(forPlayer, pool);
    }

    public void clearUpgradedDicePool(boolean forPlayer) {
        diceState.clearUpgradedDicePool(forPlayer);
    }

    public void clearDiceSelection(boolean forPlayer) {
        diceState.clearDiceSelection(forPlayer);
    }

    public int countSelectedDice(boolean forPlayer) {
        return diceState.countSelectedDice(forPlayer);
    }

    public int countSelected(List<Boolean> selected) {
        return diceState.countSelected(selected);
    }

    public int calculateSelectedSum(List<Integer> values, List<Boolean> selected) {
        return diceState.calculateSelectedSum(values, selected);
    }

    public List<Boolean> getPrismaticFlagsForDice(boolean isPlayer) {
        return diceState.getPrismaticFlagsForDice(isPlayer);
    }

    public void setDamageAnimationCallback(BattleEventBus.DamageAnimationCallback callback) {
        eventBus.setDamageAnimationCallback(callback);
    }
    
    public boolean hasCombo() {
        StatusEffectProcessor effects = effectState.getEffects(turnState.isPlayerAttacker());
        return effects.hasEffect(StatusEffectProcessor.StatusEffect.COMBO);
    }

    public BattleState() {
        this.prismaticManager = new PrismaticManager(this);

        this.playerFaceSelectionHistory = new HashMap<>();
        this.opponentFaceSelectionHistory = new HashMap<>();
        this.playerCumulativeAtkDef = 0;
        this.opponentCumulativeAtkDef = 0;
        this.playerCyreneThresholdMet = false;
        this.opponentCyreneThresholdMet = false;
        this.aiSelectionVisualizer = new AISelectionVisualizer();
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
        diceState.initDicePools(playerCard, opponentCard);
        resetBattleState(playerCard.getMaxHp(), opponentCard.getMaxHp(), playerIsAttacker);
        playerCumulativeAtkDef = 0;
        opponentCumulativeAtkDef = 0;
        playerCyreneThresholdMet = false;
        opponentCyreneThresholdMet = false;

        if (CharacterIds.CYRENE.equals(playerCard.getId())) {
            effectState.getPlayerEffects().addEffect(StatusEffectProcessor.StatusEffect.CYRENE_TALLY, 1);
        }
        if (CharacterIds.CYRENE.equals(opponentCard.getId())) {
            effectState.getOpponentEffects().addEffect(StatusEffectProcessor.StatusEffect.CYRENE_TALLY, 1);
        }

        prismaticManager.initializeFromCards(playerCard, opponentCard);

        CosmiconLogger.debug("BattleState initialized - Player: %s (HP %d), Opponent: %s (HP %d)",
            playerCard.getName(), hpManager.getPlayerHp(), opponentCard.getName(), hpManager.getOpponentHp());
    }

    private void resetBattleState(int playerMaxHp, int opponentMaxHp, boolean playerIsAttacker) {
        hpManager.setPlayerHp(playerMaxHp, playerCard);
        hpManager.setOpponentHp(opponentMaxHp, opponentCard);
        turnState.resetTurnNumber();
        turnState.setPlayerIsAttacker(playerIsAttacker);
        turnState.setCurrentPhase(TurnState.Phase.WAITING_NEXT_TURN);
        turnState.setWinner(null);
        playerFaceSelectionHistory.clear();
        opponentFaceSelectionHistory.clear();
    }
    
    public void setPrismaticManager(PrismaticManager manager) {
        this.prismaticManager = manager;
    }

    public void setWeatherController(WeatherController controller) {
        this.weatherController = controller;
    }

    public void addListener(BattleEventBus.BattleEventListener listener) {
        eventBus.addListener(listener);
    }

    public void removeListener(BattleEventBus.BattleEventListener listener) {
        eventBus.removeListener(listener);
    }

    public void selectPlayerDice(int index) {
        TurnState.Phase phase = turnState.getCurrentPhase();
        if (phase != TurnState.Phase.SELECTING_ATTACK && phase != TurnState.Phase.SELECTING_DEFENSE) return;
        List<Boolean> selected = diceState.getPlayerDiceSelected();
        List<Integer> values = diceState.getPlayerDiceValues();
        if (index < 0 || index >= values.size()) return;
        selected.set(index, !selected.get(index));
    }

    public int getRemainingRerolls() {
        return rerollState.getRemainingRerolls();
    }

    public boolean canReroll(boolean forPlayer) {
        TurnState.Phase phase = turnState.getCurrentPhase();
        boolean inSelection = phase == TurnState.Phase.SELECTING_ATTACK || phase == TurnState.Phase.SELECTING_DEFENSE;
        boolean isCorrectRole = (phase == TurnState.Phase.SELECTING_ATTACK && turnState.isAttacker(forPlayer)) ||
                                (phase == TurnState.Phase.SELECTING_DEFENSE && turnState.isDefender(forPlayer));
        return inSelection && isCorrectRole && getRemainingRerolls(forPlayer) > 0;
    }

    public int getRerollsUsedThisTurn() {
        return rerollState.getRerollsUsedThisTurn();
    }

    

    public int getRequiredDiceCount(boolean isPlayer) {
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        if (card == null) return 0;
        int baseLevel = (isPlayer == turnState.isPlayerAttacker()) ? card.getAtkLevel() : card.getDefLevel();
        int weatherMod = (isPlayer == turnState.isPlayerAttacker()) 
            ? (isPlayer ? turnState.getPlayerWeatherAtkMod() : turnState.getOpponentWeatherAtkMod())
            : (isPlayer ? turnState.getPlayerWeatherDefMod() : turnState.getOpponentWeatherDefMod());
        return Math.max(1, Math.min(baseLevel + weatherMod, 5));
    }

    public StatusEffectProcessor.BattleContext createBattleContext(boolean isPlayer) {
        int hp = isPlayer ? hpManager.getPlayerHp() : hpManager.getOpponentHp();
        int maxHp = isPlayer ? (playerCard != null ? playerCard.getMaxHp() : hp) : (opponentCard != null ? opponentCard.getMaxHp() : hp);
        StatusEffectProcessor.BattleContext context = new StatusEffectProcessor.BattleContext(hp, maxHp);

        List<Integer> values = diceState.getDiceValues(isPlayer);
        List<Boolean> selected = diceState.getDiceSelected(isPlayer);
        List<DiceType> types = diceState.getDiceTypes(isPlayer);

        if (values != null && selected != null) {
            context.setDiceValues(values, types);
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

    public void addPrismaticDiceToPool(PrismaticDiceInstance dice, boolean forPlayer) {
        prismaticState.addPrismaticDiceToPool(dice, forPlayer, diceState);
        prismaticManager.consumePrismaticUse(dice.type, forPlayer);
    }

    public PrismaticDiceInstance getPrismaticDiceAt(int index, boolean forPlayer) {
        return prismaticState.getPrismaticDiceAt(index, forPlayer);
    }

    public boolean isPrismaticDiceAt(int index, boolean forPlayer) {
        return prismaticState.isPrismaticDiceAt(index, forPlayer);
    }

    public void updatePrismaticDiceAt(int index, PrismaticDiceInstance newInstance, boolean forPlayer) {
        prismaticState.updatePrismaticDiceAt(index, newInstance, forPlayer);
    }

    public Map<Integer, PrismaticDiceInstance> getPrismaticDiceMap(boolean forPlayer) {
        return prismaticState.getPrismaticDiceMap(forPlayer);
    }

    public List<PrismaticDiceInstance> getSelectedPrismaticDice(boolean forPlayer) {
        return prismaticState.getSelectedPrismaticDice(forPlayer, diceState);
    }

    public CharacterCard getPlayerCard() {
        return playerCard;
    }

    public CharacterCard getOpponentCard() {
        return opponentCard;
    }

    public StatusEffectProcessor getPlayerEffects() {
        return effectState.getPlayerEffects();
    }

    public StatusEffectProcessor getOpponentEffects() {
        return effectState.getOpponentEffects();
    }

    public StatusEffectProcessor getEffects(boolean forPlayer) {
        return effectState.getEffects(forPlayer);
    }
    
    public void applyEffect(StatusEffectProcessor.StatusEffect effect, int layers, boolean toPlayer) {
        effectState.applyEffect(effect, layers, toPlayer);
    }

    public List<ModificationRecord> getModificationOrder() {
        return effectState.getModificationOrder();
    }

    public void clearModificationOrder() {
        effectState.clearModificationOrder();
    }

    public boolean hasPendingModification() {
        return effectState.hasPendingModification();
    }
    
    public void resetEffectTurnState() {
        effectState.resetEffectTurnState();
    }
    
    public void clearTemporaryEffects() {
        effectState.clearTemporaryEffects();
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
        return hpManager.getPlayerHp();
    }
    
    public void setPlayerHp(int hp) {
        hpManager.setPlayerHp(hp, playerCard);
    }

    public int getOpponentHp() {
        return hpManager.getOpponentHp();
    }
    
    public void setOpponentHp(int hp) {
        hpManager.setOpponentHp(hp, opponentCard);
    }

    public int getTurnNumber() {
        return turnState.getTurnNumber();
    }

    public boolean isPlayerAttacker() {
        return turnState.isPlayerAttacker();
    }

    public boolean isAttacker(boolean forPlayer) {
        return turnState.isAttacker(forPlayer);
    }

    public boolean isDefender(boolean forPlayer) {
        return turnState.isDefender(forPlayer);
    }

    public boolean isDefenderRolling() {
        return turnState.isDefenderRolling();
    }

    public void setDefenderRolling(boolean isDefenderRolling) {
        turnState.setDefenderRolling(isDefenderRolling);
    }

    public TurnState.Phase getCurrentPhase() {
        return turnState.getCurrentPhase();
    }

    public int getAttackValue() {
        return turnState.getAttackValue();
    }

    public int getDefenseValue() {
        return turnState.getDefenseValue();
    }

    public String getWinner() {
        return turnState.getWinner();
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


public boolean canConfirmPrismaticSelection(boolean isPlayer) {
        return prismaticManager.canConfirmPrismaticSelection(isPlayer);
    }
    
    
    
    
    
    public int getPlayerTotalDamageTaken() {
        return hpManager.getPlayerTotalDamageTaken();
    }
    
    public int getOpponentTotalDamageTaken() {
        return hpManager.getOpponentTotalDamageTaken();
    }
    
    
    
    public void recordFaceSelection(int faceValue, boolean isPlayer) {
        Map<Integer, Integer> history = isPlayer ? playerFaceSelectionHistory : opponentFaceSelectionHistory;
        history.merge(faceValue, 1, Integer::sum);
    }
    
    public void recordDamageTaken(int damage, boolean isPlayer) {
        hpManager.recordDamageTaken(damage, isPlayer);
    }
    
    public void applyDamageTo(boolean isPlayer, int damage) {
        hpManager.applyDamageTo(isPlayer, damage, getEffects(isPlayer),
            isPlayer ? playerCard : opponentCard, this::consumeUnyieldingCheck);
    }

    private void consumeUnyieldingCheck(boolean isPlayer) {
        StatusEffectProcessor effects = getEffects(isPlayer);
        if (!effects.hasEffect(StatusEffectProcessor.StatusEffect.UNYIELDING)) {
            return;
        }
        CharacterCard card = isPlayer ? playerCard : opponentCard;
        if (card != null && CharacterIds.PHAINON.equals(card.getId())) {
            consumePhainonUnyielding(isPlayer);
        }
    }
    
    public void applyHealTo(boolean isPlayer, int heal) {
        hpManager.applyHealTo(isPlayer, heal, isPlayer ? playerCard : opponentCard);
    }
    
    public Map<Integer, Integer> getPlayerFaceSelectionHistory() {
        return playerFaceSelectionHistory;
    }
    
    public Map<Integer, Integer> getOpponentFaceSelectionHistory() {
        return opponentFaceSelectionHistory;
    }
    
    public int getPrismaticTriggerCount(boolean forPlayer) {
        return prismaticState.getPrismaticTriggerCount(forPlayer);
    }

    public void incrementPrismaticTriggerCount(boolean forPlayer) {
        prismaticState.incrementPrismaticTriggerCount(forPlayer);
    }
    
    
    
    public void setDoubleValueActive(boolean isPlayer, boolean active) {
        prismaticManager.setDoubleValueActive(isPlayer, active);
    }
    
    public void addInstantDamage(boolean isPlayer, int amount) {
        prismaticManager.addInstantDamage(isPlayer, amount);
    }
    
    public void clearPrismaticState() {
        if (eventBus.isValueChangeAnimationInProgress()) {
            CosmiconLogger.warn("[PRISM_DIAG] Cannot clear prismatic state (turn %d): valueChangeAnimationInProgress, playerPrismaticDiceByIndex=%s",
                turnState.getTurnNumber(), prismaticState.getPrismaticDiceMap(true).keySet());
            return;
        }
        TurnState.Phase phase = turnState.getCurrentPhase();
        if (phase == TurnState.Phase.RESOLVING || phase == TurnState.Phase.RESOLVING_PRE_CLASH || phase == TurnState.Phase.RESOLVING_MODIFICATION) {
            CosmiconLogger.warn("[PRISM_DIAG] Cannot clear prismatic state (turn %d): phase=%s, playerPrismaticDiceByIndex=%s",
                turnState.getTurnNumber(), phase, prismaticState.getPrismaticDiceMap(true).keySet());
            return;
        }
        prismaticManager.clearState();
        prismaticState.clearDiceMaps();
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
        rerollState.setRemainingRerolls(isPlayer, count);
    }
    
    public void modifyAttackValue(int delta) {
        turnState.modifyAttackValue(delta);
    }
    
    public void modifyDefenseValue(int delta) {
        turnState.modifyDefenseValue(delta);
    }
    
    public void multiplyAttackValue(int multiplier) {
        turnState.multiplyAttackValue(multiplier);
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
        return turnState.getEffectiveAtkLevel(getCard(forPlayer), forPlayer);
    }

    public int getEffectiveDefLevel(boolean forPlayer) {
        int base = turnState.getEffectiveDefLevel(getCard(forPlayer), forPlayer);
        int pending = getPendingDefLevelBoost(forPlayer);
        if (pending > 0) {
            base = Math.max(1, Math.min(base + pending, 5));
        }
        return base;
    }

    public void modifyWeatherAtkMod(boolean forPlayer, int delta) {
        turnState.modifyWeatherAtkMod(forPlayer, delta);
    }

    public void modifyWeatherDefMod(boolean forPlayer, int delta) {
        turnState.modifyWeatherDefMod(forPlayer, delta);
    }

    public void clearWeatherMods() {
        turnState.clearWeatherMods();
    }
    
    public int getRemainingRerolls(boolean forPlayer) {
        return rerollState.getRemainingRerolls(forPlayer);
    }
    
    public int getRerollsUsedThisTurn(boolean forPlayer) {
        return rerollState.getRerollsUsedThisTurn(forPlayer);
    }
    
    public void decrementRerolls(boolean forPlayer) {
        rerollState.decrementRerolls(forPlayer);
    }
    
    public void incrementRerollsUsed(boolean forPlayer) {
        rerollState.incrementRerollsUsed(forPlayer);
    }
    
    public void resetRerollsUsedThisTurn() {
        rerollState.resetRerollsUsedThisTurn();
    }
    
    public void setCurrentPhase(TurnState.Phase phase) {
        turnState.setCurrentPhase(phase);
    }
    
    public void setAttackValue(int value) {
        turnState.setAttackValue(value);
    }
    
    public void setDefenseValue(int value) {
        turnState.setDefenseValue(value);
    }
    
    public void incrementTurnNumber() {
        turnState.incrementTurnNumber();
    }
    
    public void swapAttackerDefender() {
        turnState.swapAttackerDefender();
    }
    
    public int calculateSelectedSum(boolean forPlayer) {
        return calculateSelectedSumExcludingPrismatic(diceState.getDiceValues(forPlayer), diceState.getDiceSelected(forPlayer), forPlayer);
    }
    
    private int calculateSelectedSumExcludingPrismatic(List<Integer> values, List<Boolean> selected, boolean forPlayer) {
        if (values == null || selected == null) return 0;
        int sum = 0;
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i) && !prismaticState.isPrismaticDiceAt(i, forPlayer)) {
                sum += values.get(i);
            }
        }
        return sum;
    }
    
    public void recordSelectedFaces(boolean forPlayer) {
        List<Integer> values = diceState.getDiceValues(forPlayer);
        List<Boolean> selected = diceState.getDiceSelected(forPlayer);
        if (values != null && selected != null) {
            for (int i = 0; i < selected.size(); i++) {
                if (selected.get(i)) {
                    recordFaceSelection(values.get(i), forPlayer);
                }
            }
        }
    }
    
    public String getSelectedDiceValuesFormatted(boolean forPlayer) {
        List<Integer> values = diceState.getDiceValues(forPlayer);
        List<Boolean> selected = diceState.getDiceSelected(forPlayer);
        if (values == null || selected == null) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                if (!sb.isEmpty()) sb.append("+");
                if (prismaticState.isPrismaticDiceAt(i, forPlayer)) {
                    sb.append("*").append(values.get(i));
                } else {
                    sb.append(values.get(i));
                }
            }
        }
        return sb.toString();
    }
    
    
    
    public void notifyPhaseChange(TurnState.Phase phase) {
        eventBus.notifyPhaseChange(phase);
    }

    public void notifyDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices) {
        eventBus.notifyDiceRerolled(isPlayer, newValues, rerolledIndices);
    }

    public void notifyDamageResolved(int damage, int pHp, int oHp) {
        eventBus.notifyDamageResolved(damage, pHp, oHp);
    }

    public void notifyBattleEnd(String winner) {
        eventBus.notifyBattleEnd(winner, hpManager.getPlayerHp(), hpManager.getOpponentHp(), hpManager.getPlayerTotalDamageTaken(), hpManager.getOpponentTotalDamageTaken());
    }

    public void notifyDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        eventBus.notifyDiceRolled(isPlayer, types, values);
    }

    public void notifyTransitionToDefenderRoll() {
        eventBus.notifyTransitionToDefenderRoll();
    }



    public void setWinner(String winner) {
        turnState.setWinner(winner);
    }

    public void notifyWeatherChange(WeatherType newWeather) {
        eventBus.notifyWeatherChange(newWeather);
    }

    public void notifyDamageAnimationStart(DamageResolver.DamageResult result) {
        eventBus.notifyDamageAnimationStart(result);
    }

    public void notifyDamageAnimationComplete() {
        eventBus.notifyDamageAnimationComplete();
    }

    public void notifyDamageImpacted() {
        eventBus.notifyDamageImpacted();
    }

    public void queueValueChange(boolean isPlayer, String changeType, int delta) {
        eventBus.queueValueChange(isPlayer, changeType, delta);
    }

    public List<BattleEventBus.ValueChangeRecord> getPendingValueChanges(boolean isPlayer) {
        return eventBus.getPendingValueChanges(isPlayer);
    }

    public void clearPendingValueChanges() {
        eventBus.clearPendingValueChanges();
    }

    public void setValueChangeAnimationInProgress(boolean inProgress) {
        eventBus.setValueChangeAnimationInProgress(inProgress);
    }

    public void notifyValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta) {
        eventBus.notifyValueChange(isPlayer, changeType, oldValue, newValue, delta);
    }

    public void notifySecondaryDamage(boolean isPlayer, int damage, String damageType) {
        eventBus.notifySecondaryDamage(isPlayer, damage, damageType);
    }

    public void notifyHeal(boolean isPlayer, int heal) {
        eventBus.notifyHeal(isPlayer, heal);
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


    public void setPendingStrength(boolean forPlayer, int strength) {
        if (forPlayer) {
            playerPendingStrength = strength;
        } else {
            opponentPendingStrength = strength;
        }
    }

    public int consumePendingStrength(boolean forPlayer) {
        int val;
        if (forPlayer) {
            val = playerPendingStrength;
            playerPendingStrength = 0;
        } else {
            val = opponentPendingStrength;
            opponentPendingStrength = 0;
        }
        return val;
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
            ? (turnState.isPlayerAttacker() ? turnState.getAttackValue() : turnState.getDefenseValue())
            : (turnState.isPlayerAttacker() ? turnState.getDefenseValue() : turnState.getAttackValue());
        addCumulativeAtkDef(forPlayer, value);
    }
    
    public AISelectionVisualizer getAiSelectionVisualizer() {
        return aiSelectionVisualizer;
    }
    
    public String getAttackerConfirmedSelectionText() {
        return turnState.getAttackerConfirmedSelectionText();
    }
    
    
    
    public void setAttackerConfirmedSelection(String text) {
        turnState.setAttackerConfirmedSelection(text);
    }
    
    public String getDefenderConfirmedSelectionText() {
        return turnState.getDefenderConfirmedSelectionText();
    }
    
    
    
    public void setDefenderConfirmedSelection(String text) {
        turnState.setDefenderConfirmedSelection(text);
    }
    
    public void clearConfirmedSelections() {
        turnState.clearConfirmedSelections();
    }
    
    public void cleanup() {
        CosmiconLogger.debug("BattleState cleanup - clearing listeners and state");

        eventBus.cleanup();

        effectState.cleanup();
        
        prismaticManager.clearState();
        
        diceState.cleanup();
        
        prismaticState.cleanup();
        
        weatherController = null;
        
        playerFaceSelectionHistory.clear();
        opponentFaceSelectionHistory.clear();
        playerCumulativeAtkDef = 0;
        opponentCumulativeAtkDef = 0;
        playerCyreneThresholdMet = false;
        opponentCyreneThresholdMet = false;
        hpManager.cleanup();
        playerPendingDefLevelBoost = 0;
        opponentPendingDefLevelBoost = 0;
        playerOriginalDefLevel = 0;
        opponentOriginalDefLevel = 0;
        phainonUnyieldingAvailable = true;
        opponentPhainonUnyieldingAvailable = true;
        aiSelectionVisualizer.reset();
        
        turnState.cleanup();
        rerollState.cleanup();
        
        CosmiconLogger.debug("BattleState cleanup complete");
    }
}
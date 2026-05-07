package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import data.scripts.cosmicon.util.CosmiconLogger;

public class BattleEventBus {

    public interface BattleEventListener {
        void onPhaseChange(TurnState.Phase newPhase);
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
        void onSecondaryDamage(boolean isPlayer, int damage, String damageType);
    }

    public record ValueChangeRecord(String changeType, int delta, String displayText, boolean isPlayer)
    {
    }

    public interface DamageAnimationCallback {
        void onDamageAnimationComplete();
    }

    private final List<BattleEventListener> listeners;
    private DamageAnimationCallback damageAnimationCallback;
    private final List<ValueChangeRecord> pendingValueChanges;
    private boolean valueChangeAnimationInProgress;

    public BattleEventBus() {
        this.listeners = new ArrayList<>();
        this.pendingValueChanges = new ArrayList<>();
        this.valueChangeAnimationInProgress = false;
    }

    public void addListener(BattleEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BattleEventListener listener) {
        listeners.remove(listener);
    }

    public void setDamageAnimationCallback(DamageAnimationCallback callback) {
        this.damageAnimationCallback = callback;
    }

    public boolean isValueChangeAnimationInProgress() {
        return valueChangeAnimationInProgress;
    }

    public void notifyPhaseChange(TurnState.Phase phase) {
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

    public void notifyBattleEnd(String winner, int playerHp, int opponentHp,
                                int playerTotalDamageTaken, int opponentTotalDamageTaken) {
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

    public void notifyValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta) {
        for (BattleEventListener l : listeners) {
            l.onValueChange(isPlayer, changeType, oldValue, newValue, delta);
        }
    }

    public void notifySecondaryDamage(boolean isPlayer, int damage, String damageType) {
        for (BattleEventListener l : listeners) {
            l.onSecondaryDamage(isPlayer, damage, damageType);
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

    public void cleanup() {
        listeners.clear();
        pendingValueChanges.clear();
        valueChangeAnimationInProgress = false;
    }
}

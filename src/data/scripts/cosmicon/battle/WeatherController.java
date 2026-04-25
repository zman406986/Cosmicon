package data.scripts.cosmicon.battle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.util.CosmiconLogger;

public class WeatherController {
    
    private final WeatherManager schedule;
    
    public WeatherController() {
        this.schedule = new WeatherManager();
    }
    
    public WeatherType getCurrentWeather() {
        return schedule.getCurrentWeather();
    }
    
    public void advanceTurn() {
        WeatherType oldWeather = schedule.getCurrentWeather();
        schedule.advanceTurn();
        WeatherType newWeather = schedule.getCurrentWeather();
        if (oldWeather != newWeather) {
            CosmiconLogger.debug("Weather changed: %s -> %s", oldWeather, newWeather);
        }
    }
    
    public void applyStartOfBattle(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case SUNNY, THE_DECISIVE_MOMENT -> {
                state.getPlayerEffects().addEffect(StatusEffect.STRENGTH, 5);
                state.getOpponentEffects().addEffect(StatusEffect.STRENGTH, 5);
            }
            case TOXIC_FOG -> {
                state.getPlayerEffects().addEffect(StatusEffect.POISON, 2);
                state.getOpponentEffects().addEffect(StatusEffect.POISON, 2);
            }
            case VENOCLLOUD -> {
                state.getPlayerEffects().addEffect(StatusEffect.VENOM, 1);
                state.getOpponentEffects().addEffect(StatusEffect.VENOM, 1);
            }
            default -> {}
        }
    }
    
    public void applyStartOfTurn(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case ACID_RAIN -> {
                int higherHpSide = state.getPlayerHp() >= state.getOpponentHp() ? 0 : 1;
                if (higherHpSide == 0) {
                    state.getPlayerEffects().addEffect(StatusEffect.POISON, 1);
                } else {
                    state.getOpponentEffects().addEffect(StatusEffect.POISON, 1);
                }
            }
            case HIGH_TEMPERATURE -> {
                int lowerHpSide = state.getPlayerHp() <= state.getOpponentHp() ? 0 : 1;
                if (lowerHpSide == 0) {
                    state.getPlayerEffects().addEffect(StatusEffect.STRENGTH, 2);
                } else {
                    state.getOpponentEffects().addEffect(StatusEffect.STRENGTH, 2);
                }
            }
            case STORM -> {
                state.modifyCardAtkLevel(true, 1);
                CharacterCard playerCard = state.getPlayerCard();
                if (playerCard != null) {
                    playerCard.setDefLevel(playerCard.getDefLevel() + 1);
                }
                state.modifyCardAtkLevel(false, 1);
                CharacterCard opponentCard = state.getOpponentCard();
                if (opponentCard != null) {
                    opponentCard.setDefLevel(opponentCard.getDefLevel() + 1);
                }
            }
            default -> {}
        }
    }
    
    public void applyRerollPhase(BattleState state, int attackerBaseRerolls) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case FISH_RAIN -> {
                state.setRemainingRerolls(state.isPlayerAttacker(), attackerBaseRerolls + 1);
                CosmiconLogger.debug("%s: +1 reroll for attacker", weather);
            }
            case PARHELION -> {
                state.setRemainingRerolls(state.isPlayerAttacker(), attackerBaseRerolls + 2);
                CosmiconLogger.debug("%s: +2 rerolls for attacker", weather);
            }
            default -> {}
        }
    }
    
    public void applySelectionPhase(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        List<Integer> values = isPlayer ? state.getPlayerDiceValues() : state.getOpponentDiceValues();
        List<Boolean> selected = isPlayer ? state.getPlayerDiceSelected() : state.getOpponentDiceSelected();
        int sum = calculateSelectedSum(values, selected);
        boolean isAttacker = state.isAttacker(isPlayer);
        
        switch (weather) {
            case SOLAR_ECLIPSE -> {
                if (isAttacker) {
                    boolean allDifferent = checkAllDifferentValues(values, selected);
                    if (allDifferent) {
                        state.modifyAttackValue(4);
                    }
                }
            }
            case DUST -> {
                if (isAttacker) {
                    boolean allOdd = checkAllOddValues(values, selected);
                    if (allOdd) {
                        state.getEffects(isPlayer).addEffect(StatusEffect.STRENGTH, 3);
                    }
                }
            }
            case RAINBOW -> {
                if (isAttacker && sum <= 10) {
                    state.getEffects(isPlayer).addEffect(StatusEffect.PERFORATION, 1);
                }
            }
            case LUNISOLAR_LUMINANCE -> {
                if (isAttacker) {
                    int hp = isPlayer ? state.getPlayerHp() : state.getOpponentHp();
                    if (hp <= 3) {
                        state.multiplyAttackValue(2);
                    }
                }
            }
            case HEAVY_SNOW -> {
                boolean hasSeven = checkContainsValue(values, selected, 7);
                if (hasSeven) {
                    if (isAttacker) {
                        state.modifyAttackValue(4);
                    } else {
                        state.modifyDefenseValue(4);
                    }
                }
            }
            case DROUGHT -> {
                if (isAttacker) {
                    int defLevel = isPlayer ? state.getOpponentCard().getDefLevel() : state.getPlayerCard().getDefLevel();
                    int extraAttack = defLevel * 3;
                    state.modifyAttackValue(extraAttack);
                }
            }
            case CREPUSCULAR_RAYS -> {
                if (isAttacker) {
                    int attackerHp = isPlayer ? state.getPlayerHp() : state.getOpponentHp();
                    int defenderHp = isPlayer ? state.getOpponentHp() : state.getPlayerHp();
                    if (attackerHp < defenderHp) {
                        state.getEffects(isPlayer).addEffect(StatusEffect.COMBO, 1);
                    }
                }
            }
            case MODERATE_SNOW -> {
                Map<Integer, Integer> counts = countSelectedValues(values, selected);
                for (int count : counts.values()) {
                    if (count >= 3) {
                        state.applyHealTo(isPlayer, 10);
                        break;
                    }
                }
            }
            case DRIZZLE -> {
                boolean hasSix = checkContainsValue(values, selected, 6);
                if (hasSix) {
                    state.getEffects(isPlayer).removeEffect(StatusEffect.POISON);
                    CosmiconLogger.debug("DRIZZLE: Removed POISON from %s (rolled a 6)", isPlayer ? "Player" : "Opponent");
                }
            }
            default -> {}
        }
    }
    
    public void applyDefenderSelectionPhase(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        List<Integer> values = state.getOpponentDiceValues();
        List<Boolean> selected = state.getOpponentDiceSelected();
        
        switch (weather) {
            case BLIZZARD -> {
                if (state.getDefenseValue() <= 8) {
                    state.getOpponentEffects().addEffect(StatusEffect.FORCEFIELD, 1);
                }
            }
            case FROST -> {
                Map<Integer, Integer> counts = countSelectedValues(values, selected);
                boolean hasMatch = counts.values().stream().anyMatch(c -> c >= 2);
                if (hasMatch) {
                    state.getOpponentCard().setDefLevel(state.getOpponentCard().getDefLevel() + 1);
                }
            }
            case SLEET -> {
                if (state.getOpponentHp() < state.getOpponentCard().getMaxHp()) {
                    state.getOpponentEffects().addEffect(StatusEffect.COUNTER, 1);
                    state.getOpponentCard().setDefLevel(state.getOpponentCard().getDefLevel() + 2);
                }
            }
            default -> {}
        }
    }
    
    public void applyPreResolution(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case DRY_THUNDERSTORM -> {
                if (state.isPlayerAttacker()) {
                    state.addInstantDamage(false, 3);
                }
            }
            case CYCLONIC_SWARM -> {
                if (state.isPlayerAttacker()) {
                    state.getPlayerEffects().addEffect(StatusEffect.COMBO, 1);
                }
            }
            case TEMPORAL_STORM -> {
                if (state.isPlayerAttacker()) {
                    List<Integer> values = state.getPlayerDiceValues();
                    List<Boolean> selected = state.getPlayerDiceSelected();
                    boolean allSixes = checkAllSixes(values, selected);
                    int selectedCount = countSelected(selected);
                    if (allSixes && selectedCount > 0) {
                        int temp = state.getPlayerHp();
                        state.setPlayerHp(state.getOpponentHp());
                        state.setOpponentHp(temp);
                    }
                }
            }
            default -> {}
        }
    }
    
    public void applyRerollThornsEffect(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == WeatherType.PARHELION) {
            state.getEffects(isPlayer).addEffect(StatusEffect.THORNS, 2);
        }
    }
    
    public void applyRerollGlidingEffect(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == WeatherType.THUNDERSTORM) {
            state.modifyCardAtkLevel(isPlayer, 2);
            CharacterCard card = state.getCard(isPlayer);
            if (card != null) {
                card.setDefLevel(card.getDefLevel() + 2);
            }
        }
    }
    
    public boolean shouldPreventMinimumRoll() {
        WeatherType weather = getCurrentWeather();
        return weather == WeatherType.FROG_RAIN;
    }
    
    public boolean shouldPreventMaxRoll(boolean isDefender) {
        WeatherType weather = getCurrentWeather();
        return weather == WeatherType.SUNSHOWER && isDefender;
    }
    
    public float getSiphonMultiplier() {
        WeatherType weather = getCurrentWeather();
        return weather == WeatherType.SCORCHING_SUN ? 0.5f : 0f;
    }
    
    public void applyWeatherTransitionEffect(BattleState state, WeatherType oldWeather, WeatherType newWeather) {
        if (newWeather == WeatherType.SEA_OF_CLOUDS && oldWeather != WeatherType.SEA_OF_CLOUDS) {
            state.addPrismaticUse(1);
            for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
                state.addPrismaticUseByType(type, true, 1);
                state.addPrismaticUseByType(type, false, 1);
            }
        }
    }
    
    public boolean shouldApplyFineSnowEffect(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather != WeatherType.FINE_SNOW) return false;
        if (state.isAttacker(isPlayer) && state.getRerollsUsedThisTurn() == 0) {
            return true;
        }
        return false;
    }
    
    private int calculateSelectedSum(List<Integer> values, List<Boolean> selected) {
        int sum = 0;
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                sum += values.get(i);
            }
        }
        return sum;
    }
    
    private int countSelected(List<Boolean> selected) {
        int count = 0;
        for (Boolean b : selected) {
            if (b) count++;
        }
        return count;
    }
    
    private boolean checkAllDifferentValues(List<Integer> values, List<Boolean> selected) {
        int lastValue = -1;
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                if (lastValue != -1 && values.get(i) == lastValue) {
                    return false;
                }
                lastValue = values.get(i);
            }
        }
        return true;
    }
    
    private boolean checkAllOddValues(List<Integer> values, List<Boolean> selected) {
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i) && values.get(i) % 2 == 0) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkContainsValue(List<Integer> values, List<Boolean> selected, int targetValue) {
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i) && values.get(i) == targetValue) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkAllSixes(List<Integer> values, List<Boolean> selected) {
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i) && values.get(i) != 6) {
                return false;
            }
        }
        return true;
    }
    
    private Map<Integer, Integer> countSelectedValues(List<Integer> values, List<Boolean> selected) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                counts.merge(values.get(i), 1, Integer::sum);
            }
        }
        return counts;
    }
}
package data.scripts.cosmicon.battle;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.battle.StatusEffectProcessor.DurationType;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.util.CosmiconLogger;

public class WeatherController {
    
    private final WeatherManager schedule;
    
    public WeatherController() {
        this.schedule = new WeatherManager();
    }
    
    public WeatherManager getWeatherManager() {
        return schedule;
    }
    
    public WeatherType getCurrentWeather() {
        return schedule.getCurrentWeather();
    }
    
    public void advanceTurn(boolean allowSafeguard, boolean allowAttack) {
        WeatherType oldWeather = schedule.getCurrentWeather();
        schedule.advanceTurn(allowSafeguard, allowAttack);
        WeatherType newWeather = schedule.getCurrentWeather();
        if (oldWeather != newWeather) {
            CosmiconLogger.debug("Weather changed: %s -> %s", oldWeather, newWeather);
        }
    }
    
    public void applyWeatherAppearanceEffects(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case SUNNY, THE_DECISIVE_MOMENT -> {
                String src = weather.name();
                state.getPlayerEffects().addEffect(StatusEffect.STRENGTH, src, 5, DurationType.PERMANENT);
                state.getOpponentEffects().addEffect(StatusEffect.STRENGTH, src, 5, DurationType.PERMANENT);
            }
            case VENOCLLOUD -> {
                String src = weather.name();
                state.getPlayerEffects().addEffect(StatusEffect.VENOM, src, 1, DurationType.PERMANENT);
                state.getOpponentEffects().addEffect(StatusEffect.VENOM, src, 1, DurationType.PERMANENT);
            }
            case SEA_OF_CLOUDS -> {
                for (int i = 0; i < 2; i++) {
                    boolean isPlayer = (i == 0);
                    CharacterCard card = state.getCard(isPlayer);
                    Map<String, Integer> prismaticDiceIds = card != null
                        ? card.getPrismaticDiceIds() : Collections.emptyMap();
                    if (!prismaticDiceIds.isEmpty()) {
                        state.addPrismaticUse(isPlayer, 1);
                        for (String diceId : prismaticDiceIds.keySet()) {
                            PrismaticDiceType type = PrismaticDiceRegistry.get(diceId);
                            if (type != null) {
                                state.addPrismaticUseByType(type, isPlayer, 1);
                            }
                        }
                    }
                }
            }
            default -> {}
        }
    }
    
    public void applyStartOfBattle() {
    }
    
    public void applyStartOfTurn(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        if (!schedule.isFullTurnBoundary()) return;
        
        switch (weather) {
            case TOXIC_FOG -> {
                String src = weather.name();
                state.getPlayerEffects().addEffect(StatusEffect.POISON, src, 2, DurationType.PERMANENT);
                state.getOpponentEffects().addEffect(StatusEffect.POISON, src, 2, DurationType.PERMANENT);
            }
            case ACID_RAIN -> {
                String src = weather.name();
                int playerHp = state.getPlayerHp();
                int opponentHp = state.getOpponentHp();
                if (playerHp > opponentHp) {
                    state.getPlayerEffects().addEffect(StatusEffect.POISON, src, 1, DurationType.PERMANENT);
                } else if (opponentHp > playerHp) {
                    state.getOpponentEffects().addEffect(StatusEffect.POISON, src, 1, DurationType.PERMANENT);
                } else {
                    state.getPlayerEffects().addEffect(StatusEffect.POISON, src, 1, DurationType.PERMANENT);
                }
            }
            case HIGH_TEMPERATURE -> {
                String src = weather.name();
                int playerHp = state.getPlayerHp();
                int opponentHp = state.getOpponentHp();
                if (playerHp < opponentHp) {
                    state.getPlayerEffects().addEffect(StatusEffect.STRENGTH, src, 2, DurationType.PERMANENT);
                } else if (opponentHp < playerHp) {
                    state.getOpponentEffects().addEffect(StatusEffect.STRENGTH, src, 2, DurationType.PERMANENT);
                } else {
                    state.getPlayerEffects().addEffect(StatusEffect.STRENGTH, src, 2, DurationType.PERMANENT);
                }
            }
            default -> {}
        }
    }
    
    public void applyRerollPhase(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case FISH_RAIN -> {
                state.setRemainingRerolls(true, state.getRemainingRerolls(true) + 1);
                state.setRemainingRerolls(false, state.getRemainingRerolls(false) + 1);
                CosmiconLogger.debug("%s: +1 reroll for both sides", weather);
            }
            case PARHELION -> {
                boolean attackerIsPlayer = state.isPlayerAttacker();
                state.setRemainingRerolls(attackerIsPlayer, state.getRemainingRerolls(attackerIsPlayer) + 2);
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
        int sum = state.calculateSelectedSum(isPlayer);
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
                        state.getEffects(isPlayer).addEffect(StatusEffect.STRENGTH, weather.name(), 3, DurationType.USAGE_BASED);
                    }
                }
            }
            case RAINBOW -> {
                if (isAttacker && sum <= 10) {
                    state.getEffects(isPlayer).addEffect(StatusEffect.PERFORATION, weather.name(), 1, DurationType.USAGE_BASED);
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
                    int defLevel = state.getEffectiveDefLevel(!isPlayer);
                    int extraAttack = defLevel * 3;
                    state.modifyAttackValue(extraAttack);
                }
            }
            case CREPUSCULAR_RAYS -> {
                if (isAttacker) {
                    int attackerHp = isPlayer ? state.getPlayerHp() : state.getOpponentHp();
                    int defenderHp = isPlayer ? state.getOpponentHp() : state.getPlayerHp();
                    if (attackerHp < defenderHp) {
                        state.getEffects(isPlayer).addEffect(StatusEffect.COMBO, weather.name(), 1, DurationType.USAGE_BASED);
                    }
                }
            }
            case MODERATE_SNOW -> {
                int[] freq = new int[13];
                for (int i = 0; i < values.size(); i++) {
                    if (selected.get(i)) {
                        int v = values.get(i);
                        if (v >= 1 && v <= 12 && ++freq[v] >= 3) {
                            state.applyHealTo(isPlayer, 10);
                            state.notifyHeal(isPlayer, 10);
                            break;
                        }
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
    
    public void applyPostModificationPhase(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;

        if (weather == WeatherType.LUNISOLAR_LUMINANCE) {
            boolean attackerIsPlayer = state.isPlayerAttacker();
            int hp = attackerIsPlayer ? state.getPlayerHp() : state.getOpponentHp();
            if (hp <= 3) {
                state.multiplyAttackValue(2);
            }
        }
    }
    
    public void applyDefenderPreSelectionEffects(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;

        if (weather == WeatherType.SLEET)
        {
            int hp = isPlayer ? state.getPlayerHp() : state.getOpponentHp();
            CharacterCard card = state.getCard(isPlayer);
            if (card != null && hp < card.getMaxHp())
            {
                state.getEffects(isPlayer).addEffect(StatusEffect.COUNTER, weather.name(), 1, DurationType.USAGE_BASED);
                state.modifyWeatherDefMod(isPlayer, 2);
                CosmiconLogger.debug("%s: COUNTER + DEF+2 for defender (%s) [pre-selection]", weather, isPlayer ? "Player" : "Opponent");
            }
        }
    }

    public void applyPersistentWeatherMods(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;

        if (weather == WeatherType.STORM)
        {
            state.modifyWeatherAtkMod(true, 1);
            state.modifyWeatherDefMod(true, 1);
            state.modifyWeatherAtkMod(false, 1);
            state.modifyWeatherDefMod(false, 1);
        }
    }

    public void clearWeatherStatusEffects(BattleState state, WeatherType oldWeather) {
        if (oldWeather == null) return;
        switch (oldWeather) {
            case SUNNY, THE_DECISIVE_MOMENT -> {
                String src = oldWeather.name();
                state.getPlayerEffects().removeLayersFromSource(StatusEffect.STRENGTH, src, 5);
                state.getOpponentEffects().removeLayersFromSource(StatusEffect.STRENGTH, src, 5);
            }
            case HIGH_TEMPERATURE -> {
                String src = oldWeather.name();
                state.getPlayerEffects().removeLayersFromSource(StatusEffect.STRENGTH, src, 2);
                state.getOpponentEffects().removeLayersFromSource(StatusEffect.STRENGTH, src, 2);
            }
            default -> {}
        }
    }

    public void applyDefenderSelectionPhase(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case BLIZZARD -> {
                if (state.getDefenseValue() <= 8) {
                    state.getEffects(isPlayer).addEffect(StatusEffect.FORCEFIELD, weather.name(), 1, DurationType.USAGE_BASED);
                    CosmiconLogger.debug("%s: FORCEFIELD granted to defender (%s)", weather, isPlayer ? "Player" : "Opponent");
                }
            }
            case FROST -> {
                boolean attackerIsPlayer = !isPlayer;
                List<Integer> attackerValues = state.getDiceValues(attackerIsPlayer);
                List<Boolean> attackerSelected = state.getDiceSelected(attackerIsPlayer);
                int[] freq = new int[13];
                boolean hasMatch = false;
                for (int i = 0; i < attackerValues.size(); i++) {
                    if (attackerSelected.get(i)) {
                        int v = attackerValues.get(i);
                        if (v >= 1 && v <= 12 && ++freq[v] >= 2) {
                            hasMatch = true;
                            break;
                        }
                    }
                }
                if (hasMatch) {
                    state.setPendingDefLevelBoost(attackerIsPlayer, 1);
                    CosmiconLogger.debug("%s: DEF level +1 next turn for attacker (%s)", weather, attackerIsPlayer ? "Player" : "Opponent");
                }
            }
            default -> {}
        }
    }
    
    public void applyPreResolution(BattleState state) {
        WeatherType weather = getCurrentWeather();
        if (weather == null) return;
        
        switch (weather) {
            case DRY_THUNDERSTORM -> state.addInstantDamage(!state.isPlayerAttacker(), 3);
            case CYCLONIC_SWARM -> {
                boolean attackerIsPlayer = state.isPlayerAttacker();
                state.getEffects(attackerIsPlayer).addEffect(StatusEffect.COMBO, weather.name(), 1, DurationType.USAGE_BASED);
            }
            case TEMPORAL_STORM -> {
                boolean attackerIsPlayer = state.isPlayerAttacker();
                List<Integer> values = state.getDiceValues(attackerIsPlayer);
                List<Boolean> selected = state.getDiceSelected(attackerIsPlayer);
                boolean allSixes = checkAllSixes(values, selected);
                int selectedCount = countSelected(selected);
                if (allSixes && selectedCount > 0) {
                    int temp = state.getPlayerHp();
                    state.setPlayerHp(state.getOpponentHp());
                    state.setOpponentHp(temp);
                }
            }
            default -> {}
        }
    }
    
    public void applyRerollThornsEffect(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather == WeatherType.PARHELION) {
            state.getEffects(isPlayer).addEffect(StatusEffect.THORNS, weather.name(), 2, DurationType.TURN_BASED);
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
    
    public boolean shouldApplyFineSnowEffect(BattleState state, boolean isPlayer) {
        WeatherType weather = getCurrentWeather();
        if (weather != WeatherType.FINE_SNOW) return false;
        if (state.isAttacker(isPlayer) && state.getRerollsUsedThisTurn(isPlayer) == 0) {
            return true;
        }
        return false;
    }
    
    private int countSelected(List<Boolean> selected) {
        int count = 0;
        for (Boolean aBoolean : selected)
        {
            if (aBoolean) count++;
        }
        return count;
    }
    
    private boolean checkAllDifferentValues(List<Integer> values, List<Boolean> selected) {
        boolean[] seen = new boolean[13];
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                int v = values.get(i);
                if (v >= 1 && v <= 12) {
                    if (seen[v]) return false;
                    seen[v] = true;
                }
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
    
    public void reset() {
        schedule.reset();
    }
}

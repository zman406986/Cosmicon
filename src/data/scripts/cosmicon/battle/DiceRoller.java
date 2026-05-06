package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.character.PassiveEventSystem;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.CosmiconRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceRoller {
    
    private final WeatherController weatherController;
    private TutorialDiceRoller tutorialDiceRoller;
    
    public DiceRoller(WeatherController weatherController) {
        this.weatherController = weatherController;
    }
    
    public void setTutorialDiceRoller(TutorialDiceRoller tutorialDiceRoller) {
        this.tutorialDiceRoller = tutorialDiceRoller;
    }
    
    public void rollForAttacker(BattleState state) {
        boolean attackerIsPlayer = state.isPlayerAttacker();
        rollForParticipant(state, attackerIsPlayer);
    }
    
    public void rollForDefender(BattleState state) {
        boolean defenderIsPlayer = !state.isPlayerAttacker();
        rollForParticipant(state, defenderIsPlayer);
    }
    
    private int getMinRoll() {
        if (weatherController != null && weatherController.shouldPreventMinimumRoll()) {
            return 2;
        }
        return 1;
    }
    
    private int getMaxRoll(int typeMaxFace, boolean isAttacker) {
        if (weatherController != null && weatherController.shouldPreventMaxRoll(!isAttacker)) {
            return Math.max(1, typeMaxFace - 1);
        }
        return typeMaxFace;
    }

    public void rollForParticipant(BattleState state, boolean forPlayer) {
        if (tutorialDiceRoller != null) {
            tutorialDiceRoller.rollForParticipant(state, forPlayer);
            return;
        }

        CharacterCard card = state.getCard(forPlayer);
        boolean isAttacker = state.isAttacker(forPlayer);

        List<DiceType> types = state.getUpgradedDicePool(forPlayer);
        if (types == null) {
            types = card.getDicePool();
        }
        List<Integer> values = new ArrayList<>();
        List<Boolean> selected = new ArrayList<>();
        
        int minRoll = getMinRoll();
        
        for (DiceType type : types) {
            int maxRoll = getMaxRoll(type.getMaxFace(), isAttacker);
            
            int value = CosmiconRandom.nextInt(maxRoll - minRoll + 1) + minRoll;
            values.add(value);
            selected.add(false);
        }
        
        state.setDiceValues(forPlayer, values);
        state.setDiceTypes(forPlayer, types);
        state.setDiceSelected(forPlayer, selected);
        
        state.notifyDiceRolled(forPlayer, types, values);
        
        logDiceRoll(card.getName(), types, values, isAttacker);
    }
    
    private void logDiceRoll(String character, List<DiceType> types, List<Integer> values, boolean isAttacker) {
        StringBuilder sb = new StringBuilder();
        sb.append(character).append(" (").append(isAttacker ? "Attacker" : "Defender").append(") rolled: ");
        int total = 0;
        for (int i = 0; i < types.size(); i++) {
            DiceType type = types.get(i);
            int value = values.get(i);
            total += value;
            if (i > 0) sb.append(" + ");
            sb.append(type.name()).append("(").append(value).append(")");
        }
        sb.append(" = ").append(total);
        CosmiconLogger.debug(sb.toString());
    }
    
    public void rerollSelected(BattleState state, boolean forPlayer) {
        if (tutorialDiceRoller != null && tutorialDiceRoller.shouldInterceptReroll(forPlayer)) {
            tutorialDiceRoller.rerollSelected(state, forPlayer);
            PassiveEventSystem.onRerollCompleted(state, forPlayer);
            if (weatherController != null) {
                weatherController.applyRerollThornsEffect(state, forPlayer);
            }

            List<Integer> rerolledIndices = new ArrayList<>();
            List<Boolean> selected = state.getDiceSelected(forPlayer);
            for (int i = 0; i < selected.size(); i++) {
                if (selected.get(i)) {
                    rerolledIndices.add(i);
                }
            }

            state.notifyDiceRerolled(forPlayer, state.getDiceValues(forPlayer), rerolledIndices);
            return;
        }

        List<Integer> values = state.getDiceValues(forPlayer);
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        List<DiceType> types = state.getDiceTypes(forPlayer);
        List<Integer> rerolledIndices = new ArrayList<>();
        
        boolean isAttacker = state.isAttacker(forPlayer);
        int minRoll = getMinRoll();
        
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                DiceType type = types.get(i);
                
                boolean isPrismatic = state.isPrismaticDiceAt(i, forPlayer);
                if (isPrismatic) {
                    PrismaticDiceInstance existingInstance = state.getPrismaticDiceAt(i, forPlayer);
                    if (existingInstance != null) {
                        Random random = CosmiconRandom.getRandom();
                        PrismaticDiceInstance newInstance = PrismaticDiceInstance.roll(
                            existingInstance.type, existingInstance.isTrueVersion, random);
                        values.set(i, newInstance.rolledFace);
                        state.updatePrismaticDiceAt(i, newInstance, forPlayer);
                    }
                } else {
                    int maxRoll = getMaxRoll(type.getMaxFace(), isAttacker);
                    
                    int value = CosmiconRandom.nextInt(maxRoll - minRoll + 1) + minRoll;
                    values.set(i, value);
                }
                rerolledIndices.add(i);
            }
        }
        
        state.setDiceValues(forPlayer, values);
        state.decrementRerolls(forPlayer);
        state.incrementRerollsUsed(forPlayer);
        
        PassiveEventSystem.onRerollCompleted(state, forPlayer);
        
        if (weatherController != null) {
            weatherController.applyRerollThornsEffect(state, forPlayer);
        }
        
        state.notifyDiceRerolled(forPlayer, values, rerolledIndices);
        
        logReroll(state.getCard(forPlayer).getName(), types, values, rerolledIndices);
    }
    
    private void logReroll(String character, List<DiceType> types, List<Integer> values, List<Integer> rerolledIndices) {
        if (rerolledIndices.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(character).append(" rerolled: ");
        for (int i = 0; i < rerolledIndices.size(); i++) {
            int idx = rerolledIndices.get(i);
            DiceType type = types.get(idx);
            int value = values.get(idx);
            if (i > 0) sb.append(", ");
            sb.append(type.name()).append("(").append(value).append(")");
        }
        int total = 0;
        for (Integer value : values)
        {
            total += value;
        }
        sb.append(" | New total: ").append(total);
        CosmiconLogger.debug(sb.toString());
    }
}
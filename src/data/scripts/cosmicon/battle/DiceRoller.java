package data.scripts.cosmicon.battle;

import data.scripts.cosmicon.character.PassiveEventSystem;
import data.scripts.cosmicon.util.CosmiconRandom;
import java.util.ArrayList;
import java.util.List;

public class DiceRoller {
    
    private final WeatherController weatherController;
    
    public DiceRoller(WeatherController weatherController) {
        this.weatherController = weatherController;
    }
    
    public void rollAll(BattleState state) {
        rollForParticipant(state, true);
        rollForParticipant(state, false);
    }
    
    private void rollForParticipant(BattleState state, boolean forPlayer) {
        CharacterCard card = state.getCard(forPlayer);
        boolean isAttacker = state.isAttacker(forPlayer);
        
        List<DiceType> types = card.getDicePool();
        List<Integer> values = new ArrayList<>();
        List<Boolean> selected = new ArrayList<>();
        
        boolean preventMinimum = weatherController != null && weatherController.shouldPreventMinimumRoll();
        boolean preventMax = weatherController != null && weatherController.shouldPreventMaxRoll(!isAttacker);
        
        for (DiceType type : types) {
            int minRoll = preventMinimum ? 2 : 1;
            int maxRoll = type.getMaxFace();
            if (preventMax) maxRoll = Math.max(1, maxRoll - 1);
            
            int value = CosmiconRandom.nextInt(maxRoll - minRoll + 1) + minRoll;
            values.add(value);
            selected.add(false);
        }
        
        state.setDiceValues(forPlayer, values);
        state.setDiceTypes(forPlayer, types);
        state.setDiceSelected(forPlayer, selected);
        
        state.notifyDiceRolled(forPlayer, types, values);
    }
    
    public void rerollSelected(BattleState state, boolean forPlayer) {
        List<Integer> values = state.getDiceValues(forPlayer);
        List<Boolean> selected = state.getDiceSelected(forPlayer);
        List<DiceType> types = state.getDiceTypes(forPlayer);
        List<Integer> rerolledIndices = new ArrayList<>();
        
        boolean isAttacker = state.isAttacker(forPlayer);
        boolean preventMinimum = weatherController != null && weatherController.shouldPreventMinimumRoll();
        boolean preventMax = weatherController != null && weatherController.shouldPreventMaxRoll(!isAttacker);
        
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                DiceType type = types.get(i);
                int minRoll = preventMinimum ? 2 : 1;
                int maxRoll = type.getMaxFace();
                if (preventMax) maxRoll = Math.max(1, maxRoll - 1);
                
                int value = CosmiconRandom.nextInt(maxRoll - minRoll + 1) + minRoll;
                values.set(i, value);
                rerolledIndices.add(i);
            }
        }
        
        state.setDiceValues(forPlayer, values);
        state.decrementRerolls(forPlayer);
        state.incrementRerollsUsed(forPlayer);
        
        PassiveEventSystem.onRerollCompleted(state, forPlayer);
        
        if (weatherController != null) {
            weatherController.applyRerollThornsEffect(state, forPlayer);
            weatherController.applyRerollGlidingEffect(state, forPlayer);
        }
        
        state.notifyDiceRerolled(forPlayer, values, rerolledIndices);
    }
}
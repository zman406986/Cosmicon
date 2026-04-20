package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class KafkaAIProfile implements CharacterAIProfile {

    @Override
    public String getCharacterId() {
        return "kafka";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.kafka.name");
    }

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return isAttacking;
    }

    @Override
    public boolean isAttackPassive() {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (!isAttacking || selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }
        
        PassiveResult result = PassiveEvaluator.evaluateForCharacter("kafka", selectedValues, isAttacking);
        int distinct = PassiveEvaluator.countDistinctValues(selectedValues);
        return PassiveEvaluator.toPassiveEvaluation(result, Strings.format("character.kafka.passive_desc", distinct));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (!isAttacking) return 0f;
        return PassiveEvaluator.countDistinctValues(selectedValues);
    }
}
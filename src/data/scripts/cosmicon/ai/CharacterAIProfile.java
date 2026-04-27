package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.battle.DiceType;
import java.util.List;

public interface CharacterAIProfile {

    String getCharacterId();

    String getCharacterName();

    boolean prefersHighValues(boolean isAttacking);

    default boolean shouldOptimizeForPassive(boolean isAttacking) {
        return false;
    }

    default float getRiskTolerance() {
        return 0.5f;
    }

    default int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 15 : 10;
    }

    PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking);

    float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking);

    default boolean isDefensePassive() {
        return false;
    }

    default boolean isAttackPassive() {
        return true;
    }
    
    default float getPrismaticDiceBonus(DiceType type, int faceValue, boolean isAttacking) {
        return 0f;
    }

    default boolean prefersPairs() {
        return false;
    }

    record PassiveEvaluation(boolean triggered, float triggerProbability, float bonusValue, String description)
    {

        public static PassiveEvaluation notTriggered()
        {
                return new PassiveEvaluation(false, 0f, 0f, "");
            }

            public static PassiveEvaluation triggered(float bonus, String desc)
            {
                return new PassiveEvaluation(true, 1f, bonus, desc);
            }

            public static PassiveEvaluation potential(float probability, float bonus, String desc)
            {
                return new PassiveEvaluation(false, probability, bonus, desc);
            }
        }
}
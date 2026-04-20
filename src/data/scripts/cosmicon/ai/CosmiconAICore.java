package data.scripts.cosmicon.ai;

import data.scripts.cosmicon.ai.profiles.CharacterProfileRegistry;
import data.scripts.cosmicon.ai.profiles.DefaultCharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.Strings;
import java.util.*;

public final class CosmiconAICore {

    private static final CharacterAIProfile DEFAULT_PROFILE = new DefaultCharacterAIProfile();

    private CosmiconAICore() {}

    public static CharacterAIProfile getProfile(String characterId) {
        return CharacterProfileRegistry.get(characterId);
    }

    public static AIDecision makeDecision(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredSelectCount,
            String characterId,
            boolean isAttacking,
            int rerollsAvailable,
            int targetSum) {
        
        CharacterAIProfile profile = getProfile(characterId);

        SelectionOptimizer.SelectionResult selection = SelectionOptimizer.optimalSelection(
            diceValues, diceTypes, requiredSelectCount, isAttacking, profile);

        Set<Integer> rerollIndices = Set.of();
        if (rerollsAvailable > 0) {
            int effectiveTarget = targetSum > 0 ? targetSum : profile.getTargetThreshold(isAttacking);
            rerollIndices = RerollOptimizer.optimalRerolls(
                diceValues, diceTypes, requiredSelectCount, rerollsAvailable, effectiveTarget, isAttacking);
        }

        return new AIDecision(selection, rerollIndices, profile);
    }

    public static Set<Integer> recommendRerolls(
            List<Integer> diceValues,
            List<DiceType> diceTypes,
            int requiredCount,
            int rerollsAvailable,
            String characterId,
            boolean isAttacking,
            int targetSum) {
        
        return RerollOptimizer.optimalRerolls(diceValues, diceTypes, requiredCount, rerollsAvailable, targetSum, isAttacking);
    }

    public static float predictDamage(
            int attackValue,
            int defenseValue,
            CharacterAIProfile attackerProfile) {
        
        float baseDamage = Math.max(0, attackValue - defenseValue);
        
        if (attackerProfile != null) {
            float attackBonus = attackerProfile.getPassiveBonusValue(List.of(attackValue), true);
            baseDamage += attackBonus;
        }
        
        return baseDamage;
    }

    public static float probabilityToReachTarget(
            List<DiceType> diceTypes,
            int selectCount,
            int target) {
        return DiceProbabilityCalculator.probabilitySumAtLeast(diceTypes, selectCount, target);
    }

    public static float evaluateSituation(
            int myHp,
            int opponentHp,
            int currentTurn,
            boolean iAmAttacking,
            String myCharacterId,
            String opponentCharacterId) {
        
        CharacterAIProfile myProfile = getProfile(myCharacterId);
        CharacterAIProfile opponentProfile = getProfile(opponentCharacterId);

        float hpAdvantage = (myHp - opponentHp) / 30f;
        float turnAdvantage = currentTurn > 4 ? 0.2f : 0f;
        
        float positionAdvantage = iAmAttacking && myProfile.isAttackPassive() ? 0.3f : 0f;
        if (!iAmAttacking && myProfile.isDefensePassive()) {
            positionAdvantage = 0.25f;
        }

        return hpAdvantage + turnAdvantage + positionAdvantage + myProfile.getRiskTolerance() * 0.1f;
    }

    public static final class AIDecision {
        public final SelectionOptimizer.SelectionResult selection;
        public final Set<Integer> rerollIndices;
        public final CharacterAIProfile profile;

        private AIDecision(SelectionOptimizer.SelectionResult selection, Set<Integer> rerollIndices, CharacterAIProfile profile) {
            this.selection = selection;
            this.rerollIndices = rerollIndices;
            this.profile = profile;
        }

        public List<Integer> getSelectedIndicesList() {
            return selection.getSelectedIndicesList();
        }

        public int getSelectedSum() {
            return selection.sumValue;
        }

        public boolean isPassiveTriggered() {
            return selection.passiveTriggered;
        }

        public float getPassiveBonus() {
            return selection.passiveBonus;
        }

        public float getTotalValue() {
            return selection.totalScore;
        }

        public boolean shouldReroll() {
            return !rerollIndices.isEmpty();
        }

        public int getRerollCount() {
            return rerollIndices.size();
        }

        public List<Integer> getRerollIndicesList() {
            return new ArrayList<>(rerollIndices);
        }

        public String getDecisionSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(Strings.format("ai.decision_selected", getSelectedSum()));
            if (isPassiveTriggered()) {
                sb.append(Strings.format("ai.decision_passive", (int) getPassiveBonus()));
            }
            if (shouldReroll()) {
                sb.append(Strings.format("ai.decision_reroll", getRerollCount()));
            }
            return sb.toString();
        }
    }
}
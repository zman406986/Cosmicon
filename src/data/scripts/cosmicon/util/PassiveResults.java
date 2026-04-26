package data.scripts.cosmicon.util;

import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import java.util.ArrayList;
import java.util.List;

public class PassiveResults {

    public static class PassiveResult {
        private int attackBonus;
        private int defenseBonus;
        private boolean perforation;
        private final List<GrantedEffect> grantedEffects;
        private int healAmount;
        private int instantDamageToOpponent;
        private int toughnessToRemove;
        private boolean triggerToughnessInstantDamage;
        private int pendingDefLevelBoost;

        public PassiveResult() {
            this.attackBonus = 0;
            this.defenseBonus = 0;
            this.perforation = false;
            this.grantedEffects = new ArrayList<>();
            this.healAmount = 0;
            this.instantDamageToOpponent = 0;
            this.toughnessToRemove = 0;
            this.triggerToughnessInstantDamage = false;
            this.pendingDefLevelBoost = 0;
        }

        public void addAttackBonus(int bonus) {
            this.attackBonus += bonus;
        }

        public void setPerforation(boolean perforation) {
            this.perforation = perforation;
        }

        public void addGrantedEffect(StatusEffect effect, int layers) {
            grantedEffects.add(new GrantedEffect(effect, layers));
        }

        public void setHealAmount(int heal) {
            this.healAmount = heal;
        }

        public void setToughnessTrigger(int damage, int remove) {
            this.triggerToughnessInstantDamage = true;
            this.instantDamageToOpponent = damage;
            this.toughnessToRemove = remove;
        }

        public int getAttackBonus() {
            return attackBonus;
        }

        public int getDefenseBonus() {
            return defenseBonus;
        }

        public boolean hasPerforation() {
            return perforation;
        }

        public List<GrantedEffect> getGrantedEffects() {
            return new ArrayList<>(grantedEffects);
        }

        public int getHealAmount() {
            return healAmount;
        }

        public int getInstantDamageToOpponent() {
            return instantDamageToOpponent;
        }

        public int getToughnessToRemove() {
            return toughnessToRemove;
        }

        public boolean shouldTriggerToughnessInstantDamage() {
            return triggerToughnessInstantDamage;
        }

        public void setPendingDefLevelBoost(int boost) {
            this.pendingDefLevelBoost = boost;
        }

        public int getPendingDefLevelBoost() {
            return pendingDefLevelBoost;
        }

        public boolean isEmpty() {
            return attackBonus == 0 && defenseBonus == 0 && !perforation && grantedEffects.isEmpty() 
                && healAmount == 0 && instantDamageToOpponent == 0 && pendingDefLevelBoost == 0;
        }
    }

    public record GrantedEffect(StatusEffect effect, int layers) {}

    public static class PostDamageResult {
        private int atkLevelIncrease;
        private int defLevelIncrease;
        private int instantDamageToAttacker;
        private String description;

        public PostDamageResult() {
            this.atkLevelIncrease = 0;
            this.defLevelIncrease = 0;
            this.instantDamageToAttacker = 0;
            this.description = "";
        }

        public void addAtkLevelIncrease(int increase) {
            this.atkLevelIncrease += increase;
        }

        public void addDefLevelIncrease(int increase) {
            this.defLevelIncrease += increase;
        }

        public void setInstantDamageToAttacker(int damage) {
            this.instantDamageToAttacker = damage;
        }

        public void setDescription(String desc) {
            this.description = desc;
        }

        public int getAtkLevelIncrease() {
            return atkLevelIncrease;
        }

        public int getDefLevelIncrease() {
            return defLevelIncrease;
        }

        public int getInstantDamageToAttacker() {
            return instantDamageToAttacker;
        }

        public String getDescription() {
            return description;
        }

        public boolean hasEffects() {
            return atkLevelIncrease > 0 || defLevelIncrease > 0 || instantDamageToAttacker > 0;
        }
    }

    public static class EndOfTurnPassiveResult {
        private int prismaticUseBonus;
        private boolean grantArise;
        private final int atkLevelBoost;
        
        public EndOfTurnPassiveResult() {
            this.prismaticUseBonus = 0;
            this.grantArise = false;
            this.atkLevelBoost = 0;
        }
        
        public void addPrismaticUseBonus(int bonus) {
            this.prismaticUseBonus += bonus;
        }
        
        public void setGrantArise(boolean grant) {
            this.grantArise = grant;
        }
        
        public int getPrismaticUseBonus() {
            return prismaticUseBonus;
        }
        
        public boolean shouldGrantArise() {
            return grantArise;
        }
        
        public int getAtkLevelBoost() {
            return atkLevelBoost;
        }
        
        public boolean hasEffects() {
            return prismaticUseBonus > 0 || grantArise || atkLevelBoost > 0;
        }
    }

    private PassiveResults() {}
}
package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;

public class TheHertaAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.THE_HERTA;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.the_herta.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return false;
    }

    @Override
    public float getPrismaticDiceBonus(DiceType type, int faceValue, boolean isAttacking) {
        float bonus = 0f;
        if (type == DiceType.PRISMATIC) {
            bonus += 5f;
            if (faceValue >= 8) {
                bonus += 3f;
            }
        }
        return bonus;
    }
}

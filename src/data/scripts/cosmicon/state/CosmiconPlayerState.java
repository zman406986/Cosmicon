package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.util.CharacterIds;

import java.util.List;
import java.util.Map;

public class CosmiconPlayerState {

    private static final String KEY_SELECTED_CHARACTER = "$cos_selected_character";
    private static final String KEY_EQUIPPED_PRISMATIC = "$cos_equipped_prismatic";
    private static final String KEY_EQUIPPED_PRISMATIC_TRUE = "$cos_equipped_prismatic_true";
    private static final String KEY_BONUS_SELECTION_PREFIX = "$cos_bonus_";
    private static final String KEY_CREDIT_BONUS_ACTIVE = "$cos_credit_bonus_active";

    private static MemoryAPI getMemory() {
        return Global.getSector().getMemory();
    }

    public static void saveCharacter(String charId) {
        if (charId == null || charId.isEmpty()) return;
        getMemory().set(KEY_SELECTED_CHARACTER, charId);
    }

    public static String loadCharacter() {
        return getMemory().getString(KEY_SELECTED_CHARACTER);
    }

    public static void savePrismaticDice(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            getMemory().unset(KEY_EQUIPPED_PRISMATIC);
            return;
        }
        getMemory().set(KEY_EQUIPPED_PRISMATIC, diceId);
    }

    public static String loadPrismaticDice() {
        return getMemory().getString(KEY_EQUIPPED_PRISMATIC);
    }

    public static void savePrismaticDiceTrueVersion(boolean useTrue) {
        getMemory().set(KEY_EQUIPPED_PRISMATIC_TRUE, useTrue);
    }

    public static boolean loadPrismaticDiceTrueVersion() {
        return getMemory().getBoolean(KEY_EQUIPPED_PRISMATIC_TRUE);
    }

    public static void saveBonusSelection(String charId, BonusState bonus) {
        if (charId == null || charId.isEmpty()) return;
        getMemory().set(KEY_BONUS_SELECTION_PREFIX + charId, bonus.name());
    }

    public static BonusState loadBonusSelection(String charId) {
        if (charId == null || charId.isEmpty()) return BonusState.NONE;
        String val = getMemory().getString(KEY_BONUS_SELECTION_PREFIX + charId);
        if (val == null || val.isEmpty()) return BonusState.NONE;
        try {
            return BonusState.valueOf(val);
        } catch (IllegalArgumentException e) {
            return BonusState.NONE;
        }
    }

    public static void setCreditBonusActive(boolean active) {
        getMemory().set(KEY_CREDIT_BONUS_ACTIVE, active);
    }

    public static boolean isCreditBonusActive() {
        return getMemory().getBoolean(KEY_CREDIT_BONUS_ACTIVE);
    }

    public static boolean isBasicCharacter() {
        String charId = loadCharacter();
        return charId != null && CharacterIds.EASY_MODE_CHARACTERS.contains(charId);
    }

    public static int getCreditBonusPercent() {
        boolean basic = isBasicCharacter();
        boolean noBonus = isCreditBonusActive();
        if (basic && noBonus) return 100;
        if (basic || noBonus) return 50;
        return 0;
    }

    public static String getDefaultPrismaticForCharacter(String charId) {
        if (charId == null) return null;
        
        CharacterCard card = CharacterRegistry.getCharacterById(charId);
        if (card == null) return null;
        
        Map<String, Integer> prismaticDice = card.getPrismaticDiceIds();
        if (prismaticDice.isEmpty()) return null;
        
        return prismaticDice.keySet().iterator().next();
    }

    public static CharacterCard getConfiguredPlayerCard() {
        String charId = loadCharacter();
        
        if (charId == null || charId.isEmpty()) {
            List<CharacterCard> allCards = CharacterRegistry.getAllCards();
            if (allCards.isEmpty()) return null;
            charId = allCards.get(0).getId();
        }
        
        return CharacterRegistry.getCharacterById(charId);
    }


}
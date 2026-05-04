package data.scripts.cosmicon.state;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;

import java.util.Map;

public class CosmiconPlayerState {

    private static final String KEY_SELECTED_CHARACTER = "$cos_selected_character";
    private static final String KEY_EQUIPPED_PRISMATIC = "$cos_equipped_prismatic";
    private static final String KEY_EQUIPPED_PRISMATIC_TRUE = "$cos_equipped_prismatic_true";

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
            java.util.List<CharacterCard> allCards = CharacterRegistry.getAllCards();
            if (allCards.isEmpty()) return null;
            charId = allCards.get(0).getId();
        }
        
        return CharacterRegistry.getCharacterById(charId);
    }


}
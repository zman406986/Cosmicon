package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;

public class DefaultAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return "default";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.default.name");
    }
}

package data.scripts.cosmicon.battle;

import com.fs.starfarer.api.Global;
import data.scripts.CosmiconConfig;
import data.scripts.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CharacterRegistry {

    private static final Random random = new Random();
    private static final String CARDS_PATH = "data/config/cards.json";
    private static List<CharacterCard> threeStarCards = null;
    private static List<CharacterCard> opponentOnlyCards = null;

    static { 
        threeStarCards = new ArrayList<>();
        opponentOnlyCards = new ArrayList<>();
    }

    public static void loadCards() {
        try {
            JSONObject cardsJson = Global.getSettings().loadJSON(CARDS_PATH, CosmiconConfig.MOD_ID);
            
            threeStarCards = parseCardArray(cardsJson.getJSONArray("threeStar"));
            opponentOnlyCards = parseCardArray(cardsJson.optJSONArray("opponentOnly"));
            if (opponentOnlyCards == null) opponentOnlyCards = new ArrayList<>();
            
            Global.getLogger(CharacterRegistry.class).info(
                "Loaded " + threeStarCards.size() + " threeStar cards, " + 
                opponentOnlyCards.size() + " opponentOnly cards from " + CARDS_PATH);
        } catch (IOException | JSONException e) {
            Global.getLogger(CharacterRegistry.class).error("Error loading cards from " + CARDS_PATH, e);
            threeStarCards = new ArrayList<>();
            opponentOnlyCards = new ArrayList<>();
        }
    }

    private static List<CharacterCard> parseCardArray(JSONArray arr) throws JSONException {
        List<CharacterCard> cards = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            cards.add(parseCard(obj));
        }
        return cards;
    }

    private static CharacterCard parseCard(JSONObject obj) throws JSONException {
        String jsonName = obj.getString("name");
        String id = generateId(jsonName);
        int hp = obj.getInt("hp");
        int atkLevel = obj.getInt("atkLevel");
        int defLevel = obj.getInt("defLevel");
        
        List<DiceType> dicePool = parseDicePool(obj.getJSONArray("dice"));
        
        String name = getLocalizedString(id, "name", jsonName);
        String passive = getLocalizedString(id, "passive", obj.getString("passive"));
        
        return new CharacterCard(id, name, hp, atkLevel, defLevel, dicePool, passive);
    }

    private static String getLocalizedString(String id, String field, String fallback) {
        try {
            return Strings.get("character." + id + "." + field);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String generateId(String name) {
        return name.toLowerCase()
            .replace(" ", "_")
            .replace("•", "_")
            .replace("\"", "")
            .replaceAll("_+", "_");
    }

    private static List<DiceType> parseDicePool(JSONArray diceArr) throws JSONException {
        List<DiceType> pool = new ArrayList<>();
        for (int i = 0; i < diceArr.length(); i++) {
            String entry = diceArr.getString(i);
            pool.addAll(parseDiceEntry(entry));
        }
        return pool;
    }

    private static List<DiceType> parseDiceEntry(String entry) {
        List<DiceType> result = new ArrayList<>();
        String[] parts = entry.split(":");
        if (parts.length != 2) return result;
        
        String typeStr = parts[0].trim();
        int count = Integer.parseInt(parts[1].trim());
        
        DiceType type = mapDiceType(typeStr);
        if (type != null) {
            for (int i = 0; i < count; i++) {
                result.add(type);
            }
        }
        return result;
    }

    private static DiceType mapDiceType(String typeStr) {
        return switch (typeStr) {
            case "prismatic" -> DiceType.PRISMATIC_D12;
            case "orange_d8" -> DiceType.ORANGE_D8;
            case "purple_d6" -> DiceType.PURPLE_D6;
            case "blue_d4" -> DiceType.BLUE_D4;
            default -> null;
        };
    }

    public static CharacterCard getRandomCharacter() {
        if (threeStarCards.isEmpty()) return null;
        return threeStarCards.get(random.nextInt(threeStarCards.size()));
    }

    public static CharacterCard getRandomOpponent() {
        List<CharacterCard> allOpponents = new ArrayList<>(opponentOnlyCards);
        allOpponents.addAll(threeStarCards);
        if (allOpponents.isEmpty()) return null;
        return allOpponents.get(random.nextInt(allOpponents.size()));
    }

    public static CharacterCard getCharacterById(String id) {
        for (CharacterCard card : threeStarCards) {
            if (card.getId().equals(id)) return card;
        }
        for (CharacterCard card : opponentOnlyCards) {
            if (card.getId().equals(id)) return card;
        }
        return getRandomCharacter();
    }

    public static List<CharacterCard> getAllCards() {
        List<CharacterCard> all = new ArrayList<>(threeStarCards);
        all.addAll(opponentOnlyCards);
        return all;
    }

    public static List<CharacterCard> getThreeStarCards() {
        return new ArrayList<>(threeStarCards);
    }

    public static List<CharacterCard> getOpponentOnlyCards() {
        return new ArrayList<>(opponentOnlyCards);
    }
}
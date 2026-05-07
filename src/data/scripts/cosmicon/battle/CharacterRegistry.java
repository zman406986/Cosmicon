package data.scripts.cosmicon.battle;

import com.fs.starfarer.api.Global;
import data.scripts.CosmiconConfig;
import data.scripts.Strings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ThreadLocalRandom;

public class CharacterRegistry {

    private static final String CARDS_PATH = "data/config/cards.json";
    private static List<CharacterCard> threeStarCards;
    private static Map<String, Integer> cardIndex;
    private static List<CharacterCard> eligibleOpponents;

    static { 
        threeStarCards = new ArrayList<>();
        cardIndex = new HashMap<>();
        eligibleOpponents = new ArrayList<>();
    }

    public static void loadCards() {
        try {
            JSONObject cardsJson = Global.getSettings().loadJSON(CARDS_PATH, CosmiconConfig.MOD_ID);
            
            threeStarCards = parseCardArray(cardsJson.getJSONArray("threeStar"));
            
            cardIndex = new HashMap<>();
            for (int i = 0; i < threeStarCards.size(); i++) {
                cardIndex.put(threeStarCards.get(i).getId(), i);
            }

            eligibleOpponents = new ArrayList<>();
            for (CharacterCard card : threeStarCards) {
                if (!"trashcan".equals(card.getId())) {
                    eligibleOpponents.add(card);
                }
            }
            
            Global.getLogger(CharacterRegistry.class).info(
                "Loaded " + threeStarCards.size() + " threeStar cards from " + CARDS_PATH);
        } catch (IOException | JSONException e) {
            Global.getLogger(CharacterRegistry.class).error("Error loading cards from " + CARDS_PATH, e);
            threeStarCards = new ArrayList<>();
            cardIndex = new HashMap<>();
            eligibleOpponents = new ArrayList<>();
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
        
        Map<String, Integer> prismaticDice = parsePrismaticDice(obj);
        
        return new CharacterCard(id, name, hp, atkLevel, defLevel, dicePool, passive, prismaticDice);
    }

    private static String getLocalizedString(String id, String field, String fallback) {
        try {
            return Strings.get("character." + id + "." + field);
        } catch (MissingResourceException e) {
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
            case "orange_d8" -> DiceType.ORANGE_D8;
            case "purple_d6" -> DiceType.PURPLE_D6;
            case "blue_d4" -> DiceType.BLUE_D4;
            default -> null;
        };
    }

    private static Map<String, Integer> parsePrismaticDice(JSONObject obj) {
        Map<String, Integer> result = new HashMap<>();
        
        if (!obj.has("prismaticDice")) {
            return result;
        }
        
        try {
            JSONArray arr = obj.getJSONArray("prismaticDice");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject entry = arr.getJSONObject(i);
                String displayName = entry.getString("type");
                int count = entry.getInt("count");
                String registryId = mapPrismaticDiceName(displayName);
                if (registryId != null) {
                    result.put(registryId, count);
                }
            }
        } catch (JSONException e) {
            Global.getLogger(CharacterRegistry.class).warn("Error parsing prismaticDice", e);
        }
        
        return result;
    }

    private static String mapPrismaticDiceName(String displayName) {
        return switch (displayName) {
            case "Doctor's Advice" -> "doctors_advice";
            case "Repeater" -> "repeater";
            case "Sorcerer" -> "sorcerer";
            case "Berserker" -> "berserker";
            case "Prime Number" -> "prime_number";
            case "Magic Bullet" -> "magic_bullet";
            case "Destiny" -> "destiny";
            case "Gambler" -> "gambler";
            case "Astral Shield" -> "astral_shield";
            case "Oath" -> "oath";
            default -> null;
        };
    }

    public static CharacterCard getRandomCharacter() {
        if (threeStarCards.isEmpty()) return null;
        return threeStarCards.get(ThreadLocalRandom.current().nextInt(threeStarCards.size())).copy();
    }

    public static CharacterCard getRandomOpponent() {
        if (eligibleOpponents.isEmpty()) return null;
        return eligibleOpponents.get(ThreadLocalRandom.current().nextInt(eligibleOpponents.size())).copy();
    }

    public static CharacterCard getCharacterById(String id) {
        Integer index = cardIndex.get(id);
        if (index != null) return threeStarCards.get(index).copy();
        return getRandomCharacter();
    }

    public static List<CharacterCard> getAllCards() {
        List<CharacterCard> copies = new ArrayList<>();
        for (CharacterCard card : threeStarCards) {
            copies.add(card.copy());
        }
        return copies;
    }

}
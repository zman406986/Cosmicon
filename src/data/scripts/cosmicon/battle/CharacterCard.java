package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CharacterCard {

    private final String id;
    private final String name;
    private final int maxHp;
    private int atkLevel;
    private int defLevel;
    private final int defaultAtkLevel;
    private final int defaultDefLevel;
    private final List<DiceType> dicePool;
    private final List<DiceType> dicePoolView;
    private final String passiveDescription;
    private final Map<String, Integer> prismaticDiceIds;
    private final Map<String, Integer> prismaticDiceIdsView;
    private final boolean useTruePrismatic;

    public CharacterCard(String id, String name, int maxHp, int atkLevel, int defLevel,
                         List<DiceType> dicePool, String passiveDescription,
                         Map<String, Integer> prismaticDiceIds) {
        this(id, name, maxHp, atkLevel, defLevel, dicePool, passiveDescription, prismaticDiceIds, false);
    }

    public CharacterCard(String id, String name, int maxHp, int atkLevel, int defLevel,
                         List<DiceType> dicePool, String passiveDescription,
                         Map<String, Integer> prismaticDiceIds, boolean useTruePrismatic) {
        this(id, name, maxHp, atkLevel, defLevel, atkLevel, defLevel, dicePool, passiveDescription, prismaticDiceIds, useTruePrismatic);
    }

    public CharacterCard(String id, String name, int maxHp, int atkLevel, int defLevel,
                         int defaultAtkLevel, int defaultDefLevel,
                         List<DiceType> dicePool, String passiveDescription,
                         Map<String, Integer> prismaticDiceIds, boolean useTruePrismatic) {
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.atkLevel = atkLevel;
        this.defLevel = defLevel;
        this.defaultAtkLevel = defaultAtkLevel;
        this.defaultDefLevel = defaultDefLevel;
        this.dicePool = new ArrayList<>(dicePool);
        this.dicePoolView = Collections.unmodifiableList(this.dicePool);
        this.passiveDescription = passiveDescription;
        this.prismaticDiceIds = prismaticDiceIds != null ? prismaticDiceIds : Collections.emptyMap();
        this.prismaticDiceIdsView = Collections.unmodifiableMap(this.prismaticDiceIds);
        this.useTruePrismatic = useTruePrismatic;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAtkLevel() {
        return atkLevel;
    }

    public void setAtkLevel(int level) {
        this.atkLevel = Math.max(1, Math.min(level, 5));
    }

    public int getDefLevel() {
        return defLevel;
    }

    public void setDefLevel(int level) {
        this.defLevel = Math.max(1, Math.min(level, 5));
    }

    public int getDefaultAtkLevel() {
        return defaultAtkLevel;
    }

    public int getDefaultDefLevel() {
        return defaultDefLevel;
    }

    public List<DiceType> getDicePool() {
        return dicePoolView;
    }

    public String getPassiveDescription() {
        return passiveDescription;
    }

    public Map<String, Integer> getPrismaticDiceIds() {
        return prismaticDiceIdsView;
    }

    public boolean isUseTruePrismatic() {
        return useTruePrismatic;
    }

    public CharacterCard withPrismaticDice(String diceId, int uses, boolean useTruePrismatic) {
        return new CharacterCard(id, name, maxHp, atkLevel, defLevel, defaultAtkLevel, defaultDefLevel, dicePool, passiveDescription, Map.of(diceId, uses), useTruePrismatic);
    }

    public CharacterCard withPrismaticDice(String diceId, int uses) {
        return withPrismaticDice(diceId, uses, useTruePrismatic);
    }

    public CharacterCard withMaxHp(int newMaxHp) {
        return new CharacterCard(id, name, newMaxHp, atkLevel, defLevel, defaultAtkLevel, defaultDefLevel, dicePool, passiveDescription, prismaticDiceIds, useTruePrismatic);
    }

    public CharacterCard withAtkLevel(int atkLevel) {
        CharacterCard copy = this.copy();
        copy.setAtkLevel(atkLevel);
        return copy;
    }

    public CharacterCard withDefLevel(int defLevel) {
        CharacterCard copy = this.copy();
        copy.setDefLevel(defLevel);
        return copy;
    }

    public CharacterCard copy() {
        return new CharacterCard(id, name, maxHp, atkLevel, defLevel, defaultAtkLevel, defaultDefLevel, dicePool, passiveDescription, prismaticDiceIds, useTruePrismatic);
    }
}
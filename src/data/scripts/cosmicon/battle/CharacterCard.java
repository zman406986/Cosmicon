package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

public class CharacterCard {

    private final String id;
    private final String name;
    private final int maxHp;
    private int atkLevel;
    private int defLevel;
    private final List<DiceType> dicePool;
    private final String passiveDescription;

    public CharacterCard(String id, String name, int maxHp, int atkLevel, int defLevel, 
                         List<DiceType> dicePool, String passiveDescription) {
        this.id = id;
        this.name = name;
        this.maxHp = maxHp;
        this.atkLevel = atkLevel;
        this.defLevel = defLevel;
        this.dicePool = new ArrayList<>(dicePool);
        this.passiveDescription = passiveDescription;
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

    public List<DiceType> getDicePool() {
        return new ArrayList<>(dicePool);
    }

    public String getPassiveDescription() {
        return passiveDescription;
    }
}
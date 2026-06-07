package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;

public class PrismaticState {

    private int playerPrismaticTriggerCount;
    private int opponentPrismaticTriggerCount;
    private final Map<Integer, PrismaticDiceInstance> playerPrismaticDiceByIndex;
    private final Map<Integer, PrismaticDiceInstance> opponentPrismaticDiceByIndex;

    public PrismaticState() {
        this.playerPrismaticTriggerCount = 0;
        this.opponentPrismaticTriggerCount = 0;
        this.playerPrismaticDiceByIndex = new HashMap<>();
        this.opponentPrismaticDiceByIndex = new HashMap<>();
    }

    public int getPrismaticTriggerCount(boolean forPlayer) {
        return forPlayer ? playerPrismaticTriggerCount : opponentPrismaticTriggerCount;
    }

    public void incrementPrismaticTriggerCount(boolean forPlayer) {
        if (forPlayer) {
            playerPrismaticTriggerCount++;
        } else {
            opponentPrismaticTriggerCount++;
        }
    }

    private Map<Integer, PrismaticDiceInstance> diceMap(boolean forPlayer) {
        return forPlayer ? playerPrismaticDiceByIndex : opponentPrismaticDiceByIndex;
    }

    public void addPrismaticDiceToPool(PrismaticDiceInstance dice, boolean forPlayer, DiceState diceState) {
        List<DiceType> types = diceState.getDiceTypesDirect(forPlayer);
        List<Integer> values = diceState.getDiceValuesDirect(forPlayer);
        List<Boolean> selected = diceState.getDiceSelectedDirect(forPlayer);
        Map<Integer, PrismaticDiceInstance> map = diceMap(forPlayer);

        types.add(DiceType.PRISMATIC);
        values.add(dice.rolledFace);
        selected.add(false);
        map.put(types.size() - 1, dice);
    }

    public PrismaticDiceInstance getPrismaticDiceAt(int index, boolean forPlayer) {
        return diceMap(forPlayer).get(index);
    }

    public boolean isPrismaticDiceAt(int index, boolean forPlayer) {
        return diceMap(forPlayer).containsKey(index);
    }

    public void updatePrismaticDiceAt(int index, PrismaticDiceInstance newInstance, boolean forPlayer) {
        Map<Integer, PrismaticDiceInstance> map = diceMap(forPlayer);
        if (map.containsKey(index)) {
            map.put(index, newInstance);
        }
    }

    public Map<Integer, PrismaticDiceInstance> getPrismaticDiceMap(boolean forPlayer) {
        return diceMap(forPlayer);
    }

    public List<PrismaticDiceInstance> getSelectedPrismaticDice(boolean forPlayer, DiceState diceState) {
        List<PrismaticDiceInstance> result = new ArrayList<>();
        Map<Integer, PrismaticDiceInstance> map = diceMap(forPlayer);
        List<Boolean> selected = diceState.getDiceSelected(forPlayer);

        for (Map.Entry<Integer, PrismaticDiceInstance> entry : map.entrySet()) {
            int idx = entry.getKey();
            if (idx < selected.size() && selected.get(idx)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public void clearDiceMaps() {
        playerPrismaticDiceByIndex.clear();
        opponentPrismaticDiceByIndex.clear();
    }

    public void cleanup() {
        playerPrismaticTriggerCount = 0;
        opponentPrismaticTriggerCount = 0;
        playerPrismaticDiceByIndex.clear();
        opponentPrismaticDiceByIndex.clear();
    }
}

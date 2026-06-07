package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiceState {

    private List<DiceType> playerDiceTypes;
    private List<Integer> playerDiceValues;
    private List<Boolean> playerDiceSelected;

    private List<DiceType> opponentDiceTypes;
    private List<Integer> opponentDiceValues;
    private List<Boolean> opponentDiceSelected;

    private List<DiceType> playerUpgradedDicePool;
    private List<DiceType> opponentUpgradedDicePool;

    private DicePoolCounts playerUpgradedDicePoolCounts;
    private DicePoolCounts opponentUpgradedDicePoolCounts;

    private DicePoolCounts playerDicePoolCounts;
    private DicePoolCounts opponentDicePoolCounts;

    public DiceState() {
    }

    public void initDicePools(CharacterCard playerCard, CharacterCard opponentCard) {
        this.playerDicePoolCounts = DicePoolCounts.fromPool(playerCard.getDicePool());
        this.opponentDicePoolCounts = DicePoolCounts.fromPool(opponentCard.getDicePool());
        clearUpgradedDicePool(true);
        clearUpgradedDicePool(false);
    }

    public List<DiceType> getPlayerDiceTypes() {
        return playerDiceTypes;
    }

    public List<Integer> getPlayerDiceValues() {
        return playerDiceValues;
    }

    public List<Boolean> getPlayerDiceSelected() {
        return playerDiceSelected;
    }

    public List<DiceType> getOpponentDiceTypes() {
        return opponentDiceTypes;
    }

    public List<Integer> getOpponentDiceValues() {
        return opponentDiceValues;
    }

    public List<Boolean> getOpponentDiceSelected() {
        return opponentDiceSelected;
    }

    public List<Integer> getDiceValues(boolean forPlayer) {
        return forPlayer ? playerDiceValues : opponentDiceValues;
    }

    public List<DiceType> getDiceTypes(boolean forPlayer) {
        return forPlayer ? playerDiceTypes : opponentDiceTypes;
    }

    public List<Boolean> getDiceSelected(boolean forPlayer) {
        return forPlayer ? playerDiceSelected : opponentDiceSelected;
    }

    List<Integer> getDiceValuesDirect(boolean forPlayer) {
        return forPlayer ? playerDiceValues : opponentDiceValues;
    }

    List<DiceType> getDiceTypesDirect(boolean forPlayer) {
        return forPlayer ? playerDiceTypes : opponentDiceTypes;
    }

    List<Boolean> getDiceSelectedDirect(boolean forPlayer) {
        return forPlayer ? playerDiceSelected : opponentDiceSelected;
    }

    public void setDiceValues(boolean forPlayer, List<Integer> values) {
        if (forPlayer) playerDiceValues = values;
        else opponentDiceValues = values;
    }

    public void setDiceTypes(boolean forPlayer, List<DiceType> types) {
        if (forPlayer) playerDiceTypes = types;
        else opponentDiceTypes = types;
    }

    public void setDiceSelected(boolean forPlayer, List<Boolean> selected) {
        if (forPlayer) playerDiceSelected = selected;
        else opponentDiceSelected = selected;
    }

    public DicePoolCounts getPlayerDicePoolCounts() {
        if (playerUpgradedDicePoolCounts != null) return playerUpgradedDicePoolCounts;
        return playerDicePoolCounts;
    }

    public DicePoolCounts getOpponentDicePoolCounts() {
        if (opponentUpgradedDicePoolCounts != null) return opponentUpgradedDicePoolCounts;
        return opponentDicePoolCounts;
    }

    public List<DiceType> getUpgradedDicePool(boolean forPlayer) {
        return forPlayer ? playerUpgradedDicePool : opponentUpgradedDicePool;
    }

    public void setUpgradedDicePool(boolean forPlayer, List<DiceType> pool) {
        if (forPlayer) {
            playerUpgradedDicePool = pool;
            playerUpgradedDicePoolCounts = pool != null ? DicePoolCounts.fromPool(pool) : null;
        } else {
            opponentUpgradedDicePool = pool;
            opponentUpgradedDicePoolCounts = pool != null ? DicePoolCounts.fromPool(pool) : null;
        }
    }

    public void clearUpgradedDicePool(boolean forPlayer) {
        if (forPlayer) { playerUpgradedDicePool = null; playerUpgradedDicePoolCounts = null; }
        else { opponentUpgradedDicePool = null; opponentUpgradedDicePoolCounts = null; }
    }

    public void clearDiceSelection(boolean forPlayer) {
        List<Boolean> selected = getDiceSelectedDirect(forPlayer);
        if (selected != null) {
            Collections.fill(selected, false);
        }
    }

    public int countSelectedDice(boolean forPlayer) {
        return countSelected(getDiceSelected(forPlayer));
    }

    public int countSelected(List<Boolean> selected) {
        if (selected == null) return 0;
        int count = 0;
        for (int i = 0, size = selected.size(); i < size; i++) {
            if (selected.get(i)) count++;
        }
        return count;
    }

    public int calculateSelectedSum(List<Integer> values, List<Boolean> selected) {
        if (values == null || selected == null) return 0;
        int sum = 0;
        for (int i = 0; i < values.size(); i++) if (selected.get(i)) sum += values.get(i);
        return sum;
    }

    public List<Boolean> getPrismaticFlagsForDice(boolean isPlayer) {
        List<Boolean> flags = new ArrayList<>();
        List<DiceType> types = isPlayer ? playerDiceTypes : opponentDiceTypes;
        if (types == null) return flags;
        for (DiceType type : types) {
            flags.add(type == DiceType.PRISMATIC);
        }
        return flags;
    }

    public void cleanup() {
        playerDiceTypes = null;
        playerDiceValues = null;
        playerDiceSelected = null;
        opponentDiceTypes = null;
        opponentDiceValues = null;
        opponentDiceSelected = null;
        playerUpgradedDicePoolCounts = null;
        opponentUpgradedDicePoolCounts = null;
        clearUpgradedDicePool(true);
        clearUpgradedDicePool(false);
    }
}

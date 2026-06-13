package data.scripts.cosmicon.setup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.util.Misc;

import data.scripts.Strings;
import data.scripts.cosmicon.CosmiconSFX;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.DicePoolCounts;
import data.scripts.cosmicon.battle.BattleRenderingUtils;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import data.scripts.cosmicon.util.UnifiedCoord;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UIComponentFactory;
import data.scripts.cosmicon.state.BonusState;
import data.scripts.cosmicon.state.CosmiconPlayerState;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.util.CharacterIds;

public class CharacterSetupPanelUI extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final float PANEL_WIDTH = 1000f;
    private static final float PANEL_HEIGHT = 700f;
    private static final float CARD_WIDTH = BattleRenderingUtils.CARD_WIDTH;
    private static final float CARD_HEIGHT = BattleRenderingUtils.CARD_HEIGHT;
    private static final int COLS = 3;
    private static final float GAP_X = 10f;
    private static final float GAP_Y = 15f;
    private static final float EXTRA_SCROLL_PADDING = 10f;
    private static final float MARGIN = 15f;
    private static final float HEADER_HEIGHT = 40f;
    private static final float SELECTION_BAR_HEIGHT = 30f;

    private static final float GALLERY_WIDTH = 595f;
    private static final float SCROLLBAR_WIDTH = 14f;
    private static final float DICE_LIST_X = GALLERY_WIDTH + MARGIN + 10f;
    private static final float DICE_LIST_WIDTH = PANEL_WIDTH - DICE_LIST_X - MARGIN - SCROLLBAR_WIDTH;
    private static final float DICE_ENTRY_HEIGHT = 65f;
    private static final float DICE_LEFT_COL_WIDTH = 165f;

    private static final float BOTTOM_BAR_HEIGHT = 35f;
    private static final float CONTENT_START_Y = MARGIN + HEADER_HEIGHT + SELECTION_BAR_HEIGHT + 55f;
    private static final float BUTTON_WIDTH = 140f;
    private static final Color COLOR_SCROLLBAR_TRACK = new Color(35, 38, 48, 200);
    private static final Color COLOR_SCROLLBAR_THUMB = new Color(90, 100, 120, 180);

    private static final Color COLOR_BG_DARK = new Color(20, 20, 30);
    private static final Color COLOR_SELECTED = new Color(255, 215, 0);
    private static final Color COLOR_HEADER = new Color(100, 150, 255);
    private static final Color COLOR_TEXT = new Color(220, 220, 230);
    private static final Color COLOR_DICE_PANEL_BG = new Color(35, 40, 55, 220);
    private static final Color COLOR_SECTION_HEADER = new Color(100, 120, 150);
    private static final Color COLOR_DICE_ENTRY_BG = new Color(30, 35, 50, 180);
    private static final Color COLOR_DICE_ENTRY_SELECTED = new Color(50, 55, 75, 220);
    private static final Color COLOR_RADIO_SELECTED = new Color(255, 215, 0);
    private static final Color COLOR_RADIO_UNSELECTED = new Color(100, 100, 100);
    private static final Color COLOR_VERSION_LABEL = new Color(140, 140, 160);
    private static final Color COLOR_GALLERY_BG = new Color(35, 40, 50, 180);

    private static final int RADIO_SEGMENTS = 24;
    private static final float[] RADIO_COS = new float[RADIO_SEGMENTS];
    private static final float[] RADIO_SIN = new float[RADIO_SEGMENTS];
    static {
        for (int i = 0; i < RADIO_SEGMENTS; i++) {
            double rad = Math.toRadians(i * 15);
            RADIO_COS[i] = (float) Math.cos(rad);
            RADIO_SIN[i] = (float) Math.sin(rad);
        }
    }

    private static final float[] SCRATCH_COLOR = new float[4];

    private static float glX(float uiX) {
        UnifiedCoord.PanelContext c = UnifiedCoord.getCurrent();
        return c.panelX() + uiX;
    }

    private static float glSpriteY(float uiY, float spriteHeight) {
        UnifiedCoord.PanelContext c = UnifiedCoord.getCurrent();
        return c.panelY() + c.panelHeight() - uiY - spriteHeight;
    }

    private static final String ACTION_CONFIRM = "setup_confirm";
    private static final String ACTION_CANCEL = "setup_cancel";
    private static final String ACTION_BONUS_NONE = "bonus_none";
    private static final String ACTION_BONUS_HP = "bonus_hp";
    private static final String ACTION_BONUS_ATK = "bonus_atk";
    private static final String ACTION_BONUS_DEF = "bonus_def";

    private static final float BONUS_BUTTON_WIDTH = 80f;
    private static final float BONUS_BUTTON_HEIGHT = 24f;

    private int selectedIndex = -1;
    private String selectedPrismaticDiceId = null;
    private BonusState selectedBonus = BonusState.NONE;
    private final List<CharacterCard> characters;

    private CustomPanelAPI panel;
    private DialogCallbacks callbacks;

    private LabelAPI selectedNameLabel;
    private LabelAPI passiveLabel;
    private LabelAPI bonusDescLabel;
    private LabelAPI creditBonusLabel;
    private final Map<String, ButtonAPI> bonusButtons = new java.util.LinkedHashMap<>();

    private boolean wasMousePressed = false;

    private boolean needsLabelUpdate = true;
    private float prevScrollOffset = -1f;
    private float prevDiceScrollOffset = -1f;
    private int prevSelectedIndex = -2;
    private int prevDiceEntryIndex = -2;
    private boolean prevUseTrueVersion = false;
    private boolean canEquip = false;

    // Card gallery scrollbar
    private float scrollOffset = 0f;
    private float maxScroll = 0f;
    private boolean isDraggingScrollbar = false;
    private float dragStartMouseY = 0f;
    private float dragStartScrollOffset = 0f;
    private float scrollThumbUiY = 0f;
    private float scrollThumbHeight = 0f;

    // Dice list scrollbar
    private float diceScrollOffset = 0f;
    private float diceMaxScroll = 0f;
    private boolean isDraggingDiceScrollbar = false;
    private float diceDragStartMouseY = 0f;
    private float diceDragStartScrollOffset = 0f;
    private float diceScrollThumbUiY = 0f;
    private float diceScrollThumbHeight = 0f;

    // Dice list state
    private int selectedDiceEntryIndex = -1;
    private boolean selectedUseTrueVersion = false;

    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private final List<CardLabels> cardLabels = new ArrayList<>();
    private final List<DiceClickRegion> diceClickRegions = new ArrayList<>();
    private final List<VersionClickRegion> versionClickRegions = new ArrayList<>();
    private final List<DiceEntryLabels> diceEntryLabels = new ArrayList<>();
    private LabelAPI noPrismaticLabel;

    private final List<CardDisplayData> cardDisplayData = new ArrayList<>();

    private final List<PrismaticDiceType> filteredDiceList;

    private final CharacterSetupCallback callback;

    private record ClickRegion(float boxX, float boxY, int index) {}
    private record CardLabels(LabelAPI nameLabel, LabelAPI hpLabel, LabelAPI atkLabel, LabelAPI defLabel,
                              LabelAPI orangeLabel, LabelAPI purpleLabel, LabelAPI blueLabel, LabelAPI prismaticLabel) {}
    private record DiceClickRegion(float y, int entryIndex) {}
    private record VersionClickRegion(float x, float y, float width, float height, int entryIndex, boolean useTrue) {}
    private record DiceEntryLabels(LabelAPI nameLabel, LabelAPI facesLabel, LabelAPI descLabel,
                                   String diceId, boolean hasBothVersions,
                                   String defaultDisplayName, String trueDisplayName,
                                   String defaultFaces, String trueFaces) {}

    private record CardDisplayData(String name, int hp, int atk, int def,
                                   int orange, int purple, int blue, int prismatic) {}

    public interface CharacterSetupCallback {
        void onConfirm(String charId, String prismaticDiceId, boolean useTrueVersion, BonusState bonus);
        void onCancel();
    }

    public CharacterSetupPanelUI(CharacterSetupCallback callback) {
        this.callback = callback;

        List<CharacterCard> allCards = CharacterRegistry.getAllCards();
        this.characters = new ArrayList<>();
        for (CharacterCard card : allCards) {
            if (CharacterIds.TRASHCAN.equals(card.getId())) continue;
            if (CosmiconStats.isCharacterUnlocked(card.getId())) {
                this.characters.add(card);
            }
        }
        if (this.characters.isEmpty()) {
            for (CharacterCard card : allCards) {
                if (CharacterIds.TRASHCAN.equals(card.getId())) continue;
                this.characters.add(card);
            }
        }

        List<String> allDiceIds = new ArrayList<>();
        for (Map.Entry<String, PrismaticDiceType> entry : PrismaticDiceRegistry.getAll().entrySet()) {
            if (CosmiconStats.isPrismaticDiceUnlocked(entry.getKey())) {
                allDiceIds.add(entry.getKey());
            }
        }
        if (!allDiceIds.isEmpty()) {
            selectedPrismaticDiceId = allDiceIds.get(0);
            selectedDiceEntryIndex = 0;
            PrismaticDiceType firstType = PrismaticDiceRegistry.get(selectedPrismaticDiceId);
            selectedUseTrueVersion = firstType != null && !PrismaticDisplayHelper.hasDistinctDefaultFaces(firstType);
        }

        if (!characters.isEmpty()) {
            this.selectedIndex = 0;
        }

        for (CharacterCard card : this.characters) {
            DicePoolCounts counts = DicePoolCounts.fromPool(card.getDicePool());
            int prismatic = card.getPrismaticDiceIds().values().stream().mapToInt(Integer::intValue).sum();
            cardDisplayData.add(new CardDisplayData(
                card.getName(), card.getMaxHp(), card.getAtkLevel(), card.getDefLevel(),
                counts.getCount(DiceType.ORANGE_D8), counts.getCount(DiceType.PURPLE_D6),
                counts.getCount(DiceType.BLUE_D4), prismatic));
        }

        this.filteredDiceList = new ArrayList<>();
        for (PrismaticDiceType type : PrismaticDiceRegistry.getAll().values()) {
            if (CosmiconStats.isPrismaticDiceUnlocked(type.getId())) {
                this.filteredDiceList.add(type);
            }
        }
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.panel = panel;
        this.callbacks = callbacks;

        callbacks.getPanelFader().setDurationOut(0.3f);

        createUIElements();
        createCardLabels();
        updateLabels();
    }

    private void createUIElements() {
        if (panel == null) return;

        createHeaderLabels();
        createSelectionBarLabels();
        createBonusUI();
        createPassiveLabel();
        createDiceListLabels();
    }

    private void createHeaderLabels() {
        UIComponentFactory.createLabelSmall(panel,
            Strings.get("menu.play") + " - " + Strings.get("menu.character_setup"),
            COLOR_HEADER, Alignment.LMID, PANEL_WIDTH - MARGIN * 2 - BUTTON_WIDTH, HEADER_HEIGHT, MARGIN, MARGIN);

        TooltipMakerAPI topBtnTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH * 2 + 30f, HEADER_HEIGHT,
            PANEL_WIDTH - BUTTON_WIDTH * 2 - MARGIN - 30f, MARGIN);
        ButtonAPI confirmButton = topBtnTp.addButton(Strings.get("setup.confirm"), ACTION_CONFIRM, BUTTON_WIDTH, HEADER_HEIGHT - 5f, 0f);
        confirmButton.setQuickMode(true);
        confirmButton.getPosition().inTL(0, 0);

        ButtonAPI cancelButton = topBtnTp.addButton(Strings.get("setup.cancel"), ACTION_CANCEL, BUTTON_WIDTH, HEADER_HEIGHT - 5f, 0f);
        cancelButton.setQuickMode(true);
        cancelButton.getPosition().inTL(BUTTON_WIDTH + 10f, 0);
    }

    private void createSelectionBarLabels() {
        float barY = MARGIN + HEADER_HEIGHT + 10f;

        selectedNameLabel = UIComponentFactory.createLabelSmall(panel, "",
            COLOR_SELECTED, Alignment.LMID, PANEL_WIDTH - MARGIN * 2, SELECTION_BAR_HEIGHT, MARGIN, barY);
    }

    private void createBonusUI() {
        float bonusY = PANEL_HEIGHT - BOTTOM_BAR_HEIGHT + 3f;
        float bonusX = MARGIN;

        UIComponentFactory.createLabelSmall(panel, Strings.get("bonus.title") + ":",
            COLOR_SECTION_HEADER, Alignment.LMID, 100f, BONUS_BUTTON_HEIGHT, bonusX, bonusY);

        float btnX = bonusX + 105f;
        bonusButtons.clear();

        boolean hpUnlocked = CosmiconStats.isHpBonusUnlocked();
        boolean atkUnlocked = CosmiconStats.isAtkBonusUnlocked();
        boolean defUnlocked = CosmiconStats.isDefBonusUnlocked();

        TooltipMakerAPI tp = panel.createUIElement(
            BONUS_BUTTON_WIDTH * 4 + 30f, BONUS_BUTTON_HEIGHT + 20f, false);
        tp.setActionListenerDelegate(this);
        panel.addUIElement(tp).inTL(btnX, bonusY - 2f);

        addBonusButton(tp, ACTION_BONUS_NONE, Strings.get("bonus.none"), true);
        addBonusButton(tp, ACTION_BONUS_HP, Strings.get("bonus.hp_9"), hpUnlocked);
        addBonusButton(tp, ACTION_BONUS_ATK, Strings.get("bonus.atk_1"), atkUnlocked);
        addBonusButton(tp, ACTION_BONUS_DEF, Strings.get("bonus.def_1"), defUnlocked);

        float descX = bonusX + 105f + BONUS_BUTTON_WIDTH * 4 + 35f;
        float descWidth = PANEL_WIDTH - descX - MARGIN - 10f;
        bonusDescLabel = UIComponentFactory.createLabelSmall(panel, "",
            new Color(180, 180, 200), Alignment.LMID,
            descWidth, BONUS_BUTTON_HEIGHT, descX, bonusY);

        creditBonusLabel = UIComponentFactory.createLabelSmall(panel, "",
            Color.GREEN, Alignment.LMID,
            descWidth, BONUS_BUTTON_HEIGHT, descX, bonusY);

        updateBonusDescription();
    }

    private void addBonusButton(TooltipMakerAPI tp, String action, String text, boolean unlocked) {
        int index = bonusButtons.size();
        ButtonAPI btn = tp.addButton(text, action, BONUS_BUTTON_WIDTH, BONUS_BUTTON_HEIGHT, 0f);
        btn.getPosition().inTL(index * (BONUS_BUTTON_WIDTH + 5f), 0f);
        btn.setQuickMode(true);
        btn.setEnabled(unlocked);
        if (!unlocked) {
            btn.setOpacity(0.35f);
        }
        if (action.equals(ACTION_BONUS_NONE) && selectedBonus == BonusState.NONE) {
            btn.highlight();
        }
        bonusButtons.put(action, btn);
    }

    private void updateBonusDescription() {
        String descKey;
        if (selectedBonus == BonusState.ATK_1 && selectedIndex >= 0 && selectedIndex < characters.size()
                && characters.get(selectedIndex).getAtkLevel() >= 5) {
            descKey = "bonus.atk_capped";
        } else if (selectedBonus == BonusState.DEF_1 && selectedIndex >= 0 && selectedIndex < characters.size()
                && characters.get(selectedIndex).getDefLevel() >= 5) {
            descKey = "bonus.def_capped";
        } else {
            descKey = switch (selectedBonus) {
                case NONE -> "bonus.none_desc";
                case HP_9 -> "bonus.hp_9_desc";
                case ATK_1 -> "bonus.atk_1_desc";
                case DEF_1 -> "bonus.def_1_desc";
            };
        }
        
        boolean isBasic = selectedIndex >= 0 && selectedIndex < characters.size()
            && CharacterIds.EASY_MODE_CHARACTERS.contains(characters.get(selectedIndex).getId());
        boolean noBonus = selectedBonus == BonusState.NONE;

        String creditKey;
        if (isBasic && noBonus) {
            creditKey = "bonus.credit_red";
        } else if (isBasic || noBonus) {
            creditKey = "bonus.credit_orange";
        } else {
            creditKey = "bonus.credit_green";
        }
        
        String combinedText = Strings.get(descKey) + "   " + Strings.get(creditKey);
        
        if (bonusDescLabel != null) {
            bonusDescLabel.setText(combinedText);
        }
        if (creditBonusLabel != null) {
            creditBonusLabel.setText("");
        }
    }

    private void updateBonusButtonHighlights() {
        for (Map.Entry<String, ButtonAPI> entry : bonusButtons.entrySet()) {
            String action = entry.getKey();
            ButtonAPI btn = entry.getValue();
            boolean isSelected = switch (action) {
                case ACTION_BONUS_NONE -> selectedBonus == BonusState.NONE;
                case ACTION_BONUS_HP -> selectedBonus == BonusState.HP_9;
                case ACTION_BONUS_ATK -> selectedBonus == BonusState.ATK_1;
                case ACTION_BONUS_DEF -> selectedBonus == BonusState.DEF_1;
                default -> false;
            };
            if (isSelected) {
                btn.highlight();
            } else {
                btn.unhighlight();
            }
        }
    }

    private void createPassiveLabel() {
        float passiveY = MARGIN + HEADER_HEIGHT + SELECTION_BAR_HEIGHT + 8f;
        passiveLabel = UIComponentFactory.createLabelSmall(panel, "",
            COLOR_TEXT, Alignment.LMID, PANEL_WIDTH - MARGIN * 2, 45f, MARGIN, passiveY);
    }

    private void createDiceListLabels() {
        if (panel == null) return;

        diceEntryLabels.clear();
        List<PrismaticDiceType> diceList = filteredDiceList;

        float listStartY = CONTENT_START_Y;
        float titleOffset = 20f;

        float labelX = DICE_LIST_X + 8f;
        float leftLabelW = DICE_LEFT_COL_WIDTH - 8f;
        float descX = DICE_LIST_X + DICE_LEFT_COL_WIDTH;
        float descW = DICE_LIST_WIDTH - DICE_LEFT_COL_WIDTH - 8f;

        for (int i = 0; i < diceList.size(); i++) {
            PrismaticDiceType type = diceList.get(i);
            String diceId = type.getId();
            boolean hasBoth = PrismaticDisplayHelper.hasDistinctDefaultFaces(type);
            float entryY = listStartY + titleOffset + i * DICE_ENTRY_HEIGHT;

            String defaultDisplayName = PrismaticDisplayHelper.getDiceDisplayName(diceId);
            String trueDisplayName = Strings.get("setup.prismatic_true_prefix") + defaultDisplayName;
            String defaultFaces = PrismaticDisplayHelper.getFaceValuesDisplay(type, false);
            String trueFaces = PrismaticDisplayHelper.getFaceValuesDisplay(type, true);

            LabelAPI nameLabel = UIComponentFactory.createLabelSmall(panel,
                defaultDisplayName,
                ColorHelper.PRISMATIC_BRIGHT, Alignment.LMID, leftLabelW, 24f,
                labelX, entryY + 2f);

            LabelAPI facesLabel = UIComponentFactory.createLabelSmall(panel,
                "", Color.LIGHT_GRAY, Alignment.LMID, leftLabelW, 16f,
                labelX, entryY + 28f);

            LabelAPI descLabel = UIComponentFactory.createLabelSmall(panel,
                PrismaticDisplayHelper.getEffectDescription(type),
                Color.LIGHT_GRAY, Alignment.LMID, descW, 48f,
                descX, entryY + 2f);

            diceEntryLabels.add(new DiceEntryLabels(nameLabel, facesLabel, descLabel, diceId, hasBoth,
                defaultDisplayName, trueDisplayName, defaultFaces, trueFaces));
        }

        noPrismaticLabel = UIComponentFactory.createLabelSmall(panel,
            Strings.get("setup.prismatic_robin"),
            new Color(180, 100, 100), Alignment.MID, DICE_LIST_WIDTH, 20f,
            DICE_LIST_X, listStartY + 40f);
        noPrismaticLabel.setOpacity(0f);
    }

    private void updateDiceListLabels() {
        for (int i = 0; i < diceEntryLabels.size(); i++) {
            DiceEntryLabels labels = diceEntryLabels.get(i);

            if (!canEquip) {
                labels.nameLabel().setOpacity(0f);
                labels.facesLabel().setOpacity(0f);
                labels.descLabel().setOpacity(0f);
            } else {
                float entryY = CONTENT_START_Y + 20f + i * DICE_ENTRY_HEIGHT - diceScrollOffset;
                float listHeight = PANEL_HEIGHT - CONTENT_START_Y - BOTTOM_BAR_HEIGHT - 15f;
                labels.nameLabel().setOpacity(labelOpacity(entryY + 2f, 24f, CONTENT_START_Y, listHeight));
                labels.facesLabel().setOpacity(labelOpacity(entryY + 28f, 16f, CONTENT_START_Y, listHeight));
                labels.descLabel().setOpacity(labelOpacity(entryY + 2f, 48f, CONTENT_START_Y, listHeight));
            }

            boolean useTrue = (i == selectedDiceEntryIndex) ? selectedUseTrueVersion : !labels.hasBothVersions();

            labels.nameLabel().setText(useTrue && labels.hasBothVersions() ? labels.trueDisplayName() : labels.defaultDisplayName());
            labels.facesLabel().setText(useTrue ? labels.trueFaces() : labels.defaultFaces());
        }
    }

    private void repositionDiceListLabels(float listStartY, float listHeight) {
        float titleOffset = 20f;

        float labelX = DICE_LIST_X + 8f;
        float descX = DICE_LIST_X + DICE_LEFT_COL_WIDTH;

        for (int i = 0; i < diceEntryLabels.size(); i++) {
            DiceEntryLabels labels = diceEntryLabels.get(i);

            float entryY = listStartY + titleOffset + i * DICE_ENTRY_HEIGHT - diceScrollOffset;

            labels.nameLabel().getPosition().inTL(labelX, entryY + 2f);
            labels.nameLabel().setOpacity(labelOpacity(entryY + 2f, 24f, listStartY, listHeight));
            labels.facesLabel().getPosition().inTL(labelX, entryY + 28f);
            labels.facesLabel().setOpacity(labelOpacity(entryY + 28f, 16f, listStartY, listHeight));
            labels.descLabel().getPosition().inTL(descX, entryY + 2f);
            labels.descLabel().setOpacity(labelOpacity(entryY + 2f, 48f, listStartY, listHeight));
        }
    }

    private void createCardLabels() {
        if (panel == null || characters.isEmpty()) return;

        cardLabels.clear();

        float galleryStartY = CONTENT_START_Y;

        for (int i = 0; i < characters.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            float boxX = MARGIN + col * (CARD_WIDTH + GAP_X);
            float boxY = galleryStartY + row * (CARD_HEIGHT + GAP_Y);

            LabelAPI nameLabel = createCardLabel(boxX, boxY + 5f, "", ColorHelper.PLAYER_NAME, CARD_WIDTH, 20f);

            LabelAPI hpLabel = createCardLabel(boxX - 3f, boxY + 14f, "0", Color.WHITE, 50f, 20f);

            float labelCenterY = boxY + CARD_HEIGHT
                - BattleRenderingUtils.ATK_DEF_BOTTOM_MARGIN
                - BattleRenderingUtils.ATK_DEF_ICON_SIZE / 2f - 10f;
            float atkLabelX = boxX + BattleRenderingUtils.ATK_LEFT_MARGIN
                + (BattleRenderingUtils.ATK_DEF_ICON_SIZE - 30f) / 2f;
            float defLabelX = boxX + CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN
                - (BattleRenderingUtils.ATK_DEF_ICON_SIZE + 30f) / 2f;
            LabelAPI atkLabel = createCardLabel(atkLabelX, labelCenterY, "0", ColorHelper.ATTACK_VALUE, 30f, 20f);
            LabelAPI defLabel = createCardLabel(defLabelX, labelCenterY, "0", ColorHelper.DEFENSE_VALUE, 30f, 20f);

            float diceX = boxX + CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN
                - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
            float diceStartY = boxY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

            LabelAPI orangeLabel = createCardLabel(diceX, diceStartY, "0", Color.WHITE, 22f, 16f);
            LabelAPI purpleLabel = createCardLabel(diceX, diceStartY + 21f, "0", Color.WHITE, 22f, 16f);
            LabelAPI blueLabel = createCardLabel(diceX, diceStartY + 42f, "0", Color.WHITE, 22f, 16f);
            LabelAPI prismaticLabel = createCardLabel(diceX, diceStartY + 63f, "0", ColorHelper.PRISMATIC_GOLD, 22f, 16f);

            cardLabels.add(new CardLabels(nameLabel, hpLabel, atkLabel, defLabel, orangeLabel, purpleLabel, blueLabel, prismaticLabel));
        }
    }

    private LabelAPI createCardLabel(float x, float y, String text, Color color, float w, float h) {
        return UIComponentFactory.createLabelSmall(panel, text, color, Alignment.MID, w, h, x, y);
    }

    private static float labelOpacity(float labelTopY, float labelHeight,
            float areaTopY, float areaHeight) {
        float areaBottomY = areaTopY + areaHeight;
        return (labelTopY >= areaTopY && labelTopY + labelHeight <= areaBottomY) ? 1f : 0f;
    }

    private void repositionCardLabels(float galleryStartY, float galleryHeight) {
        for (int i = 0; i < characters.size() && i < cardLabels.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            float boxX = MARGIN + col * (CARD_WIDTH + GAP_X);
            float boxY = galleryStartY + row * (CARD_HEIGHT + GAP_Y) - scrollOffset;

            CardLabels labels = cardLabels.get(i);

            labels.nameLabel.getPosition().inTL(boxX, boxY + 5f);
            labels.nameLabel.setOpacity(labelOpacity(boxY + 5f, 20f, galleryStartY, galleryHeight));

            labels.hpLabel.getPosition().inTL(boxX - 3f, boxY + 14f);
            labels.hpLabel.setOpacity(labelOpacity(boxY + 14f, 20f, galleryStartY, galleryHeight));

            float labelCenterY = boxY + CARD_HEIGHT
                - BattleRenderingUtils.ATK_DEF_BOTTOM_MARGIN
                - BattleRenderingUtils.ATK_DEF_ICON_SIZE / 2f - 10f;

            labels.atkLabel.getPosition().inTL(
                boxX + BattleRenderingUtils.ATK_LEFT_MARGIN + (BattleRenderingUtils.ATK_DEF_ICON_SIZE - 30f) / 2f,
                labelCenterY);
            labels.atkLabel.setOpacity(labelOpacity(labelCenterY, 20f, galleryStartY, galleryHeight));

            labels.defLabel.getPosition().inTL(
                boxX + CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN - (BattleRenderingUtils.ATK_DEF_ICON_SIZE + 30f) / 2f,
                labelCenterY);
            labels.defLabel.setOpacity(labelOpacity(labelCenterY, 20f, galleryStartY, galleryHeight));

            float diceX = boxX + CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN
                - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
            float diceStartY = boxY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

            labels.orangeLabel.getPosition().inTL(diceX, diceStartY);
            labels.orangeLabel.setOpacity(labelOpacity(diceStartY, 16f, galleryStartY, galleryHeight));
            labels.purpleLabel.getPosition().inTL(diceX, diceStartY + 21f);
            labels.purpleLabel.setOpacity(labelOpacity(diceStartY + 21f, 16f, galleryStartY, galleryHeight));
            labels.blueLabel.getPosition().inTL(diceX, diceStartY + 42f);
            labels.blueLabel.setOpacity(labelOpacity(diceStartY + 42f, 16f, galleryStartY, galleryHeight));
            labels.prismaticLabel.getPosition().inTL(diceX, diceStartY + 63f);
            labels.prismaticLabel.setOpacity(labelOpacity(diceStartY + 63f, 16f, galleryStartY, galleryHeight));
        }
    }

    private void updateCardLabels() {
        for (int i = 0; i < cardDisplayData.size() && i < cardLabels.size(); i++) {
            CardDisplayData data = cardDisplayData.get(i);
            CardLabels labels = cardLabels.get(i);

            labels.nameLabel.setText(data.name());
            labels.hpLabel.setText(String.valueOf(data.hp()));
            labels.atkLabel.setText(String.valueOf(data.atk()));
            labels.defLabel.setText(String.valueOf(data.def()));
            labels.orangeLabel.setText(String.valueOf(data.orange()));
            labels.purpleLabel.setText(String.valueOf(data.purple()));
            labels.blueLabel.setText(String.valueOf(data.blue()));
            labels.prismaticLabel.setText(String.valueOf(data.prismatic()));
        }
    }

    private void updateLabels() {
        if (selectedIndex < 0 || selectedIndex >= characters.size()) {
            selectedNameLabel.setText(Strings.get("setup.no_selection"));
            passiveLabel.setText("");
            return;
        }

        CharacterCard card = characters.get(selectedIndex);
        selectedNameLabel.setText(Strings.format("setup.selected", card.getName()));

        String passive = card.getPassiveDescription();
        if (passive != null && !passive.isEmpty()) {
            passiveLabel.setText(passive);
        } else {
            passiveLabel.setText("");
        }
    }

    // --- Rendering ---

    public void renderBelow(float alphaMult) {
        if (panel == null) return;
        PositionAPI pos = panel.getPosition();
        float panelX = pos.getX();
        float panelY = pos.getY();

        UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT));

        Misc.renderQuad(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BG_DARK, alphaMult);

        float galleryStartY = CONTENT_START_Y;
        float galleryHeight = PANEL_HEIGHT - galleryStartY - BOTTOM_BAR_HEIGHT - 15f;
        float galleryWidth = GALLERY_WIDTH - MARGIN;

        float scale = Global.getSettings().getScreenScaleMult();
        int sX = Math.round(panelX * scale);
        int sY = Math.round((panelY + PANEL_HEIGHT - galleryStartY - galleryHeight) * scale);
        int sW = Math.round(galleryWidth * scale);
        int sH = Math.round(galleryHeight * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sX, sY, sW, sH);

        Misc.renderQuad(glX(MARGIN - 5f), glSpriteY(galleryStartY, galleryHeight),
            galleryWidth + 10f, galleryHeight,
            COLOR_GALLERY_BG, alphaMult * 0.7f);

        renderCardBoxes(galleryStartY, alphaMult, galleryHeight, sX, sY, sW, sH);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        renderScrollbar(galleryStartY, galleryHeight, alphaMult);

        canEquip = selectedIndex >= 0 && selectedIndex < characters.size()
            && !characters.get(selectedIndex).getPrismaticDiceIds().isEmpty();

        renderDiceList(alphaMult, panelX, panelY, scale);

        // Bonus section background removed - bonus now shares button row

        UnifiedCoord.clearCurrent();

        boolean scrollChanged = Float.compare(prevScrollOffset, scrollOffset) != 0
            || Float.compare(prevDiceScrollOffset, diceScrollOffset) != 0;
        boolean selectionChanged = prevSelectedIndex != selectedIndex
            || prevDiceEntryIndex != selectedDiceEntryIndex
            || prevUseTrueVersion != selectedUseTrueVersion;

        if (needsLabelUpdate || scrollChanged || selectionChanged) {
            if (scrollChanged || needsLabelUpdate || selectionChanged) {
                repositionCardLabels(galleryStartY, galleryHeight);
                float diceListStartY = CONTENT_START_Y;
                float diceListHeight = PANEL_HEIGHT - diceListStartY - BOTTOM_BAR_HEIGHT - 15f;
                repositionDiceListLabels(diceListStartY, diceListHeight);
            }
            if (selectionChanged || needsLabelUpdate) {
                updateLabels();
                updateDiceListLabels();
            }
            updateCardLabels();

            prevScrollOffset = scrollOffset;
            prevDiceScrollOffset = diceScrollOffset;
            prevSelectedIndex = selectedIndex;
            prevDiceEntryIndex = selectedDiceEntryIndex;
            prevUseTrueVersion = selectedUseTrueVersion;
            needsLabelUpdate = false;
        }
    }

    private void renderCardBoxes(float startY, float alphaMult, float galleryHeight,
            int scissorX, int scissorY, int scissorW, int scissorH) {
        clickRegions.clear();

        int totalRows = (int) Math.ceil((float) characters.size() / COLS);
        float totalContentHeight = totalRows * (CARD_HEIGHT + GAP_Y) - GAP_Y;
        maxScroll = Math.max(0f, totalContentHeight - galleryHeight + EXTRA_SCROLL_PADDING);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < -EXTRA_SCROLL_PADDING) scrollOffset = -EXTRA_SCROLL_PADDING;

        for (int i = 0; i < characters.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            float boxX = MARGIN + col * (CARD_WIDTH + GAP_X);
            float boxY = startY + row * (CARD_HEIGHT + GAP_Y) - scrollOffset;

            float cardBottom = boxY + CARD_HEIGHT;
            if (cardBottom < startY || boxY > startY + galleryHeight) {
                continue;
            }

            clickRegions.add(new ClickRegion(boxX, boxY, i));

            float cardGlX = glX(boxX);
            float cardGlY = glSpriteY(boxY, CARD_HEIGHT);

            CharacterCard card = characters.get(i);
            BattleRenderingUtils.renderCharacterCard(cardGlX, cardGlY, card, card.getAtkLevel(), card.getDefLevel(), alphaMult);

            if (i == selectedIndex) {
                GLStateUtil.resetBlendState();
                float[] c = ColorHelper.toGLComponents(COLOR_SELECTED, alphaMult, SCRATCH_COLOR);
                GL11.glColor4f(c[0], c[1], c[2], c[3]);
                float hlPad = 3f;
                GL11.glLineWidth(5f);
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(cardGlX - hlPad, cardGlY - hlPad);
                GL11.glVertex2f(cardGlX + CARD_WIDTH + hlPad, cardGlY - hlPad);
                GL11.glVertex2f(cardGlX + CARD_WIDTH + hlPad, cardGlY + CARD_HEIGHT + hlPad);
                GL11.glVertex2f(cardGlX - hlPad, cardGlY + CARD_HEIGHT + hlPad);
                GL11.glEnd();
                GLStateUtil.resetColor();
            }

            BattleRenderingUtils.renderHpCircle(glX(boxX + 22f), glSpriteY(boxY + 24f, 0f),
                BattleRenderingUtils.HP_CIRCLE_RADIUS, 1f, alphaMult);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
        }
    }

    private void renderScrollbar(float trackUiY, float galleryHeight, float alphaMult) {
        if (maxScroll <= 0f) return;

        float trackX = GALLERY_WIDTH - 15f;

        float visibleRatio = galleryHeight / (galleryHeight + maxScroll);
        float thumbHeight = Math.max(20f, galleryHeight * visibleRatio);
        float thumbTravel = galleryHeight - thumbHeight;
        float scrollRatio = maxScroll > 0f ? scrollOffset / maxScroll : 0f;
        float thumbUiY = trackUiY + scrollRatio * thumbTravel;

        scrollThumbUiY = thumbUiY;
        scrollThumbHeight = thumbHeight;

        GLStateUtil.resetBlendState();

        Misc.renderQuad(glX(trackX), glSpriteY(trackUiY, galleryHeight),
            SCROLLBAR_WIDTH, galleryHeight, COLOR_SCROLLBAR_TRACK, alphaMult);

        Misc.renderQuad(glX(trackX), glSpriteY(thumbUiY, thumbHeight),
            SCROLLBAR_WIDTH, thumbHeight, COLOR_SCROLLBAR_THUMB, alphaMult);
    }

    // --- Dice list rendering ---

    private void renderDiceList(float alphaMult, float panelX, float panelY, float scale) {
        float listStartY = CONTENT_START_Y;
        float listHeight = PANEL_HEIGHT - listStartY - BOTTOM_BAR_HEIGHT - 15f;

        GLStateUtil.resetBlendState();

        float bgGlX = glX(DICE_LIST_X - 5f);
        float bgGlY = glSpriteY(listStartY, listHeight);
        Misc.renderQuad(bgGlX, bgGlY,
            DICE_LIST_WIDTH + 10f, listHeight,
            COLOR_DICE_PANEL_BG, alphaMult);

        float[] c = ColorHelper.toGLComponents(COLOR_SECTION_HEADER, alphaMult * 0.6f, SCRATCH_COLOR);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(bgGlX, bgGlY);
        GL11.glVertex2f(bgGlX + DICE_LIST_WIDTH + 10f, bgGlY);
        GL11.glVertex2f(bgGlX + DICE_LIST_WIDTH + 10f, bgGlY + listHeight);
        GL11.glVertex2f(bgGlX, bgGlY + listHeight);
        GL11.glEnd();
        GLStateUtil.resetColor();

        if (noPrismaticLabel != null) {
            noPrismaticLabel.setOpacity(canEquip ? 0f : 1f);
        }

        if (!canEquip) {
            renderDiceScrollbar(listStartY, listHeight, alphaMult);
            return;
        }

        int dsX = Math.round((panelX + DICE_LIST_X) * scale);
        int dsY = Math.round((panelY + PANEL_HEIGHT - listStartY - listHeight) * scale);
        int dsW = Math.round(DICE_LIST_WIDTH * scale);
        int dsH = Math.round(listHeight * scale);

        renderDiceEntries(listStartY, listHeight, alphaMult, dsX, dsY, dsW, dsH);
        renderDiceScrollbar(listStartY, listHeight, alphaMult);
    }

    private void renderDiceEntries(float startY, float height, float alphaMult,
            int scissorX, int scissorY, int scissorW, int scissorH) {
        diceClickRegions.clear();
        versionClickRegions.clear();

        List<PrismaticDiceType> diceList = filteredDiceList;

        float titleOffset = 20f;
        float totalContentHeight = titleOffset + diceList.size() * DICE_ENTRY_HEIGHT;
        diceMaxScroll = Math.max(0f, totalContentHeight - height);
        if (diceScrollOffset > diceMaxScroll) diceScrollOffset = diceMaxScroll;
        if (diceScrollOffset < 0f) diceScrollOffset = 0f;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        for (int i = 0; i < diceList.size(); i++) {
            PrismaticDiceType type = diceList.get(i);
            float entryY = startY + titleOffset + i * DICE_ENTRY_HEIGHT - diceScrollOffset;

            float entryBottom = entryY + DICE_ENTRY_HEIGHT;
            if (entryBottom < startY || entryY > startY + height) continue;

            boolean isSelected = (i == selectedDiceEntryIndex);
            boolean hasBothVersions = PrismaticDisplayHelper.hasDistinctDefaultFaces(type);

            diceClickRegions.add(new DiceClickRegion(entryY, i));

            float entryGlX = glX(DICE_LIST_X);
            float entryGlY = glSpriteY(entryY, DICE_ENTRY_HEIGHT);
            Color bgColor = isSelected ? COLOR_DICE_ENTRY_SELECTED : COLOR_DICE_ENTRY_BG;
            Misc.renderQuad(entryGlX, entryGlY,
                DICE_LIST_WIDTH, DICE_ENTRY_HEIGHT, bgColor, alphaMult);

            if (isSelected) {
                float[] sel = ColorHelper.toGLComponents(COLOR_SELECTED, alphaMult * 0.8f, SCRATCH_COLOR);
                GL11.glColor4f(sel[0], sel[1], sel[2], sel[3]);
                GL11.glLineWidth(2f);
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(entryGlX, entryGlY);
                GL11.glVertex2f(entryGlX + DICE_LIST_WIDTH, entryGlY);
                GL11.glVertex2f(entryGlX + DICE_LIST_WIDTH, entryGlY + DICE_ENTRY_HEIGHT);
                GL11.glVertex2f(entryGlX, entryGlY + DICE_ENTRY_HEIGHT);
                GL11.glEnd();
                GLStateUtil.resetColor();
            }

            // Version toggle (D/T radio buttons)
            if (hasBothVersions) {
                renderVersionToggle(entryY, alphaMult, i);
            }

            // Face values text
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderVersionToggle(float entryY, float alphaMult, int entryIndex) {
        float radioSize = 12f;
        float toggleX = DICE_LIST_X + 8f;
        float toggleY = entryY + DICE_ENTRY_HEIGHT - 18f;

        String diceId = filteredDiceList.get(entryIndex).getId();
        boolean trueUnlocked = CosmiconStats.isPrismaticTrueUnlocked(diceId);

        boolean useTrue = entryIndex == selectedDiceEntryIndex && selectedUseTrueVersion;

        // "D" radio
        float dCenterX = glX(toggleX + radioSize / 2f);
        float dCenterY = glSpriteY(toggleY + radioSize / 2f, 0f);
        Color dColor = (!useTrue && entryIndex == selectedDiceEntryIndex) ? COLOR_RADIO_SELECTED : COLOR_RADIO_UNSELECTED;
        boolean dFilled = !trueUnlocked || (!useTrue && entryIndex == selectedDiceEntryIndex);
        drawRadioCircle(dCenterX, dCenterY, radioSize, dColor, alphaMult, dFilled);

        versionClickRegions.add(new VersionClickRegion(toggleX, toggleY, radioSize + 14f, radioSize, entryIndex, false));

        if (trueUnlocked) {
            // "T" radio
            float tX = toggleX + radioSize + 18f;
            float tCenterX = glX(tX + radioSize / 2f);
            float tCenterY = glSpriteY(toggleY + radioSize / 2f, 0f);
            Color tColor = (useTrue && entryIndex == selectedDiceEntryIndex) ? COLOR_RADIO_SELECTED : COLOR_RADIO_UNSELECTED;
            drawRadioCircle(tCenterX, tCenterY, radioSize, tColor, alphaMult, useTrue && entryIndex == selectedDiceEntryIndex);

            versionClickRegions.add(new VersionClickRegion(tX, toggleY, radioSize + 14f, radioSize, entryIndex, true));
        }

        // D/T labels - drawn as small colored quads with text approximation
        float[] labelC = ColorHelper.toGLComponents(COLOR_VERSION_LABEL, alphaMult, SCRATCH_COLOR);
        GL11.glColor4f(labelC[0], labelC[1], labelC[2], labelC[3]);

        float indicatorSize = 8f;
        Misc.renderQuad(glX(toggleX + radioSize + 2f), glSpriteY(toggleY + 2f, indicatorSize), indicatorSize, indicatorSize, COLOR_VERSION_LABEL, alphaMult);

        if (trueUnlocked) {
            float tX = toggleX + radioSize + 18f;
            Misc.renderQuad(glX(tX + radioSize + 2f), glSpriteY(toggleY + 2f, indicatorSize), indicatorSize, indicatorSize, COLOR_VERSION_LABEL, alphaMult);
        }

        GLStateUtil.resetColor();
    }

    private void drawRadioCircle(float centerX, float centerY, float size, Color color, float alphaMult, boolean filled) {
        float outerRadius = size / 2f;
        float innerRadius = outerRadius * 0.4f;

        float[] outer = ColorHelper.toGLComponents(color, alphaMult, SCRATCH_COLOR);
        GL11.glColor4f(outer[0], outer[1], outer[2], outer[3]);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < RADIO_SEGMENTS; i++) {
            GL11.glVertex2f(centerX + RADIO_COS[i] * outerRadius,
                            centerY + RADIO_SIN[i] * outerRadius);
        }
        GL11.glEnd();

        if (filled) {
            GL11.glBegin(GL11.GL_POLYGON);
            for (int i = 0; i < RADIO_SEGMENTS; i++) {
                GL11.glVertex2f(centerX + RADIO_COS[i] * innerRadius,
                                centerY + RADIO_SIN[i] * innerRadius);
            }
            GL11.glEnd();
        }
    }

    private void renderDiceScrollbar(float trackUiY, float listHeight, float alphaMult) {
        if (diceMaxScroll <= 0f) return;

        float trackX = DICE_LIST_X + DICE_LIST_WIDTH + 2f;

        float visibleRatio = listHeight / (listHeight + diceMaxScroll);
        float thumbHeight = Math.max(20f, listHeight * visibleRatio);
        float thumbTravel = listHeight - thumbHeight;
        float scrollRatio = diceMaxScroll > 0f ? diceScrollOffset / diceMaxScroll : 0f;
        float thumbUiY = trackUiY + scrollRatio * thumbTravel;

        diceScrollThumbUiY = thumbUiY;
        diceScrollThumbHeight = thumbHeight;

        GLStateUtil.resetBlendState();

        Misc.renderQuad(glX(trackX), glSpriteY(trackUiY, listHeight),
            SCROLLBAR_WIDTH, listHeight, COLOR_SCROLLBAR_TRACK, alphaMult);

        Misc.renderQuad(glX(trackX), glSpriteY(thumbUiY, thumbHeight),
            SCROLLBAR_WIDTH, thumbHeight, COLOR_SCROLLBAR_THUMB, alphaMult);
    }

    // --- Input handling ---

    @Override
    public void advance(float amount) {
        if (panel == null) return;
        if (maxScroll > 0f || diceMaxScroll > 0f) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) {
                PositionAPI pos = panel.getPosition();
                float mouseX = Mouse.getX() / Global.getSettings().getScreenScaleMult();
                float panelX = pos.getX();
                float relMouseX = mouseX - panelX;

                if (relMouseX >= DICE_LIST_X - 10f) {
                    diceScrollOffset -= wheel / 4f;
                    if (diceScrollOffset < 0f) diceScrollOffset = 0f;
                    if (diceScrollOffset > diceMaxScroll) diceScrollOffset = diceMaxScroll;
                } else {
                    scrollOffset -= wheel / 4f;
                    if (scrollOffset < -EXTRA_SCROLL_PADDING) scrollOffset = -EXTRA_SCROLL_PADDING;
                    if (scrollOffset > maxScroll) scrollOffset = maxScroll;
                }
            }
        }
    }

    public void processInput(List<InputEventAPI> events) {
        if (panel == null) return;
        boolean mouseDown = Mouse.isButtonDown(0);

        if (!mouseDown && !wasMousePressed && !isDraggingScrollbar && !isDraggingDiceScrollbar) {
            return;
        }

        PositionAPI pos = panel.getPosition();
        UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(
            pos.getX(), pos.getY(), PANEL_WIDTH, PANEL_HEIGHT));
        UnifiedCoord mousePos = UnifiedCoord.fromMouse();

        float galleryStartY = CONTENT_START_Y;
        float galleryHeight = PANEL_HEIGHT - galleryStartY - BOTTOM_BAR_HEIGHT - 15f;
        float trackX = GALLERY_WIDTH - 15f;

        float diceListStartY = CONTENT_START_Y;
        float diceListHeight = PANEL_HEIGHT - diceListStartY - BOTTOM_BAR_HEIGHT - 15f;
        float diceTrackX = DICE_LIST_X + DICE_LIST_WIDTH + 2f;

        // Gallery scrollbar drag
        if (isDraggingScrollbar) {
            if (mouseDown) {
                float deltaY = mousePos.uiY() - dragStartMouseY;
                float thumbTravel = galleryHeight - scrollThumbHeight;
                if (thumbTravel > 0f) {
                    scrollOffset = dragStartScrollOffset + deltaY / thumbTravel * maxScroll;
                    if (scrollOffset < -EXTRA_SCROLL_PADDING) scrollOffset = -EXTRA_SCROLL_PADDING;
                    if (scrollOffset > maxScroll) scrollOffset = maxScroll;
                }
            } else {
                isDraggingScrollbar = false;
            }
        }
        // Dice list scrollbar drag
        else if (isDraggingDiceScrollbar) {
            if (mouseDown) {
                float deltaY = mousePos.uiY() - diceDragStartMouseY;
                float thumbTravel = diceListHeight - diceScrollThumbHeight;
                if (thumbTravel > 0f) {
                    diceScrollOffset = diceDragStartScrollOffset + deltaY / thumbTravel * diceMaxScroll;
                    if (diceScrollOffset < 0f) diceScrollOffset = 0f;
                    if (diceScrollOffset > diceMaxScroll) diceScrollOffset = diceMaxScroll;
                }
            } else {
                isDraggingDiceScrollbar = false;
            }
        }
        else if (mouseDown && !wasMousePressed) {
            // Gallery scrollbar click
            if (maxScroll > 0f && mousePos.uiX() >= trackX && mousePos.uiX() <= trackX + SCROLLBAR_WIDTH
                    && mousePos.uiY() >= galleryStartY && mousePos.uiY() <= galleryStartY + galleryHeight) {
                float thumbTop = scrollThumbUiY;
                float thumbBottom = thumbTop + scrollThumbHeight;
                if (mousePos.uiY() >= thumbTop && mousePos.uiY() <= thumbBottom) {
                    isDraggingScrollbar = true;
                    dragStartMouseY = mousePos.uiY();
                    dragStartScrollOffset = scrollOffset;
                } else {
                    float thumbTravel = galleryHeight - scrollThumbHeight;
                    if (thumbTravel > 0f) {
                        float clickRatio = (mousePos.uiY() - galleryStartY) / thumbTravel;
                        if (clickRatio < 0f) clickRatio = 0f;
                        if (clickRatio > 1f) clickRatio = 1f;
                        scrollOffset = clickRatio * maxScroll;
                    }
                }
            }
            // Dice list scrollbar click
            else if (diceMaxScroll > 0f && mousePos.uiX() >= diceTrackX && mousePos.uiX() <= diceTrackX + SCROLLBAR_WIDTH
                    && mousePos.uiY() >= diceListStartY && mousePos.uiY() <= diceListStartY + diceListHeight) {
                float thumbTop = diceScrollThumbUiY;
                float thumbBottom = thumbTop + diceScrollThumbHeight;
                if (mousePos.uiY() >= thumbTop && mousePos.uiY() <= thumbBottom) {
                    isDraggingDiceScrollbar = true;
                    diceDragStartMouseY = mousePos.uiY();
                    diceDragStartScrollOffset = diceScrollOffset;
                } else {
                    float thumbTravel = diceListHeight - diceScrollThumbHeight;
                    if (thumbTravel > 0f) {
                        float clickRatio = (mousePos.uiY() - diceListStartY) / thumbTravel;
                        if (clickRatio < 0f) clickRatio = 0f;
                        if (clickRatio > 1f) clickRatio = 1f;
                        diceScrollOffset = clickRatio * diceMaxScroll;
                    }
                }
            }
            // Version toggle click
            else {
                boolean versionClicked = false;
                for (VersionClickRegion region : versionClickRegions) {
                    if (mousePos.isInsideRect(region.x, region.y, region.width, region.height)) {
                        handleVersionToggle(region.entryIndex, region.useTrue());
                        CosmiconSFX.playUIBeep();
                        versionClicked = true;
                        break;
                    }
                }

                if (!versionClicked) {
                    // Dice entry click
                    boolean diceClicked = false;
                    for (DiceClickRegion region : diceClickRegions) {
                        if (mousePos.isInsideRect(DICE_LIST_X, region.y, DICE_LIST_WIDTH, DICE_ENTRY_HEIGHT)) {
                            handleDiceSelection(region.entryIndex);
                            CosmiconSFX.playUIBeep();
                            diceClicked = true;
                            break;
                        }
                    }

                    // Card click
                    if (!diceClicked) {
                        for (ClickRegion region : clickRegions) {
                            if (mousePos.isInsideRect(region.boxX, region.boxY, CARD_WIDTH, CARD_HEIGHT)) {
                                handleCardSelection(region.index);
                                CosmiconSFX.playUIBeep();
                                break;
                            }
                        }
                    }
                }
            }
        }

        wasMousePressed = mouseDown;
        UnifiedCoord.clearCurrent();
    }

    private void handleCardSelection(int index) {
        if (index < 0 || index >= characters.size()) return;
        selectedIndex = index;
        selectedBonus = CosmiconPlayerState.loadBonusSelection(characters.get(index).getId());
        updateBonusDescription();
        updateBonusButtonHighlights();
    }

    private List<PrismaticDiceType> getFilteredDiceList() {
        return filteredDiceList;
    }

    private void handleDiceSelection(int entryIndex) {
        if (selectedIndex < 0 || selectedIndex >= characters.size()) return;
        if (characters.get(selectedIndex).getPrismaticDiceIds().isEmpty()) return;

        List<PrismaticDiceType> diceList = getFilteredDiceList();
        if (entryIndex < 0 || entryIndex >= diceList.size()) return;

        selectedDiceEntryIndex = entryIndex;
        PrismaticDiceType type = diceList.get(entryIndex);
        selectedPrismaticDiceId = type.getId();

        boolean hasBoth = PrismaticDisplayHelper.hasDistinctDefaultFaces(type);
        if (!hasBoth) {
            selectedUseTrueVersion = true;
        }
    }

    private void handleVersionToggle(int entryIndex, boolean useTrue) {
        if (selectedIndex < 0 || selectedIndex >= characters.size()) return;
        if (characters.get(selectedIndex).getPrismaticDiceIds().isEmpty()) return;

        List<PrismaticDiceType> diceList = getFilteredDiceList();
        if (entryIndex < 0 || entryIndex >= diceList.size()) return;

        if (useTrue) {
            String diceId = diceList.get(entryIndex).getId();
            if (!CosmiconStats.isPrismaticTrueUnlocked(diceId)) return;
        }

        selectedDiceEntryIndex = entryIndex;
        selectedUseTrueVersion = useTrue;
        selectedPrismaticDiceId = diceList.get(entryIndex).getId();
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (!(source instanceof ButtonAPI btn)) return;

        Object data = btn.getCustomData();
        if (data == null) return;

        String action = data.toString();

        switch (action) {
            case ACTION_CONFIRM -> {
                if (selectedIndex >= 0 && selectedIndex < characters.size()) {
                    CharacterCard card = characters.get(selectedIndex);
                    CosmiconPlayerState.saveBonusSelection(card.getId(), selectedBonus);
                    CosmiconPlayerState.setCreditBonusActive(selectedBonus == BonusState.NONE);
                    if (callback != null) {
                        String diceId = card.getPrismaticDiceIds().isEmpty() ? null : selectedPrismaticDiceId;
                        callback.onConfirm(card.getId(), diceId, selectedUseTrueVersion, selectedBonus);
                    }
                    callbacks.dismissDialog();
                }
            }
            case ACTION_CANCEL -> {
                if (selectedIndex >= 0 && selectedIndex < characters.size()) {
                    CosmiconPlayerState.saveBonusSelection(characters.get(selectedIndex).getId(), selectedBonus);
                }
                if (callback != null) {
                    callback.onCancel();
                }
                callbacks.dismissDialog();
            }
            case ACTION_BONUS_NONE -> {
                selectedBonus = BonusState.NONE;
                updateBonusDescription();
                updateBonusButtonHighlights();
            }
            case ACTION_BONUS_HP -> {
                if (CosmiconStats.isHpBonusUnlocked()) {
                    selectedBonus = BonusState.HP_9;
                    updateBonusDescription();
                    updateBonusButtonHighlights();
                }
            }
            case ACTION_BONUS_ATK -> {
                if (CosmiconStats.isAtkBonusUnlocked() && selectedIndex >= 0 && selectedIndex < characters.size()) {
                    CharacterCard card = characters.get(selectedIndex);
                    if (card.getAtkLevel() < 5) {
                        selectedBonus = BonusState.ATK_1;
                    }
                    updateBonusDescription();
                    updateBonusButtonHighlights();
                }
            }
            case ACTION_BONUS_DEF -> {
                if (CosmiconStats.isDefBonusUnlocked() && selectedIndex >= 0 && selectedIndex < characters.size()) {
                    CharacterCard card = characters.get(selectedIndex);
                    if (card.getDefLevel() < 5) {
                        selectedBonus = BonusState.DEF_1;
                    }
                    updateBonusDescription();
                    updateBonusButtonHighlights();
                }
            }
        }
    }

    public void setSelection(String charId, String diceId) {
        setSelection(charId, diceId, false);
    }

    public void setSelection(String charId, String diceId, boolean useTrueVersion) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i).getId().equals(charId)) {
                selectedIndex = i;
                selectedBonus = CosmiconPlayerState.loadBonusSelection(charId);
                updateBonusDescription();
                updateBonusButtonHighlights();
                break;
            }
        }

        List<PrismaticDiceType> diceList = getFilteredDiceList();
        for (int i = 0; i < diceList.size(); i++) {
            if (diceList.get(i).getId().equals(diceId)) {
                selectedDiceEntryIndex = i;
                selectedPrismaticDiceId = diceId;
                PrismaticDiceType type = diceList.get(i);
                boolean hasBoth = PrismaticDisplayHelper.hasDistinctDefaultFaces(type);
                if (!hasBoth) {
                    selectedUseTrueVersion = true;
                } else {
                    selectedUseTrueVersion = useTrueVersion;
                }
                break;
            }
        }
    }

    public void setDefaultSelection() {
        if (!characters.isEmpty()) {
            selectedIndex = 0;
            CharacterCard firstCard = characters.get(0);
            selectedBonus = CosmiconPlayerState.loadBonusSelection(firstCard.getId());
            updateBonusDescription();
            updateBonusButtonHighlights();
            String defaultDice = CosmiconPlayerState.getDefaultPrismaticForCharacter(firstCard.getId());
            if (defaultDice != null) {
                selectedPrismaticDiceId = defaultDice;
                List<PrismaticDiceType> diceList = getFilteredDiceList();
                for (int i = 0; i < diceList.size(); i++) {
                    if (diceList.get(i).getId().equals(defaultDice)) {
                        selectedDiceEntryIndex = i;
                        PrismaticDiceType type = diceList.get(i);
                        if (!PrismaticDisplayHelper.hasDistinctDefaultFaces(type)) {
                            selectedUseTrueVersion = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    public void cleanup() {
        panel = null;
        callbacks = null;
        selectedNameLabel = null;
        passiveLabel = null;
        bonusDescLabel = null;
        creditBonusLabel = null;
        noPrismaticLabel = null;
        bonusButtons.clear();
        clickRegions.clear();
        cardLabels.clear();
        diceClickRegions.clear();
        versionClickRegions.clear();
        diceEntryLabels.clear();
    }
}

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
import com.fs.starfarer.api.graphics.SpriteAPI;
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
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.CharacterRegistry;
import data.scripts.cosmicon.battle.CosmiconSprites;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.battle.DicePoolCounts;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.UnifiedCoord;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UIComponentFactory;
import data.scripts.cosmicon.state.CosmiconPlayerState;

public class CharacterSetupPanelUI extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final float PANEL_WIDTH = 1000f;
    private static final float PANEL_HEIGHT = 700f;
    private static final float CARD_WIDTH = 120f;
    private static final float CARD_HEIGHT = 170f;
    private static final int COLS = 5;
    private static final float GAP_X = 15f;
    private static final float GAP_Y = 20f;
    private static final float MARGIN = 30f;
    private static final float HEADER_HEIGHT = 40f;
    private static final float SELECTION_BAR_HEIGHT = 30f;
    private static final float INFO_PANEL_X = 710f;
    private static final float INFO_PANEL_WIDTH = 270f;
    private static final float BUTTON_AREA_HEIGHT = 50f;
    private static final float BUTTON_WIDTH = 140f;
    private static final float BUTTON_HEIGHT = 35f;

    private static final Color COLOR_BG_DARK = new Color(20, 20, 30);
    private static final Color COLOR_BOX_BG = new Color(45, 50, 65);
    private static final Color COLOR_SELECTED = new Color(255, 215, 0);
    private static final Color COLOR_UNSELECTED = new Color(80, 85, 100);
    private static final Color COLOR_HEADER = new Color(100, 150, 255);
    private static final Color COLOR_TEXT = new Color(220, 220, 230);
    private static final Color COLOR_INFO_PANEL_BG = new Color(35, 40, 55, 220);
    private static final Color COLOR_SECTION_HEADER = new Color(100, 120, 150);

    private static final String ACTION_CONFIRM = "setup_confirm";
    private static final String ACTION_CANCEL = "setup_cancel";
    private static final String ACTION_CHANGE_DICE = "setup_change_dice";
    private static final String ACTION_BACK = "setup_back";

    private int selectedIndex = -1;
    private String selectedPrismaticDiceId = null;
    private final List<CharacterCard> characters;

    private CustomPanelAPI panel;
    private DialogCallbacks callbacks;

    private LabelAPI selectedNameLabel;
    private LabelAPI prismaticLabel;
    private LabelAPI passiveLabel;
    private LabelAPI prismaticEffectLabel;

    private boolean buttonsCreated = false;
    private boolean wasMousePressed = false;

    private final List<ClickRegion> clickRegions = new ArrayList<>();

    private final CharacterSetupCallback callback;

    private CustomPanelAPI popupPanel = null;

    private record ClickRegion(float boxX, float boxY, float width, float height, int index) {}

    public interface CharacterSetupCallback {
        void onConfirm(String charId, String prismaticDiceId);
        void onCancel();
    }

    public CharacterSetupPanelUI(CharacterSetupCallback callback) {
        this.callback = callback;
        this.characters = CharacterRegistry.getAllCards();
        if (!characters.isEmpty()) {
            this.selectedIndex = 0;
            CharacterCard first = characters.get(0);
            Map<String, Integer> prismatic = first.getPrismaticDiceIds();
            if (prismatic != null && !prismatic.isEmpty()) {
                this.selectedPrismaticDiceId = prismatic.keySet().iterator().next();
            }
        }
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.panel = panel;
        this.callbacks = callbacks;

        callbacks.getPanelFader().setDurationOut(0.3f);

        createUIElements();
        updateLabels();
    }

    private void createUIElements() {
        if (panel == null) return;

        createHeaderLabels();
        createSelectionBarLabels();
        createInfoPanelLabels();
        createButtons();
    }

    private void createHeaderLabels() {
        UIComponentFactory.createLabelSmall(panel, 
            Strings.get("menu.play") + " - " + Strings.get("menu.character_setup"), 
            COLOR_HEADER, Alignment.LMID, PANEL_WIDTH - MARGIN * 2 - BUTTON_WIDTH, HEADER_HEIGHT, MARGIN, MARGIN);

        TooltipMakerAPI backTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH + 20f, HEADER_HEIGHT, 
            PANEL_WIDTH - BUTTON_WIDTH - MARGIN - 20f, MARGIN);
        ButtonAPI backButton = backTp.addButton(Strings.get("menu.back"), ACTION_BACK, BUTTON_WIDTH, HEADER_HEIGHT - 5f, 0f);
        backButton.setQuickMode(true);
        backButton.getPosition().inTL(0, 0);
    }

    private void createSelectionBarLabels() {
        float barY = MARGIN + HEADER_HEIGHT + 10f;

        selectedNameLabel = UIComponentFactory.createLabelSmall(panel, "", 
            COLOR_SELECTED, Alignment.LMID, PANEL_WIDTH - MARGIN * 2 - BUTTON_WIDTH - 100f, SELECTION_BAR_HEIGHT, MARGIN, barY);

        prismaticLabel = UIComponentFactory.createLabelSmall(panel, "", 
            ColorHelper.PRISMATIC_GOLD, Alignment.LMID, 200f, SELECTION_BAR_HEIGHT, MARGIN + 250f, barY + 5f);
    }

    private void createInfoPanelLabels() {
        float infoPanelStartY = MARGIN + HEADER_HEIGHT + SELECTION_BAR_HEIGHT + 20f;

        float passiveY = infoPanelStartY + 40f;
        passiveLabel = UIComponentFactory.createLabelSmall(panel, "", 
            COLOR_TEXT, Alignment.LMID, INFO_PANEL_WIDTH - 20f, 60f, INFO_PANEL_X + 10f, passiveY);

        float prismaticEffectY = passiveY + 80f;
        prismaticEffectLabel = UIComponentFactory.createLabelSmall(panel, "", 
            ColorHelper.PRISMATIC_GOLD, Alignment.LMID, INFO_PANEL_WIDTH - 20f, 60f, INFO_PANEL_X + 10f, prismaticEffectY);
    }

    private void createButtons() {
        if (panel == null || buttonsCreated) return;

        float buttonAreaY = PANEL_HEIGHT - BUTTON_AREA_HEIGHT;

        TooltipMakerAPI btnTp = panel.createUIElement(PANEL_WIDTH, BUTTON_AREA_HEIGHT, false);
        btnTp.setActionListenerDelegate(this);
        panel.addUIElement(btnTp).inTL(0, buttonAreaY);

        ButtonAPI changeDiceButton = btnTp.addButton(Strings.get("setup.change_dice"), ACTION_CHANGE_DICE, 120f, BUTTON_HEIGHT, 0f);
        changeDiceButton.getPosition().inTL(80f, 5f);
        changeDiceButton.setQuickMode(true);

        ButtonAPI confirmButton = btnTp.addButton(Strings.get("setup.confirm"), ACTION_CONFIRM, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        confirmButton.getPosition().inTL(420f, 5f);
        confirmButton.setQuickMode(true);

        ButtonAPI cancelButton = btnTp.addButton(Strings.get("setup.cancel"), ACTION_CANCEL, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        cancelButton.getPosition().inTL(760f, 5f);
        cancelButton.setQuickMode(true);

        buttonsCreated = true;
    }

    private void updateLabels() {
        if (selectedIndex < 0 || selectedIndex >= characters.size()) {
            selectedNameLabel.setText(Strings.get("setup.no_selection"));
            prismaticLabel.setText("");
            passiveLabel.setText("");
            prismaticEffectLabel.setText("");
            return;
        }

        CharacterCard card = characters.get(selectedIndex);
        selectedNameLabel.setText(Strings.format("setup.selected", card.getName()));

        Map<String, Integer> prismaticDice = card.getPrismaticDiceIds();
        if (prismaticDice != null && !prismaticDice.isEmpty()) {
            if (selectedPrismaticDiceId == null || !prismaticDice.containsKey(selectedPrismaticDiceId)) {
                selectedPrismaticDiceId = prismaticDice.keySet().iterator().next();
            }
            int uses = prismaticDice.getOrDefault(selectedPrismaticDiceId, 0);
            String diceName = formatPrismaticName(selectedPrismaticDiceId);
            prismaticLabel.setText(Strings.format("setup.prismatic", diceName + " (" + uses + ")"));
            
            String effectDesc = getPrismaticEffectDescription(selectedPrismaticDiceId);
            prismaticEffectLabel.setText(effectDesc);
        } else {
            prismaticLabel.setText(Strings.get("setup.prismatic_na"));
            selectedPrismaticDiceId = null;
            prismaticEffectLabel.setText("");
        }

        String passive = card.getPassiveDescription();
        if (passive != null && !passive.isEmpty()) {
            passiveLabel.setText(Strings.format("setup.passive", truncatePassive(passive)));
        } else {
            passiveLabel.setText(Strings.get("setup.passive_none"));
        }
    }

    private String formatPrismaticName(String id) {
        try {
            return Strings.get("prismatic." + id + ".name");
        } catch (Exception e) {
            return id.replace("_", " ");
        }
    }

    private String truncatePassive(String text) {
        int maxLen = 80;
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private String getPrismaticEffectDescription(String diceId) {
        if (diceId == null || diceId.isEmpty()) return "";
        String key = "prismatic." + diceId + ".description";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return "";
        }
    }

    public void renderBelow(float alphaMult) {
        PositionAPI pos = panel.getPosition();
        float panelX = pos.getX();
        float panelY = pos.getY();

        UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT));

        Misc.renderQuad(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BG_DARK, alphaMult);

        float galleryStartY = MARGIN + HEADER_HEIGHT + SELECTION_BAR_HEIGHT + 15f;
        float galleryHeight = PANEL_HEIGHT - galleryStartY - BUTTON_AREA_HEIGHT - 20f;
        float galleryWidth = INFO_PANEL_X - MARGIN - 10f;

        UnifiedCoord galleryBgPos = new UnifiedCoord(MARGIN - 5f, galleryStartY - 5f);
        Misc.renderQuad(galleryBgPos.glX(), galleryBgPos.glSpriteY(galleryHeight + 10f),
            galleryWidth + 10f, galleryHeight + 10f,
            new Color(35, 40, 50, 180), alphaMult * 0.7f);

        renderCardBoxes(galleryStartY, alphaMult);
        renderInfoPanel(alphaMult);

        UnifiedCoord.clearCurrent();

        updateLabels();
    }

    private void renderCardBoxes(float startY, float alphaMult) {
        clickRegions.clear();

        for (int i = 0; i < characters.size(); i++) {
            GLStateUtil.resetBlendState();

            int col = i % COLS;
            int row = i / COLS;
            float boxX = MARGIN + col * (CARD_WIDTH + GAP_X);
            float boxY = startY + row * (CARD_HEIGHT + GAP_Y);

            clickRegions.add(new ClickRegion(boxX, boxY, CARD_WIDTH, CARD_HEIGHT, i));

            UnifiedCoord cardPos = new UnifiedCoord(boxX, boxY);
            float cardGlX = cardPos.glX();
            float cardGlY = cardPos.glSpriteY(CARD_HEIGHT);

            boolean isSelected = (i == selectedIndex);
            Color borderColor = isSelected ? COLOR_SELECTED : COLOR_UNSELECTED;

            Misc.renderQuad(cardGlX, cardGlY, CARD_WIDTH, CARD_HEIGHT, COLOR_BOX_BG, alphaMult * 0.9f);

            drawBorder(cardGlX, cardGlY, borderColor, alphaMult, isSelected);

            GLStateUtil.enableTexturing();

            CharacterCard card = characters.get(i);
            SpriteAPI portrait = CosmiconSprites.getPortrait(card.getId());
            if (portrait != null) {
                float portraitW = CARD_WIDTH * 0.95f;
                float portraitH = CARD_HEIGHT * 0.95f;
                float centerX = cardGlX + CARD_WIDTH / 2f;
                float centerY = cardPos.glCenterY(CARD_HEIGHT / 2f);
                portrait.setSize(portraitW, portraitH);
                portrait.setAlphaMult(alphaMult * (isSelected ? 1f : 0.85f));
                portrait.renderAtCenter(centerX, centerY);
            }

            renderDicePoolIcons(cardGlX, cardGlY, card, alphaMult);

            GLStateUtil.disableTexturing();
        }

        GL11.glLineWidth(1f);
    }

    private void drawBorder(float x, float y, Color color, float alphaMult, boolean isSelected) {
        float[] c = ColorHelper.toGLComponents(color, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(isSelected ? 3f : 1.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + CARD_WIDTH, y);
        GL11.glVertex2f(x + CARD_WIDTH, y + CARD_HEIGHT);
        GL11.glVertex2f(x, y + CARD_HEIGHT);
        GL11.glEnd();
        GLStateUtil.resetColor();
    }

    private void renderDicePoolIcons(float cardGlX, float cardGlY, CharacterCard card, float alphaMult) {
        float iconSize = 16f;
        float iconSpacing = 20f;
        float rightMargin = 8f;
        float topMargin = 8f;

        DicePoolCounts counts = DicePoolCounts.fromPool(card.getDicePool());

        float startX = cardGlX + CARD_WIDTH - rightMargin - iconSize;
        float startY = cardGlY + CARD_HEIGHT - topMargin - iconSize;

        List<DiceType> order = java.util.Arrays.asList(
            DiceType.ORANGE_D8, DiceType.PURPLE_D6, DiceType.BLUE_D4, DiceType.PRISMATIC
        );

        GLStateUtil.enableTexturingWithBlend();

        float offsetY = 0f;
        for (DiceType type : order) {
            SpriteAPI icon = CosmiconSprites.getDiceIcon(type);
            if (icon != null) {
                icon.setSize(iconSize, iconSize);
                icon.setAlphaMult(alphaMult);
                icon.render(startX, startY - offsetY);
            }
            offsetY += iconSpacing;
        }

        GLStateUtil.disableTexturing();
    }

    private void renderInfoPanel(float alphaMult) {
        float infoPanelStartY = MARGIN + HEADER_HEIGHT + SELECTION_BAR_HEIGHT + 20f;
        float borderH = PANEL_HEIGHT - infoPanelStartY - BUTTON_AREA_HEIGHT - 20f;

        GLStateUtil.resetBlendState();
        UnifiedCoord infoPos = new UnifiedCoord(INFO_PANEL_X, infoPanelStartY);
        float glY = infoPos.glSpriteY(borderH);
        Misc.renderQuad(infoPos.glX(), glY,
            INFO_PANEL_WIDTH, borderH,
            COLOR_INFO_PANEL_BG, alphaMult);

        float borderX = infoPos.glX();
        float borderW = INFO_PANEL_WIDTH;
        float[] c = ColorHelper.toGLComponents(COLOR_SECTION_HEADER, alphaMult * 0.8f);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(1.5f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(borderX, glY);
        GL11.glVertex2f(borderX + borderW, glY);
        GL11.glVertex2f(borderX + borderW, glY + borderH);
        GL11.glVertex2f(borderX, glY + borderH);
        GL11.glEnd();
        GLStateUtil.resetColor();
    }

    public void processInput(List<InputEventAPI> events) {
        boolean mouseDown = Mouse.isButtonDown(0);

        if (mouseDown && !wasMousePressed) {
            PositionAPI pos = panel.getPosition();
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(
                pos.getX(), pos.getY(), PANEL_WIDTH, PANEL_HEIGHT));
            
            UnifiedCoord mousePos = UnifiedCoord.fromMouse();

            for (ClickRegion region : clickRegions) {
                if (mousePos.isInsideRect(region.boxX, region.boxY, region.width, region.height)) {
                    handleSelection(region.index);
                    Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 0.6f);
                    break;
                }
            }

            UnifiedCoord.clearCurrent();
        }

        wasMousePressed = mouseDown;
    }

    private void handleSelection(int index) {
        if (index < 0 || index >= characters.size()) return;

        selectedIndex = index;
        CharacterCard card = characters.get(index);

        Map<String, Integer> prismaticDice = card.getPrismaticDiceIds();
        if (prismaticDice != null && !prismaticDice.isEmpty()) {
            selectedPrismaticDiceId = prismaticDice.keySet().iterator().next();
        } else {
            selectedPrismaticDiceId = null;
        }

        updateLabels();
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
                    if (callback != null) {
                        callback.onConfirm(card.getId(), selectedPrismaticDiceId);
                    }
                    callbacks.dismissDialog();
                }
            }
            case ACTION_CANCEL, ACTION_BACK -> {
                if (callback != null) {
                    callback.onCancel();
                }
                callbacks.dismissDialog();
            }
            case ACTION_CHANGE_DICE -> showPrismaticPopup();
        }
    }

    public void showPrismaticPopup() {
        if (selectedIndex < 0 || selectedIndex >= characters.size()) return;

        CharacterCard card = characters.get(selectedIndex);
        Map<String, Integer> prismaticDice = card.getPrismaticDiceIds();
        if (prismaticDice == null || prismaticDice.isEmpty()) return;

        String charId = card.getId();
        new PrismaticEquipPopup(charId, selectedPrismaticDiceId, new PrismaticEquipPopup.PrismaticEquipCallback() {
            @Override
            public void onDiceSelected(String selectedId) {
                selectedPrismaticDiceId = selectedId;
                closePrismaticPopup();
                updateLabels();
            }

            @Override
            public void onPopupClosed() {
                closePrismaticPopup();
            }
        });
    }

    public void closePrismaticPopup() {
        popupPanel = null;
    }

    public void setSelection(String charId, String diceId) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i).getId().equals(charId)) {
                selectedIndex = i;
                break;
            }
        }
        selectedPrismaticDiceId = diceId;
        updateLabels();
    }

    public void setDefaultSelection() {
        if (!characters.isEmpty()) {
            selectedIndex = 0;
            CharacterCard firstCard = characters.get(0);
            selectedPrismaticDiceId = CosmiconPlayerState.getDefaultPrismaticForCharacter(firstCard.getId());
            updateLabels();
        }
    }

    public void cleanup() {
        if (popupPanel != null && panel != null) {
            panel.removeComponent(popupPanel);
            popupPanel = null;
        }
    }
}
package data.scripts.cosmicon.setup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.PopupRenderer;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import data.scripts.cosmicon.util.UIComponentFactory;

public class PrismaticEquipPopup extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final float POPUP_WIDTH = 350f;
    private static final float POPUP_HEIGHT = 450f;
    private static final float MARGIN = 15f;
    private static final float DICE_ENTRY_HEIGHT = 24f;
    private static final float RADIO_SIZE = 16f;
    private static final float BUTTON_WIDTH = 100f;
    private static final float BUTTON_HEIGHT = 24f;

    private static final Color COLOR_BG = new Color(40, 35, 25, 240);
    private static final Color COLOR_RADIO_SELECTED = new Color(255, 215, 0);
    private static final Color COLOR_RADIO_UNSELECTED = new Color(100, 100, 100);
    private static final Color COLOR_SECTION_HEADER = new Color(180, 180, 200);
    private static final Color COLOR_ROBIN_BLOCKED = new Color(255, 100, 100);

    private static final String ACTION_CONFIRM = "prismatic_equip_confirm";
    private static final String ACTION_CLOSE = "prismatic_equip_close";

    private final String currentDiceId;
    private String selectedDiceId;
    private final boolean isBlockedForRobin;

    private CustomPanelAPI panel;
    private final List<PrismaticDiceType> diceTypes;
    private LabelAPI effectPreviewLabel;

    private final List<ClickRegion> diceClickRegions = new ArrayList<>();
    private boolean wasMousePressed = false;

    private float panelX;
    private float panelY;

    public interface PrismaticEquipCallback {
        void onDiceSelected(String diceId);
        void onPopupClosed();
    }

    private final PrismaticEquipCallback callback;

    private record ClickRegion(float x, float y, float width, float height, String diceId) {}

    public PrismaticEquipPopup(String characterId, String currentDiceId, PrismaticEquipCallback callback) {
        this.currentDiceId = currentDiceId;
        this.selectedDiceId = currentDiceId;
        this.callback = callback;
        this.diceTypes = loadAllDiceTypes();
        this.isBlockedForRobin = CharacterIds.ROBIN.equals(characterId);
    }

    private List<PrismaticDiceType> loadAllDiceTypes() {
        Map<String, PrismaticDiceType> all = PrismaticDiceRegistry.getAll();
        return new ArrayList<>(all.values());
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();

        createUIElements();
        updateEffectPreview();
    }

    private void createUIElements() {
        UIComponentFactory.createLabelLarge(panel, Strings.get("prismatic.equip.title"),
                ColorHelper.PRISMATIC_GOLD, Alignment.MID, POPUP_WIDTH - MARGIN * 2 - BUTTON_WIDTH, 28f, MARGIN, MARGIN);

        TooltipMakerAPI closeTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH, BUTTON_HEIGHT, 
            POPUP_WIDTH - BUTTON_WIDTH - MARGIN, MARGIN);
        ButtonAPI closeButton = closeTp.addButton(Strings.get("prismatic.equip.close"), ACTION_CLOSE, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        closeButton.setQuickMode(true);

        String currentName = PrismaticDisplayHelper.getDiceDisplayName(currentDiceId);
        UIComponentFactory.createLabelSmall(panel, Strings.format("prismatic.equip.current", currentName),
                ColorHelper.PRISMATIC_BRIGHT, Alignment.LMID, POPUP_WIDTH - MARGIN * 2, 20f, MARGIN, MARGIN + 35f);

        LabelAPI availableLabel = UIComponentFactory.createLabelSmall(panel, Strings.get("prismatic.equip.available"),
                COLOR_SECTION_HEADER, Alignment.LMID, POPUP_WIDTH - MARGIN * 2, 20f, MARGIN, MARGIN + 60f);

        if (isBlockedForRobin) {
            UIComponentFactory.createLabelSmall(panel, Strings.get("prismatic.equip.robin_blocked"),
                    COLOR_ROBIN_BLOCKED, Alignment.MID, POPUP_WIDTH - MARGIN * 2, 20f, MARGIN, MARGIN + 85f);
            availableLabel.setOpacity(0f);
        } else {
            createDiceRadioList();
        }

        float effectY = isBlockedForRobin ? MARGIN + 110f : MARGIN + 85f + diceTypes.size() * DICE_ENTRY_HEIGHT + 10f;
        UIComponentFactory.createLabelSmall(panel, Strings.get("prismatic.equip.effect_preview"),
                COLOR_SECTION_HEADER, Alignment.LMID, POPUP_WIDTH - MARGIN * 2, 20f, MARGIN, effectY);

        effectPreviewLabel = UIComponentFactory.createLabelSmall(panel, "", 
            Color.LIGHT_GRAY, Alignment.LMID, POPUP_WIDTH - MARGIN * 2, 60f, MARGIN, effectY + 20f);

        float buttonY = POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN;
        TooltipMakerAPI confirmTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH, BUTTON_HEIGHT, 
            POPUP_WIDTH / 2f - BUTTON_WIDTH / 2f, buttonY);
        ButtonAPI confirmButton = confirmTp.addButton(Strings.get("prismatic.equip.confirm"), ACTION_CONFIRM, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        confirmButton.setQuickMode(true);

        if (isBlockedForRobin) {
            confirmButton.setEnabled(false);
            confirmButton.setOpacity(0.5f);
        }
    }

    private void createDiceRadioList() {
        float startY = MARGIN + 85f;

        for (int i = 0; i < diceTypes.size(); i++) {
            PrismaticDiceType type = diceTypes.get(i);
            String diceId = type.getId();
            float y = startY + i * DICE_ENTRY_HEIGHT;

            diceClickRegions.add(new ClickRegion(MARGIN, y, POPUP_WIDTH - MARGIN * 2, DICE_ENTRY_HEIGHT, diceId));

            Color labelColor = selectedDiceId.equals(diceId) ? ColorHelper.PRISMATIC_BRIGHT : Color.WHITE;
            UIComponentFactory.createLabelSmall(panel, PrismaticDisplayHelper.getDiceDisplayName(diceId), 
                labelColor, Alignment.LMID, POPUP_WIDTH - MARGIN * 2 - RADIO_SIZE - 5f, DICE_ENTRY_HEIGHT, 
                MARGIN + RADIO_SIZE + 5f, y);
        }
    }

    private String getDiceEffectDescription(String diceId) {
        return PrismaticDisplayHelper.getEffectDescriptionForDiceId(diceId);
    }

    private void updateEffectPreview() {
        if (isBlockedForRobin) {
            effectPreviewLabel.setText("");
            return;
        }
        String desc = getDiceEffectDescription(selectedDiceId);
        effectPreviewLabel.setText(desc);
    }

    @Override
    public void advance(float amount) {
        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (isBlockedForRobin) return;

        boolean mouseDown = Mouse.isButtonDown(0);

        if (mouseDown && !wasMousePressed) {
            float scale = Global.getSettings().getScreenScaleMult();
            float mouseX = Mouse.getX() / scale;
            float mouseY = Mouse.getY() / scale;

            for (ClickRegion region : diceClickRegions) {
                float screenX = panelX + region.x;
                float screenY = panelY + POPUP_HEIGHT - region.y - region.height;

                if (mouseX >= screenX && mouseX <= screenX + region.width &&
                    mouseY >= screenY && mouseY <= screenY + region.height) {
                    selectedDiceId = region.diceId;
                    updateEffectPreview();
                    Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 0.5f);
                    break;
                }
            }
        }

        wasMousePressed = mouseDown;
    }

    @Override
    public void renderBelow(float alphaMult) {
        PopupRenderer.drawPopupBackground(panelX, panelY, POPUP_WIDTH, POPUP_HEIGHT, COLOR_BG, ColorHelper.PRISMATIC_GOLD, alphaMult);

        if (!isBlockedForRobin) {
            renderRadioButtons(alphaMult);
        }
    }

    private void renderRadioButtons(float alphaMult) {
        float startY = MARGIN + 85f;

        for (int i = 0; i < diceTypes.size(); i++) {
            String diceId = diceTypes.get(i).getId();
            float y = startY + i * DICE_ENTRY_HEIGHT;

            float screenY = panelY + POPUP_HEIGHT - y - RADIO_SIZE;

            boolean isSelected = selectedDiceId.equals(diceId);
            Color radioColor = isSelected ? COLOR_RADIO_SELECTED : COLOR_RADIO_UNSELECTED;

            float radioX = panelX + MARGIN;
            float radioCenterX = radioX + RADIO_SIZE / 2f;
            float radioCenterY = screenY + RADIO_SIZE / 2f;
            float outerRadius = RADIO_SIZE / 2f;
            float innerRadius = outerRadius * 0.4f;

            float[] outer = ColorHelper.toGLComponents(radioColor, alphaMult);
            GL11.glColor4f(outer[0], outer[1], outer[2], outer[3]);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int angle = 0; angle < 360; angle += 10) {
                double rad = Math.toRadians(angle);
                float px = radioCenterX + (float) Math.cos(rad) * outerRadius;
                float py = radioCenterY + (float) Math.sin(rad) * outerRadius;
                GL11.glVertex2f(px, py);
            }
            GL11.glEnd();

            if (isSelected) {
                float[] inner = ColorHelper.toGLComponents(COLOR_RADIO_SELECTED, alphaMult);
                GL11.glColor4f(inner[0], inner[1], inner[2], inner[3]);
                GL11.glBegin(GL11.GL_POLYGON);
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    float px = radioCenterX + (float) Math.cos(rad) * innerRadius;
                    float py = radioCenterY + (float) Math.sin(rad) * innerRadius;
                    GL11.glVertex2f(px, py);
                }
                GL11.glEnd();
            }
        }
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            Object data = btn.getCustomData();
            if (data == null) return;

            String action = data.toString();

            if (action.equals(ACTION_CLOSE)) {
                if (callback != null) {
                    callback.onPopupClosed();
                }
            } else if (action.equals(ACTION_CONFIRM)) {
                if (!isBlockedForRobin && callback != null) {
                    callback.onDiceSelected(selectedDiceId);
                }
            }
        }
    }
}
package data.scripts.cosmicon.setup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class PrismaticEquipPopup extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final SettingsAPI settings = Global.getSettings();
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

    private String currentDiceId;
    private String selectedDiceId;
    private String characterId;
    private boolean isBlockedForRobin;

    private CustomPanelAPI panel;
    private final List<PrismaticDiceType> diceTypes;
    private LabelAPI titleLabel;
    private LabelAPI currentLabel;
    private LabelAPI availableLabel;
    private LabelAPI effectPreviewHeader;
    private LabelAPI effectPreviewLabel;
    private LabelAPI robinBlockedLabel;
    private ButtonAPI confirmButton;
    private ButtonAPI closeButton;

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
        this.characterId = characterId;
        this.currentDiceId = currentDiceId;
        this.selectedDiceId = currentDiceId;
        this.callback = callback;
        this.diceTypes = loadAllDiceTypes();
        this.isBlockedForRobin = "robin".equals(characterId);
    }

    private List<PrismaticDiceType> loadAllDiceTypes() {
        List<PrismaticDiceType> types = new ArrayList<>();
        Map<String, PrismaticDiceType> all = PrismaticDiceRegistry.getAll();
        for (PrismaticDiceType type : all.values()) {
            types.add(type);
        }
        return types;
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
        titleLabel = settings.createLabel(Strings.get("prismatic.equip.title"), Fonts.INSIGNIA_LARGE);
        titleLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        titleLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) titleLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2 - BUTTON_WIDTH, 28f)
            .inTL(MARGIN, MARGIN);

        TooltipMakerAPI closeTp = panel.createUIElement(BUTTON_WIDTH, BUTTON_HEIGHT, false);
        closeTp.setActionListenerDelegate(this);
        panel.addUIElement(closeTp).inTL(POPUP_WIDTH - BUTTON_WIDTH - MARGIN, MARGIN);
        closeButton = closeTp.addButton(Strings.get("prismatic.equip.close"), ACTION_CLOSE, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        closeButton.setQuickMode(true);

        String currentName = getDiceDisplayName(currentDiceId);
        currentLabel = settings.createLabel(Strings.format("prismatic.equip.current", currentName), Fonts.DEFAULT_SMALL);
        currentLabel.setColor(ColorHelper.PRISMATIC_BRIGHT);
        currentLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) currentLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
            .inTL(MARGIN, MARGIN + 35f);

        availableLabel = settings.createLabel(Strings.get("prismatic.equip.available"), Fonts.DEFAULT_SMALL);
        availableLabel.setColor(COLOR_SECTION_HEADER);
        availableLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) availableLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
            .inTL(MARGIN, MARGIN + 60f);

        if (isBlockedForRobin) {
            robinBlockedLabel = settings.createLabel(Strings.get("prismatic.equip.robin_blocked"), Fonts.DEFAULT_SMALL);
            robinBlockedLabel.setColor(COLOR_ROBIN_BLOCKED);
            robinBlockedLabel.setAlignment(Alignment.MID);
            panel.addComponent((UIComponentAPI) robinBlockedLabel)
                .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
                .inTL(MARGIN, MARGIN + 85f);
            availableLabel.setOpacity(0f);
        } else {
            createDiceRadioList();
        }

        float effectY = isBlockedForRobin ? MARGIN + 110f : MARGIN + 85f + diceTypes.size() * DICE_ENTRY_HEIGHT + 10f;
        effectPreviewHeader = settings.createLabel(Strings.get("prismatic.equip.effect_preview"), Fonts.DEFAULT_SMALL);
        effectPreviewHeader.setColor(COLOR_SECTION_HEADER);
        effectPreviewHeader.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) effectPreviewHeader)
            .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
            .inTL(MARGIN, effectY);

        effectPreviewLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        effectPreviewLabel.setColor(Color.LIGHT_GRAY);
        effectPreviewLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) effectPreviewLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 60f)
            .inTL(MARGIN, effectY + 20f);

        float buttonY = POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN;
        TooltipMakerAPI confirmTp = panel.createUIElement(BUTTON_WIDTH, BUTTON_HEIGHT, false);
        confirmTp.setActionListenerDelegate(this);
        panel.addUIElement(confirmTp).inTL(POPUP_WIDTH / 2f - BUTTON_WIDTH / 2f, buttonY);
        confirmButton = confirmTp.addButton(Strings.get("prismatic.equip.confirm"), ACTION_CONFIRM, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
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

            LabelAPI diceLabel = settings.createLabel(getDiceDisplayName(diceId), Fonts.DEFAULT_SMALL);
            diceLabel.setColor(selectedDiceId.equals(diceId) ? ColorHelper.PRISMATIC_BRIGHT : Color.WHITE);
            diceLabel.setAlignment(Alignment.LMID);
            panel.addComponent((UIComponentAPI) diceLabel)
                .setSize(POPUP_WIDTH - MARGIN * 2 - RADIO_SIZE - 5f, DICE_ENTRY_HEIGHT)
                .inTL(MARGIN + RADIO_SIZE + 5f, y);
        }
    }

    private String getDiceDisplayName(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            return Strings.get("prismatic.equip.none");
        }
        String key = "prismatic." + diceId + ".name";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return diceId;
        }
    }

    private String getDiceEffectDescription(String diceId) {
        if (diceId == null || diceId.isEmpty()) {
            return Strings.get("prismatic.equip.none_effect");
        }
        String key = "prismatic." + diceId + ".description";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            PrismaticDiceType type = PrismaticDiceRegistry.get(diceId);
            if (type != null) {
                return getEffectDescriptionFromType(type);
            }
            return Strings.get("prismatic.equip.no_effect");
        }
    }

    private String getEffectDescriptionFromType(PrismaticDiceType type) {
        PrismaticEffect effect = type.getEffect();
        if (effect.isNone()) return Strings.get("prismatic.equip.no_effect");
        if (effect.isDoubleValue()) return Strings.get("prismatic.equip.effect_double");
        if (effect.isHealHp()) return Strings.get("prismatic.equip.effect_heal");
        if (effect.isGainPrismaticUse()) return Strings.get("prismatic.equip.effect_gain_use");
        if (effect.isInstantDamage()) return Strings.format("prismatic.equip.effect_instant_damage", effect.getInstantDamageAmount());
        if (effect.isGrantStatus()) {
            String statusKey = "status." + effect.getGrantedEffect().name().toLowerCase();
            try {
                String statusName = Strings.get(statusKey);
                return Strings.format("prismatic.equip.effect_status", statusName);
            } catch (Exception e) {
                return Strings.format("prismatic.equip.effect_status", effect.getGrantedEffect().name());
            }
        }
        return Strings.get("prismatic.equip.no_effect");
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
            float scale = settings.getScreenScaleMult();
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
        GLStateUtil.resetBlendState();

        float x = panelX;
        float y = panelY;

        float[] bg = ColorHelper.toGLComponents(COLOR_BG, alphaMult);
        GL11.glColor4f(bg[0], bg[1], bg[2], bg[3]);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + POPUP_WIDTH, y);
        GL11.glVertex2f(x + POPUP_WIDTH, y + POPUP_HEIGHT);
        GL11.glVertex2f(x, y + POPUP_HEIGHT);
        GL11.glEnd();

        float[] border = ColorHelper.toGLComponents(ColorHelper.PRISMATIC_GOLD, alphaMult * 0.8f);
        GL11.glColor4f(border[0], border[1], border[2], border[3]);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + POPUP_WIDTH, y);
        GL11.glVertex2f(x + POPUP_WIDTH, y + POPUP_HEIGHT);
        GL11.glVertex2f(x, y + POPUP_HEIGHT);
        GL11.glEnd();

        if (!isBlockedForRobin) {
            renderRadioButtons(alphaMult);
        }

        GLStateUtil.resetColor();
        GL11.glLineWidth(1f);
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
    public void render(float alphaMult) {
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
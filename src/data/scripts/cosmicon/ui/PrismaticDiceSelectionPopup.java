package data.scripts.cosmicon.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
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
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class PrismaticDiceSelectionPopup extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final SettingsAPI settings = Global.getSettings();
    private static final float POPUP_WIDTH = 400f;
    private static final float POPUP_HEIGHT = 300f;
    private static final float DICE_ENTRY_HEIGHT = 60f;
    private static final float BUTTON_WIDTH = 80f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float MARGIN = 15f;

    private CustomPanelAPI panel;
    private final BattleState battleState;
    private final PrismaticDiceSelectionCallback selectionCallback;

    private LabelAPI titleLabel;
    private LabelAPI noDiceLabel;
    private final List<DiceEntry> diceEntries;
    private ButtonAPI closeButton;

    private LabelAPI confirmTitleLabel;
    private LabelAPI confirmEffectLabel;
    private LabelAPI warningLabel;
    private ButtonAPI confirmButton;
    private ButtonAPI cancelButton;

    private PrismaticDiceType selectedType;
    private PrismaticDiceInstance rolledInstance;
    private boolean showingConfirmation;

    private float panelX;
    private float panelY;

    public interface PrismaticDiceSelectionCallback {
        void onPrismaticDiceSelected(PrismaticDiceType type, PrismaticDiceInstance instance);
        void onPopupClosed();
    }

    private static class DiceEntry {
        PrismaticDiceType type;
        int uses;
        LabelAPI nameLabel;
        LabelAPI effectLabel;
        LabelAPI usesLabel;
        ButtonAPI rollButton;
    }

    public PrismaticDiceSelectionPopup(BattleState battleState, PrismaticDiceSelectionCallback callback) {
        this.battleState = battleState;
        this.selectionCallback = callback;
        this.diceEntries = new ArrayList<>();
        this.showingConfirmation = false;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();

        createUIElements();
        populateDiceList();
        updateVisibility();
    }

    private void createUIElements() {
        titleLabel = settings.createLabel(Strings.get("prismatic.popup.title"), Fonts.INSIGNIA_LARGE);
        titleLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        titleLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) titleLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 30f)
            .inTL(MARGIN, MARGIN);

        noDiceLabel = settings.createLabel(Strings.get("prismatic.popup.no_dice_available"), Fonts.DEFAULT_SMALL);
        noDiceLabel.setColor(Color.GRAY);
        noDiceLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) noDiceLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
            .inTL(MARGIN, POPUP_HEIGHT / 2f - 10f);
        noDiceLabel.setOpacity(0f);

        TooltipMakerAPI closeTp = panel.createUIElement(BUTTON_WIDTH, BUTTON_HEIGHT, false);
        closeTp.setActionListenerDelegate(this);
        panel.addUIElement(closeTp).inTL(POPUP_WIDTH - BUTTON_WIDTH - MARGIN, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
        closeButton = closeTp.addButton(Strings.get("prismatic.popup.close"), "close", BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        closeButton.setQuickMode(true);

        confirmTitleLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        confirmTitleLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        confirmTitleLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) confirmTitleLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 30f)
            .inTL(MARGIN, POPUP_HEIGHT / 2f - 60f);
        confirmTitleLabel.setOpacity(0f);

        confirmEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        confirmEffectLabel.setColor(Color.WHITE);
        confirmEffectLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) confirmEffectLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 40f)
            .inTL(MARGIN, POPUP_HEIGHT / 2f - 20f);
        confirmEffectLabel.setOpacity(0f);

        warningLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        warningLabel.setColor(new Color(255, 100, 100));
        warningLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) warningLabel)
            .setSize(POPUP_WIDTH - MARGIN * 2, 20f)
            .inTL(MARGIN, POPUP_HEIGHT / 2f + 30f);
        warningLabel.setOpacity(0f);

        TooltipMakerAPI confirmTp = panel.createUIElement(BUTTON_WIDTH + 10f, BUTTON_HEIGHT, false);
        confirmTp.setActionListenerDelegate(this);
        panel.addUIElement(confirmTp).inTL(POPUP_WIDTH / 2f - BUTTON_WIDTH - 20f, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
        cancelButton = confirmTp.addButton(Strings.get("prismatic.popup.cancel_button"), "cancel_confirm", BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        cancelButton.setQuickMode(true);
        cancelButton.setOpacity(0f);

        TooltipMakerAPI rollTp = panel.createUIElement(BUTTON_WIDTH + 10f, BUTTON_HEIGHT, false);
        rollTp.setActionListenerDelegate(this);
        panel.addUIElement(rollTp).inTL(POPUP_WIDTH / 2f + 10f, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
        confirmButton = rollTp.addButton(Strings.get("prismatic.popup.confirm_roll"), "confirm_roll", BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        confirmButton.setQuickMode(true);
        confirmButton.setOpacity(0f);
    }

    private void populateDiceList() {
        CharacterCard playerCard = battleState.getPlayerCard();
        if (playerCard == null) return;

        Map<String, Integer> prismaticIds = playerCard.getPrismaticDiceIds();
        PrismaticManager manager = battleState.getPrismaticManager();
        ConditionContext context = createConditionContext();

        float yOffset = MARGIN + 40f;
        TooltipMakerAPI diceTp = panel.createUIElement(POPUP_WIDTH - MARGIN * 2, POPUP_HEIGHT - yOffset - BUTTON_HEIGHT - MARGIN * 2, false);
        diceTp.setActionListenerDelegate(this);
        panel.addUIElement(diceTp).inTL(MARGIN, yOffset);

        for (Map.Entry<String, Integer> entry : prismaticIds.entrySet()) {
            String diceId = entry.getKey();
            PrismaticDiceType type = PrismaticDiceRegistry.get(diceId);
            if (type == null) continue;

            int uses = manager != null ? manager.getUsesByType(type, true) : entry.getValue();
            boolean isAvailable = type.isAvailable(context);

            if (!isAvailable || uses <= 0) continue;

            DiceEntry diceEntry = new DiceEntry();
            diceEntry.type = type;
            diceEntry.uses = uses;

            diceEntry.nameLabel = settings.createLabel(getDiceName(type), Fonts.DEFAULT_SMALL);
            diceEntry.nameLabel.setColor(ColorHelper.PRISMATIC_BRIGHT);
            diceEntry.nameLabel.setAlignment(Alignment.LMID);
            panel.addComponent((UIComponentAPI) diceEntry.nameLabel)
                .setSize(POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 3, 20f)
                .inTL(MARGIN, yOffset);
            diceEntry.nameLabel.setOpacity(1f);

            diceEntry.effectLabel = settings.createLabel(getDiceEffect(type), Fonts.DEFAULT_SMALL);
            diceEntry.effectLabel.setColor(Color.LIGHT_GRAY);
            diceEntry.effectLabel.setAlignment(Alignment.LMID);
            panel.addComponent((UIComponentAPI) diceEntry.effectLabel)
                .setSize(POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 3, 20f)
                .inTL(MARGIN, yOffset + 20f);
            diceEntry.effectLabel.setOpacity(1f);

            diceEntry.usesLabel = settings.createLabel(Strings.format("prismatic.popup.uses", uses), Fonts.DEFAULT_SMALL);
            diceEntry.usesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
            diceEntry.usesLabel.setAlignment(Alignment.RMID);
            panel.addComponent((UIComponentAPI) diceEntry.usesLabel)
                .setSize(60f, 20f)
                .inTL(POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 2 - 70f, yOffset);
            diceEntry.usesLabel.setOpacity(1f);

            diceEntry.rollButton = diceTp.addButton(Strings.get("prismatic.popup.roll_button"), "roll_" + diceId, BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
            diceEntry.rollButton.setQuickMode(true);
            diceEntry.rollButton.getPosition().inTL(POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 2, yOffset + 10f);
            diceEntry.rollButton.setOpacity(1f);

            diceEntries.add(diceEntry);
            yOffset += DICE_ENTRY_HEIGHT;
        }

        if (diceEntries.isEmpty()) {
            noDiceLabel.setOpacity(1f);
        }
    }

    private ConditionContext createConditionContext() {
        int hp = battleState.getPlayerHp();
        CharacterCard card = battleState.getPlayerCard();
        int maxHp = card != null ? card.getMaxHp() : hp;
        int turnNumber = battleState.getTurnNumber();
        BattleState.TurnType turnType = battleState.isPlayerAttacker() ? BattleState.TurnType.ATTACK : BattleState.TurnType.DEFENSE;
        int dmgTaken = battleState.getPlayerTotalDamageTaken();
        Map<Integer, Integer> history = battleState.getPlayerFaceSelectionHistory();

        return new ConditionContext(hp, maxHp, turnNumber, turnType, dmgTaken, history);
    }

    private String getDiceName(PrismaticDiceType type) {
        String key = "prismatic." + type.getId() + ".name";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            return type.getId();
        }
    }

    private String getDiceEffect(PrismaticDiceType type) {
        String key = "prismatic." + type.getId() + ".description";
        try {
            return Strings.get(key);
        } catch (Exception e) {
            PrismaticEffect effect = type.getEffect();
            return getEffectDescription(effect);
        }
    }

    private String getEffectDescription(PrismaticEffect effect) {
        if (effect.isNone()) return Strings.get("prismatic.equip.no_effect");
        if (effect.isDoubleValue()) return Strings.get("prismatic.equip.effect_double");
        if (effect.isHealHp()) return Strings.get("prismatic.equip.effect_heal");
        if (effect.isGainPrismaticUse()) return Strings.get("prismatic.equip.effect_gain_use");
        if (effect.isInstantDamage()) return Strings.format("prismatic.equip.effect_instant_damage", effect.getInstantDamageAmount());
        if (effect.isGrantStatus()) {
            String statusName = effect.getGrantedEffect().name();
            String statusKey = "status." + statusName.toLowerCase();
            try {
                statusName = Strings.get(statusKey);
            } catch (Exception ignored) { }
            return Strings.format("prismatic.equip.effect_status", statusName);
        }
        return Strings.get("prismatic.equip.no_effect");
    }

    private void updateVisibility() {
        boolean showList = !showingConfirmation;
        boolean showConfirm = showingConfirmation;

        titleLabel.setOpacity(showList ? 1f : 0f);
        noDiceLabel.setOpacity(showList && diceEntries.isEmpty() ? 1f : 0f);
        closeButton.setOpacity(showList ? 1f : 0f);

        for (DiceEntry entry : diceEntries) {
            entry.nameLabel.setOpacity(showList ? 1f : 0f);
            entry.effectLabel.setOpacity(showList ? 1f : 0f);
            entry.usesLabel.setOpacity(showList ? 1f : 0f);
            entry.rollButton.setOpacity(showList ? 1f : 0f);
        }

        confirmTitleLabel.setOpacity(showConfirm ? 1f : 0f);
        confirmEffectLabel.setOpacity(showConfirm ? 1f : 0f);
        warningLabel.setOpacity(showConfirm && isRolledFaceSpecial() ? 1f : 0f);
        cancelButton.setOpacity(showConfirm ? 1f : 0f);
        confirmButton.setOpacity(showConfirm ? 1f : 0f);
    }

    private boolean isRolledFaceSpecial() {
        if (selectedType == null || rolledInstance == null) return false;
        return selectedType.isSpecialFace(rolledInstance.faceIndex);
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            String action = (String) btn.getCustomData();

            if (action.equals("close")) {
                if (selectionCallback != null) {
                    selectionCallback.onPopupClosed();
                }
            } else if (action.equals("cancel_confirm")) {
                showingConfirmation = false;
                selectedType = null;
                rolledInstance = null;
                if (selectionCallback != null) {
                    selectionCallback.onPopupClosed();
                }
            } else if (action.equals("confirm_roll")) {
                confirmRoll();
            } else if (action.startsWith("roll_")) {
                String diceId = action.substring(5);
                selectedType = PrismaticDiceRegistry.get(diceId);
                if (selectedType != null) {
                    showConfirmation(selectedType);
                }
            }
        }
    }

    private void showConfirmation(PrismaticDiceType type) {
        showingConfirmation = true;

        Random random = new Random();
        rolledInstance = PrismaticDiceInstance.roll(type, false, random);

        String diceName = getDiceName(type);
        confirmTitleLabel.setText(Strings.format("prismatic.popup.about_to_roll", diceName));

        String effectText = Strings.get("prismatic.popup.effect_header") + " " + getDiceEffect(type);
        confirmEffectLabel.setText(effectText);

        if (isRolledFaceSpecial()) {
            warningLabel.setText(Strings.get("prismatic.popup.warning_destined"));
            warningLabel.setOpacity(1f);
        } else {
            warningLabel.setOpacity(0f);
        }

        updateVisibility();
    }

    private void confirmRoll() {
        if (selectedType == null || rolledInstance == null) return;

        rolledInstance.setMustSelect(isRolledFaceSpecial());

        if (selectionCallback != null) {
            selectionCallback.onPrismaticDiceSelected(selectedType, rolledInstance);
        }
    }

    @Override
    public void advance(float amount) {
        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
    }

    @Override
    public void renderBelow(float alphaMult) {
        GLStateUtil.resetBlendState();

        float x = panelX;
        float y = panelY;

        float[] bg = ColorHelper.toGLComponents(new Color(40, 35, 25, 230), alphaMult);
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

        GLStateUtil.resetColor();
    }
}
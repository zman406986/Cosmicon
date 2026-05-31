package data.scripts.cosmicon.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.TurnState;
import data.scripts.cosmicon.battle.CharacterCard;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.AvailabilityCondition.ConditionContext;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticManager;
import data.scripts.cosmicon.state.CosmiconStats;
import data.scripts.cosmicon.state.CosmiconEventState;
import data.scripts.cosmicon.tutorial.TutorialDiceRoller;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.PopupRenderer;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;
import data.scripts.cosmicon.util.UIComponentFactory;

public class PrismaticDiceSelectionPopup extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final float POPUP_WIDTH = 400f;
    private static final float POPUP_HEIGHT = 300f;
    private static final float DICE_ENTRY_HEIGHT = 60f;
    private static final float BUTTON_WIDTH = 80f;
    private static final float BUTTON_HEIGHT = 24f;
    private static final float MARGIN = 15f;
    private static final Color COLOR_POPUP_BG = new Color(40, 35, 25, 230);

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
        titleLabel = UIComponentFactory.createLabelLarge(panel, Strings.get("prismatic.popup.title"), 
            ColorHelper.PRISMATIC_GOLD, Alignment.MID, POPUP_WIDTH - MARGIN * 2, 30f, MARGIN, MARGIN);

        noDiceLabel = UIComponentFactory.createLabelWithOpacity(panel, Strings.get("prismatic.popup.no_dice_available"), 
            Fonts.DEFAULT_SMALL, Color.GRAY, Alignment.MID, POPUP_WIDTH - MARGIN * 2, 20f, 
            MARGIN, POPUP_HEIGHT / 2f - 10f, 0f);

        TooltipMakerAPI closeTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH, BUTTON_HEIGHT, 
            POPUP_WIDTH - BUTTON_WIDTH - MARGIN, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
        closeButton = closeTp.addButton(Strings.get("prismatic.popup.close"), "close", BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        closeButton.setQuickMode(true);

        confirmTitleLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.INSIGNIA_LARGE, ColorHelper.PRISMATIC_GOLD, Alignment.MID, POPUP_WIDTH - MARGIN * 2, 30f, 
            MARGIN, POPUP_HEIGHT / 2f - 60f, 0f);

        confirmEffectLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, Color.WHITE, Alignment.MID, POPUP_WIDTH - MARGIN * 2, 40f, 
            MARGIN, POPUP_HEIGHT / 2f - 20f, 0f);

        warningLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, new Color(255, 100, 100), Alignment.MID, POPUP_WIDTH - MARGIN * 2, 20f, 
            MARGIN, POPUP_HEIGHT / 2f + 30f, 0f);

        TooltipMakerAPI confirmTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH + 10f, BUTTON_HEIGHT, 
            POPUP_WIDTH / 2f - BUTTON_WIDTH - 20f, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
        cancelButton = confirmTp.addButton(Strings.get("prismatic.popup.cancel_button"), "cancel_confirm", BUTTON_WIDTH, BUTTON_HEIGHT, 0f);
        cancelButton.setQuickMode(true);
        cancelButton.setOpacity(0f);

        TooltipMakerAPI rollTp = UIComponentFactory.createTooltipForButtons(panel, this, BUTTON_WIDTH + 10f, BUTTON_HEIGHT, 
            POPUP_WIDTH / 2f + 10f, POPUP_HEIGHT - BUTTON_HEIGHT - MARGIN);
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

            boolean isTutorial = CosmiconEventState.isTutorialMode()
                || CosmiconStats.isInTutorialMode()
                || CosmiconEventState.isReplayTutorial();
            if (!isTutorial && !CosmiconStats.isPrismaticDiceUnlocked(diceId)) continue;

            int uses = manager != null ? manager.getUsesByType(type, true) : entry.getValue();
            boolean useTrueVersion = playerCard != null && playerCard.isUseTruePrismatic();
            boolean isAvailable = type.isAvailable(context, useTrueVersion);

            if ((!isTutorial && !isAvailable) || uses <= 0) continue;

            DiceEntry diceEntry = new DiceEntry();
            diceEntry.type = type;
            diceEntry.uses = uses;

            diceEntry.nameLabel = UIComponentFactory.createLabelSmall(panel, PrismaticDisplayHelper.getDiceDisplayName(type), 
                ColorHelper.PRISMATIC_BRIGHT, Alignment.LMID, POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 3, 20f, MARGIN, yOffset);

            diceEntry.effectLabel = UIComponentFactory.createLabelSmall(panel, PrismaticDisplayHelper.getEffectDescription(type), 
                Color.LIGHT_GRAY, Alignment.LMID, POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 3, 20f, MARGIN, yOffset + 20f);

            diceEntry.usesLabel = UIComponentFactory.createLabelSmall(panel, Strings.format("prismatic.popup.uses", uses), 
                ColorHelper.PRISMATIC_GOLD, Alignment.RMID, 60f, 20f, POPUP_WIDTH - BUTTON_WIDTH - MARGIN * 2 - 70f, yOffset);

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
        TurnState.TurnType turnType = battleState.isPlayerAttacker() ? TurnState.TurnType.ATTACK : TurnState.TurnType.DEFENSE;
        int dmgTaken = battleState.getPlayerTotalDamageTaken();
        Map<Integer, Integer> history = battleState.getPlayerFaceSelectionHistory();

        return new ConditionContext(hp, maxHp, turnNumber, turnType, dmgTaken, history);
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
        warningLabel.setOpacity(showConfirm && shouldShowDestinedWarning() ? 1f : 0f);
        cancelButton.setOpacity(showConfirm ? 1f : 0f);
        confirmButton.setOpacity(showConfirm ? 1f : 0f);
    }

    private boolean shouldShowDestinedWarning() {
        if (selectedType == null || rolledInstance == null) return false;
        PrismaticEffect effect = selectedType.getEffect();
        boolean grantsDestined = effect.isGrantStatus() && 
                                 effect.getGrantedEffect() == StatusEffect.DESTINED;
        return grantsDestined && rolledInstance.isSpecialFace;
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            String action = (String) btn.getCustomData();

            if (action.equals("close")) {
                selectionCallback.onPopupClosed();
            } else if (action.equals("cancel_confirm")) {
                showingConfirmation = false;
                selectedType = null;
                rolledInstance = null;
                updateVisibility();
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

        boolean useTrueVersion = battleState.getPlayerCard() != null && battleState.getPlayerCard().isUseTruePrismatic();

        TutorialDiceRoller tutorialRoller = battleState.getTutorialDiceRoller();
        if (tutorialRoller != null && tutorialRoller.shouldInterceptPrismaticRoll()) {
            rolledInstance = tutorialRoller.getFixedPrismaticRoll(type, useTrueVersion);
        } else {
            rolledInstance = PrismaticDiceInstance.roll(type, useTrueVersion, new Random());
        }

        String diceName = PrismaticDisplayHelper.getDiceDisplayName(type);
        confirmTitleLabel.setText(Strings.format("prismatic.popup.about_to_roll", diceName));

        String effectText = Strings.get("prismatic.popup.effect_header") + " " + PrismaticDisplayHelper.getEffectDescription(type);
        confirmEffectLabel.setText(effectText);

        if (shouldShowDestinedWarning()) {
            warningLabel.setText(Strings.get("prismatic.popup.warning_destined"));
            warningLabel.setOpacity(1f);
        } else {
            warningLabel.setOpacity(0f);
        }

        updateVisibility();
    }

    private void confirmRoll() {
        if (selectedType == null || rolledInstance == null) return;

        rolledInstance.setMustSelect(shouldShowDestinedWarning());

        selectionCallback.onPrismaticDiceSelected(selectedType, rolledInstance);
    }

    @Override
    public void advance(float amount) {
        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
    }

    @Override
    public void renderBelow(float alphaMult) {
        PopupRenderer.drawPopupBackground(panelX, panelY, POPUP_WIDTH, POPUP_HEIGHT, COLOR_POPUP_BG, ColorHelper.PRISMATIC_GOLD, alphaMult);
    }
}
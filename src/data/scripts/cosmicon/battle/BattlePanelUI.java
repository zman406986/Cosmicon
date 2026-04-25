package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;

import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.Strings;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.battle.BattleState.BattleEventListener;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.battle.BattleState.TurnType;

import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticFaceDisplay;
import data.scripts.cosmicon.ui.PrismaticDiceSelectionPopup;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.GLStateUtil;

public class BattlePanelUI extends BaseCustomUIPanelPlugin implements ActionListenerDelegate, BattleEventListener {

    private static final SettingsAPI settings = Global.getSettings();

    private static final String ACTION_END_TURN = "end_turn";
    private static final String ACTION_CONTINUE = "continue";
    private static final String ACTION_REROLL = "reroll";
    private static final String ACTION_EXIT = "exit";
    private static final String ACTION_PLAYER_ABILITY = "player_ability";
    private static final String ACTION_OPPONENT_ABILITY = "opponent_ability";
    private static final float DICE_SPACING = 130f;
    private static final float DICE_CLICK_PADDING = 5f;
    private static final float PRISMATIC_BTN_SIZE = 40f;
    private static final float PRISMATIC_FACE_MAPPING_OFFSET_X = 55f;
    private static final float PRISMATIC_ROLLED_LABEL_OFFSET_Y = 20f;
    private static final float PASSIVE_BTN_WIDTH = 150f;
    private static final float PASSIVE_BTN_HEIGHT = 25f;

    private CustomPanelAPI panel;
    private DialogCallbacks callbacks;
    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;
    private boolean buttonsCreated = false;

    private ButtonAPI rerollButton;
    private ButtonAPI confirmButton;
    private PrismaticDiceSelectionPopup prismaticPopup;
    private CustomPanelAPI prismaticPopupPanel;
    private boolean prismaticPopupActive;
    private LabelAPI playerPrismaticUsesLabel;
    private LabelAPI playerPrismaticFaceMappingLabel;
    private LabelAPI playerPrismaticEffectLabel;
    private LabelAPI playerPrismaticRolledLabel;
    private LabelAPI opponentPrismaticUsesLabel;
    private LabelAPI opponentPrismaticFaceMappingLabel;
    private LabelAPI opponentPrismaticEffectLabel;
    private LabelAPI phaseLabel;
    private LabelAPI instructionLabel;
    private LabelAPI playerHpLabel;
    private LabelAPI opponentHpLabel;
    private LabelAPI playerNameLabel;
    private LabelAPI opponentNameLabel;
    private LabelAPI resultLabel;
    private LabelAPI playerAtkLabel;
    private LabelAPI playerDefLabel;
    private LabelAPI opponentAtkLabel;
    private LabelAPI opponentDefLabel;
    private LabelAPI playerPrismaticLabel;
    private LabelAPI playerOrangeLabel;
    private LabelAPI playerPurpleLabel;
    private LabelAPI playerBlueLabel;
    private LabelAPI opponentPrismaticLabel;
    private LabelAPI opponentOrangeLabel;
    private LabelAPI opponentPurpleLabel;
    private LabelAPI opponentBlueLabel;
    private LabelAPI clickHintLabel;
    private LabelAPI attackerSelectionLabel;
    private LabelAPI attackerEffectLabel;
    private LabelAPI defenderSelectionLabel;
    private LabelAPI defenderEffectLabel;
    private LabelAPI attackerConfirmedSelectionLabel;
    private LabelAPI attackerConfirmedEffectLabel;
    private LabelAPI attackerIconValueLabel;
    private LabelAPI defenderIconValueLabel;

    private float panelX;
    private float panelY;
    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float rollAnimationDelay;
    private boolean diceAnimating;
    private boolean dicePreviewActive;
    private float dicePreviewDelay;
    private boolean waitingForClickToRoll;
    private float opponentAutoRollDelay;
    private static final float OPPONENT_AUTO_ROLL_DELAY = 0.8f;

    private boolean opponentDiceAnimating;
    private float opponentRollDelay;

    private float roleTransitionProgress;
    private float targetRoleTransition;
    private static final float ROLE_TRANSITION_DURATION = 0.4f;
    private boolean lastPlayerWasAttacker;
    private float cachedRotationAngle;

    private int lastMouseButtonState;

    private float playerPrismaticBtnX;
    private float playerPrismaticBtnY;
    private float opponentPrismaticBtnX;
    private float opponentPrismaticBtnY;
    private PrismaticDiceInstance pendingPrismaticInstance;
    private int pendingPrismaticAnimatorIndex = -1;

    private final List<float[]> diceHitboxes;
    
    private DamageResolutionAnimator damageAnimator;
    private DamageResolver.DamageResult pendingDamageResult;
    private boolean damageAnimationPending;
    
    private ValueChangeAnimator attackerValueAnimator;
    private ValueChangeAnimator defenderValueAnimator;
    private boolean valueAnimationPending;
    
    private float preClashTimer;
    private static final float PRE_CLASH_DELAY = 1.5f;

    public BattlePanelUI() {
        this.diceHitboxes = new ArrayList<>();
        this.lastMouseButtonState = 0;
        this.roleTransitionProgress = 0f;
        this.targetRoleTransition = 0f;
        this.lastPlayerWasAttacker = true;
        this.cachedRotationAngle = 0f;
        this.opponentDiceAnimating = false;
        this.opponentRollDelay = 0f;
        this.dicePreviewActive = false;
        this.dicePreviewDelay = 0f;
        this.waitingForClickToRoll = false;
        this.opponentAutoRollDelay = 0f;
        this.valueAnimationPending = false;
        this.preClashTimer = 0f;
    }

    public void setBattleController(BattleController controller) {
        this.battleController = controller;
        if (controller != null) {
            this.battleState = controller.getState();
            battleState.addListener(this);
        }
    }
    
    public void cleanup() {
        CosmiconLogger.info("Battle dialog closed");
        if (battleState != null) {
            battleState.removeListener(this);
        }
        if (prismaticPopupPanel != null && panel != null) {
            panel.removeComponent(prismaticPopupPanel);
            prismaticPopupPanel = null;
            prismaticPopup = null;
        }
        if (damageAnimator != null) {
            damageAnimator.cleanup();
            damageAnimator = null;
        }
        if (attackerValueAnimator != null) {
            attackerValueAnimator.cleanup();
            attackerValueAnimator = null;
        }
        if (defenderValueAnimator != null) {
            defenderValueAnimator.cleanup();
            defenderValueAnimator = null;
        }
        diceHitboxes.clear();
        diceRollManager = null;
        prismaticPopupActive = false;
        damageAnimationPending = false;
        valueAnimationPending = false;
        pendingDamageResult = null;
        battleState = null;
        battleController = null;
        waitingForClickToRoll = false;
        opponentAutoRollDelay = 0f;
        preClashTimer = 0f;
    }

    

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        CosmiconLogger.info("Battle dialog opened");
        this.panel = panel;
        this.callbacks = callbacks;
        callbacks.getPanelFader().setDurationOut(0.5f);

        this.diceRollManager = new DiceRollManager();
        diceRollManager.init(panel);

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();
        diceZoneCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        diceZoneCenterY = BattleRenderingUtils.PANEL_HEIGHT / 2f - 40f;

        createButtons();
        createLabels();

        if (battleState != null) {
            updateLabelsFromState();
            lastPlayerWasAttacker = battleState.isPlayerAttacker();
            roleTransitionProgress = lastPlayerWasAttacker ? 0f : 1f;
            targetRoleTransition = roleTransitionProgress;
        }
    }

    private void createButtons() {
        if (buttonsCreated) return;

        PositionAPI pos = panel.getPosition();
        TooltipMakerAPI btnTp = panel.createUIElement(pos.getWidth(), pos.getHeight(), false);
        btnTp.setActionListenerDelegate(this);
        panel.addUIElement(btnTp).inBL(0f, 0f);

        float btnWidth = 100f;
        float btnHeight = 30f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomY = BattleRenderingUtils.PANEL_HEIGHT - 60f;

        confirmButton = btnTp.addButton(Strings.get("battle.confirm_attack"), ACTION_END_TURN, 
            btnWidth, btnHeight, 0f);
        confirmButton.setQuickMode(true);
        confirmButton.setShortcut(Keyboard.KEY_SPACE, false);
        confirmButton.getPosition().inTL(centerX - btnWidth - 10f, bottomY);

        rerollButton = btnTp.addButton(Strings.get("phase.reroll_selected"), ACTION_REROLL,
            btnWidth, btnHeight, 0f);
        rerollButton.setQuickMode(true);
        rerollButton.getPosition().inTL(centerX + 10f, bottomY);

        float opponentCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;

        opponentPrismaticBtnX = opponentCardX - 60f;
        opponentPrismaticBtnY = opponentCardY + 40f;

        opponentPrismaticUsesLabel = settings.createLabel("2", Fonts.DEFAULT_SMALL);
        opponentPrismaticUsesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        opponentPrismaticUsesLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) opponentPrismaticUsesLabel)
            .setSize(40, 20)
            .inTL(opponentCardX - 55f, opponentCardY + 85f);

        opponentPrismaticFaceMappingLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        opponentPrismaticFaceMappingLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        opponentPrismaticFaceMappingLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) opponentPrismaticFaceMappingLabel)
            .setSize(180, 20)
            .inTL(opponentPrismaticBtnX + PRISMATIC_FACE_MAPPING_OFFSET_X, opponentPrismaticBtnY);
        opponentPrismaticFaceMappingLabel.setOpacity(0f);

        opponentPrismaticEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        opponentPrismaticEffectLabel.setColor(Color.LIGHT_GRAY);
        opponentPrismaticEffectLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) opponentPrismaticEffectLabel)
            .setSize(180, 20)
            .inTL(opponentPrismaticBtnX + PRISMATIC_FACE_MAPPING_OFFSET_X, opponentPrismaticBtnY + 20f);
        opponentPrismaticEffectLabel.setOpacity(0f);

        float playerCardX = BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;

        playerPrismaticBtnX = BattleRenderingUtils.PANEL_WIDTH - opponentPrismaticBtnX - PRISMATIC_BTN_SIZE;
        playerPrismaticBtnY = BattleRenderingUtils.PANEL_HEIGHT - opponentPrismaticBtnY - PRISMATIC_BTN_SIZE;

        float playerPrismaticUsesX = BattleRenderingUtils.PANEL_WIDTH - (opponentCardX - 55f) - 40f;
        float playerPrismaticUsesY = BattleRenderingUtils.PANEL_HEIGHT - (opponentCardY + 85f) - 20f;

        playerPrismaticUsesLabel = settings.createLabel("2", Fonts.DEFAULT_SMALL);
        playerPrismaticUsesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        playerPrismaticUsesLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) playerPrismaticUsesLabel)
            .setSize(40, 20)
            .inTL(playerPrismaticUsesX, playerPrismaticUsesY);

        float playerFaceMappingX = BattleRenderingUtils.PANEL_WIDTH - (opponentPrismaticBtnX + PRISMATIC_FACE_MAPPING_OFFSET_X) - 180f;
        float playerFaceMappingY = BattleRenderingUtils.PANEL_HEIGHT - opponentPrismaticBtnY - 20f;

        playerPrismaticFaceMappingLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        playerPrismaticFaceMappingLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        playerPrismaticFaceMappingLabel.setAlignment(Alignment.RMID);
        panel.addComponent((UIComponentAPI) playerPrismaticFaceMappingLabel)
            .setSize(180, 20)
            .inTL(playerFaceMappingX, playerFaceMappingY);
        playerPrismaticFaceMappingLabel.setOpacity(0f);

        float playerEffectY = BattleRenderingUtils.PANEL_HEIGHT - (opponentPrismaticBtnY + 20f) - 20f;

        playerPrismaticEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        playerPrismaticEffectLabel.setColor(Color.LIGHT_GRAY);
        playerPrismaticEffectLabel.setAlignment(Alignment.RMID);
        panel.addComponent((UIComponentAPI) playerPrismaticEffectLabel)
            .setSize(180, 20)
            .inTL(playerFaceMappingX, playerEffectY);
        playerPrismaticEffectLabel.setOpacity(0f);

        playerPrismaticRolledLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        playerPrismaticRolledLabel.setColor(ColorHelper.PRISMATIC_BRIGHT);
        playerPrismaticRolledLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) playerPrismaticRolledLabel)
            .setSize(60, 20);
        playerPrismaticRolledLabel.setOpacity(0f);

        TooltipMakerAPI exitTp = panel.createUIElement(btnWidth, btnHeight, false);
        exitTp.setActionListenerDelegate(this);
        panel.addUIElement(exitTp).inTL(BattleRenderingUtils.PANEL_WIDTH - btnWidth - 10f, 10f);
        ButtonAPI exitButton = exitTp.addButton(Strings.get("phase.close"), ACTION_EXIT, btnWidth, btnHeight, 0f);
        exitButton.setQuickMode(true);

        TooltipMakerAPI abilityTp = panel.createUIElement(PASSIVE_BTN_WIDTH, PASSIVE_BTN_HEIGHT, false);
        abilityTp.setActionListenerDelegate(this);
        panel.addUIElement(abilityTp).inBL(0f, 0f);

        float opponentCardUiX = BattleRenderingUtils.MARGIN;
        float opponentCardUiY = BattleRenderingUtils.MARGIN;
        float opponentAbilityY = opponentCardUiY + BattleRenderingUtils.CARD_HEIGHT + 15f;

        ButtonAPI opponentAbilityButton = abilityTp.addButton(Strings.get("battle.ability_btn"), ACTION_OPPONENT_ABILITY,
            PASSIVE_BTN_WIDTH, PASSIVE_BTN_HEIGHT, 0f);
        opponentAbilityButton.setQuickMode(true);
        opponentAbilityButton.getPosition().inTL(opponentCardUiX + 5f, opponentAbilityY);

        abilityTp.addTooltipToPrevious(new TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) { return false; }
            @Override
            public float getTooltipWidth(Object tooltipParam) { return 350f; }
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (battleState != null && battleState.getOpponentCard() != null) {
                    tooltip.addPara(battleState.getOpponentCard().getPassiveDescription(), 5f);
                }
            }
        }, TooltipLocation.RIGHT, false);

        float playerCardUiX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardUiY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        float playerAbilityY = playerCardUiY - PASSIVE_BTN_HEIGHT - 5f;

        ButtonAPI playerAbilityButton = abilityTp.addButton(Strings.get("battle.ability_btn"), ACTION_PLAYER_ABILITY,
            PASSIVE_BTN_WIDTH, PASSIVE_BTN_HEIGHT, 0f);
        playerAbilityButton.setQuickMode(true);
        playerAbilityButton.getPosition().inTL(playerCardUiX + 5f, playerAbilityY);

        abilityTp.addTooltipToPrevious(new TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) { return false; }
            @Override
            public float getTooltipWidth(Object tooltipParam) { return 350f; }
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (battleState != null && battleState.getPlayerCard() != null) {
                    tooltip.addPara(battleState.getPlayerCard().getPassiveDescription(), 5f);
                }
            }
        }, TooltipLocation.LEFT, false);

        buttonsCreated = true;
    }

    private void createLabels() {
        phaseLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        phaseLabel.setColor(ColorHelper.PHASE_LABEL);
        phaseLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) phaseLabel)
            .setSize(400, 30)
            .inTL(BattleRenderingUtils.PANEL_WIDTH / 2f - 200, 30);

        instructionLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        instructionLabel.setColor(BattleRenderingUtils.COLOR_TEXT);
        instructionLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) instructionLabel)
            .setSize(400, 25)
            .inTL(BattleRenderingUtils.PANEL_WIDTH / 2f - 200, 60);

        playerNameLabel = settings.createLabel(Strings.get("battle.player"), Fonts.DEFAULT_SMALL);
        playerNameLabel.setColor(ColorHelper.PLAYER_NAME);
        playerNameLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) playerNameLabel)
            .setSize(BattleRenderingUtils.CARD_WIDTH, 20)
            .inTL(BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN,
                  BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN + 5);

        playerHpLabel = settings.createLabel("25/25", Fonts.DEFAULT_SMALL);
        playerHpLabel.setColor(BattleRenderingUtils.COLOR_HP_TEXT);
        playerHpLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) playerHpLabel)
            .setSize(50, 20)
            .inTL(BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN + 5,
                  BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN + 5);

        opponentNameLabel = settings.createLabel(Strings.get("battle.opponent"), Fonts.DEFAULT_SMALL);
        opponentNameLabel.setColor(ColorHelper.OPPONENT_NAME);
        opponentNameLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) opponentNameLabel)
            .setSize(BattleRenderingUtils.CARD_WIDTH, 20)
            .inTL(BattleRenderingUtils.MARGIN,
                  BattleRenderingUtils.MARGIN + 5);

        opponentHpLabel = settings.createLabel("30/30", Fonts.DEFAULT_SMALL);
        opponentHpLabel.setColor(BattleRenderingUtils.COLOR_HP_TEXT);
        opponentHpLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) opponentHpLabel)
            .setSize(50, 20)
            .inTL(BattleRenderingUtils.MARGIN + 5,
                  BattleRenderingUtils.MARGIN + 5);

        resultLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        resultLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        resultLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) resultLabel)
            .setSize(400, 40)
            .inTL(BattleRenderingUtils.PANEL_WIDTH / 2f - 200, BattleRenderingUtils.PANEL_HEIGHT / 2f - 20);
        resultLabel.setOpacity(0f);

        playerAtkLabel = settings.createLabel("3", Fonts.DEFAULT_SMALL);
        playerAtkLabel.setColor(ColorHelper.ATTACK_VALUE);
        playerAtkLabel.setAlignment(Alignment.MID);
        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        panel.addComponent((UIComponentAPI) playerAtkLabel)
            .setSize(30, 20)
            .inTL(playerCardX + BattleRenderingUtils.ATK_LEFT_MARGIN + 2f,
                  playerCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        playerDefLabel = settings.createLabel("2", Fonts.DEFAULT_SMALL);
        playerDefLabel.setColor(ColorHelper.DEFENSE_VALUE);
        playerDefLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) playerDefLabel)
            .setSize(30, 20)
            .inTL(playerCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN - 30f,
                  playerCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        opponentAtkLabel = settings.createLabel("3", Fonts.DEFAULT_SMALL);
        opponentAtkLabel.setColor(ColorHelper.ATTACK_VALUE);
        opponentAtkLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) opponentAtkLabel)
            .setSize(30, 20)
            .inTL(opponentCardX + BattleRenderingUtils.ATK_LEFT_MARGIN + 2f,
                  opponentCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        opponentDefLabel = settings.createLabel("2", Fonts.DEFAULT_SMALL);
        opponentDefLabel.setColor(ColorHelper.DEFENSE_VALUE);
        opponentDefLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) opponentDefLabel)
            .setSize(30, 20)
            .inTL(opponentCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN - 30f,
                  opponentCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        // Player dice count labels (centered on dice icons at top-right of card)
        float diceX = playerCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
        float diceStartY = playerCardY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

        playerPrismaticLabel = createCountLabel(diceX, diceStartY);
        playerOrangeLabel = createCountLabel(diceX, diceStartY + 26);
        playerPurpleLabel = createCountLabel(diceX, diceStartY + 52);
        playerBlueLabel = createCountLabel(diceX, diceStartY + 78);

        // Opponent dice count labels (centered on dice icons at top-right of card)
        diceX = opponentCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
        diceStartY = opponentCardY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

        opponentPrismaticLabel = createCountLabel(diceX, diceStartY);
        opponentOrangeLabel = createCountLabel(diceX, diceStartY + 26);
        opponentPurpleLabel = createCountLabel(diceX, diceStartY + 52);
        opponentBlueLabel = createCountLabel(diceX, diceStartY + 78);

        LabelAPI attackerValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        attackerValueLabel.setColor(ColorHelper.ATTACK_VALUE);
        attackerValueLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerValueLabel)
            .setSize(60, 40);
        attackerValueLabel.setOpacity(0f);

        LabelAPI defenderValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        defenderValueLabel.setColor(ColorHelper.DEFENSE_VALUE);
        defenderValueLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) defenderValueLabel)
            .setSize(60, 40);
        defenderValueLabel.setOpacity(0f);

        attackerValueAnimator = new ValueChangeAnimator();
        defenderValueAnimator = new ValueChangeAnimator();
        
        clickHintLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        clickHintLabel.setColor(new Color(200, 200, 200, 150));
        clickHintLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) clickHintLabel)
            .setSize(200, 20)
            .inTL(BattleRenderingUtils.PANEL_WIDTH / 2f - 100f, BattleRenderingUtils.PANEL_HEIGHT - 30f);
        clickHintLabel.setOpacity(0f);

        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float labelWidth = 200f;

        attackerSelectionLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        attackerSelectionLabel.setColor(ColorHelper.ATTACK_VALUE);
        attackerSelectionLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerSelectionLabel)
            .setSize(labelWidth, 20)
            .inTL(centerX - labelWidth / 2f, bottomIconCenterY + iconSize / 2f + 25f);
        attackerSelectionLabel.setOpacity(0f);

        attackerEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        attackerEffectLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        attackerEffectLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerEffectLabel)
            .setSize(labelWidth, 20)
            .inTL(centerX - labelWidth / 2f, bottomIconCenterY + iconSize / 2f + 5f);
        attackerEffectLabel.setOpacity(0f);

        defenderSelectionLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        defenderSelectionLabel.setColor(ColorHelper.DEFENSE_VALUE);
        defenderSelectionLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) defenderSelectionLabel)
            .setSize(labelWidth, 20)
            .inTL(centerX - labelWidth / 2f, topIconCenterY - iconSize / 2f - 25f);
        defenderSelectionLabel.setOpacity(0f);

        defenderEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        defenderEffectLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        defenderEffectLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) defenderEffectLabel)
            .setSize(labelWidth, 20)
            .inTL(centerX - labelWidth / 2f, topIconCenterY - iconSize / 2f - 5f);
        defenderEffectLabel.setOpacity(0f);

        float rightCenterX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH / 2f - BattleRenderingUtils.MARGIN;
        float confirmedLabelWidth = 150f;
        
        attackerConfirmedSelectionLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        attackerConfirmedSelectionLabel.setColor(ColorHelper.ATTACK_VALUE);
        attackerConfirmedSelectionLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerConfirmedSelectionLabel)
            .setSize(confirmedLabelWidth, 20)
            .inTL(rightCenterX - confirmedLabelWidth / 2f, bottomIconCenterY + iconSize / 2f + 25f);
        attackerConfirmedSelectionLabel.setOpacity(0f);

        attackerConfirmedEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        attackerConfirmedEffectLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        attackerConfirmedEffectLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerConfirmedEffectLabel)
            .setSize(confirmedLabelWidth, 20)
            .inTL(rightCenterX - confirmedLabelWidth / 2f, bottomIconCenterY + iconSize / 2f + 5f);
        attackerConfirmedEffectLabel.setOpacity(0f);
        
        attackerIconValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        attackerIconValueLabel.setColor(ColorHelper.ATTACK_VALUE);
        attackerIconValueLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerIconValueLabel)
            .setSize(80, 50);
        attackerIconValueLabel.setOpacity(0f);
        
        defenderIconValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        defenderIconValueLabel.setColor(ColorHelper.DEFENSE_VALUE);
        defenderIconValueLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) defenderIconValueLabel)
            .setSize(80, 50);
        defenderIconValueLabel.setOpacity(0f);
    }

    private LabelAPI createCountLabel(float x, float y) {
        LabelAPI label = Global.getSettings().createLabel("0", Fonts.DEFAULT_SMALL);
        label.setColor(Color.WHITE);
        label.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) label).setSize(22, 16).inTL(x, y);
        return label;
    }
    
    public void startDamageResolutionAnimation(BattleState state, DamageResolver.DamageResult result) {
        pendingDamageResult = result;
        damageAnimationPending = true;
        
        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        float playerCardCenterX = playerCardX + BattleRenderingUtils.CARD_WIDTH / 2f;
        float playerCardCenterY = playerCardY + BattleRenderingUtils.CARD_HEIGHT / 2f;
        
        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        float opponentCardCenterX = opponentCardX + BattleRenderingUtils.CARD_WIDTH / 2f;
        float opponentCardCenterY = opponentCardY + BattleRenderingUtils.CARD_HEIGHT / 2f;
        
        float defenderTargetX = state.isPlayerAttacker() ? opponentCardCenterX : playerCardCenterX;
        float defenderTargetY = state.isPlayerAttacker() ? opponentCardCenterY : playerCardCenterY;
        
        damageAnimator = new DamageResolutionAnimator();
        damageAnimator.startResolution(state, defenderTargetX, defenderTargetY, panel);
    }

    public void updateLabelsFromState() {
        if (battleState == null || playerNameLabel == null) return;

        CharacterCard playerCard = battleState.getPlayerCard();
        CharacterCard opponentCard = battleState.getOpponentCard();

        if (playerCard != null) {
            playerNameLabel.setText(playerCard.getName());
            playerHpLabel.setText(String.format("%d/%d", battleState.getPlayerHp(), playerCard.getMaxHp()));
            playerAtkLabel.setText(String.valueOf(playerCard.getAtkLevel()));
            playerDefLabel.setText(String.valueOf(playerCard.getDefLevel()));
        }

        if (opponentCard != null) {
            opponentNameLabel.setText(opponentCard.getName());
            opponentHpLabel.setText(String.format("%d/%d", battleState.getOpponentHp(), opponentCard.getMaxHp()));
            opponentAtkLabel.setText(String.valueOf(opponentCard.getAtkLevel()));
            opponentDefLabel.setText(String.valueOf(opponentCard.getDefLevel()));
        }

        // Update dice pool count labels from CharacterCard's base dice pool
        DicePoolCounts playerCounts = battleState.getPlayerDicePoolCounts();
        DicePoolCounts opponentCounts = battleState.getOpponentDicePoolCounts();

        playerPrismaticLabel.setText(String.valueOf(playerCounts != null ? playerCounts.getCount(DiceType.PRISMATIC) : 0));
        playerOrangeLabel.setText(String.valueOf(playerCounts != null ? playerCounts.getCount(DiceType.ORANGE_D8) : 0));
        playerPurpleLabel.setText(String.valueOf(playerCounts != null ? playerCounts.getCount(DiceType.PURPLE_D6) : 0));
        playerBlueLabel.setText(String.valueOf(playerCounts != null ? playerCounts.getCount(DiceType.BLUE_D4) : 0));

        opponentPrismaticLabel.setText(String.valueOf(opponentCounts != null ? opponentCounts.getCount(DiceType.PRISMATIC) : 0));
        opponentOrangeLabel.setText(String.valueOf(opponentCounts != null ? opponentCounts.getCount(DiceType.ORANGE_D8) : 0));
        opponentPurpleLabel.setText(String.valueOf(opponentCounts != null ? opponentCounts.getCount(DiceType.PURPLE_D6) : 0));
        opponentBlueLabel.setText(String.valueOf(opponentCounts != null ? opponentCounts.getCount(DiceType.BLUE_D4) : 0));

        updatePhaseLabel();
        updatePrismaticButton();
    }

    private void updatePhaseLabel() {
        if (phaseLabel == null || battleState == null) return;
        Phase phase = battleState.getCurrentPhase();
        boolean playerAttacking = battleState.isPlayerAttacker();

        String phaseText = switch (phase) {
            case ROLLING -> Strings.get("phase.rolling");
            case SELECTING_ATTACK -> playerAttacking ? Strings.get("phase.your_attack") : Strings.get("phase.opponent_attack");
            case SELECTING_DEFENSE -> playerAttacking ? Strings.get("phase.opponent_defense") : Strings.get("phase.your_defense");
            case RESOLVING_PRE_CLASH -> Strings.get("phase.pre_clash");
            case RESOLVING -> Strings.get("phase.resolving");
            case WAITING_NEXT_TURN -> Strings.format("phase.turn_complete", battleState.getTurnNumber());
            case ENDED -> battleState.getWinner().equals("player") ? Strings.get("phase.victory") : Strings.get("phase.defeat");
        };

        phaseLabel.setText(phaseText);

        String instructionText = "";
        if (phase == Phase.SELECTING_ATTACK || phase == Phase.SELECTING_DEFENSE) {
            boolean playerShouldSelect = (phase == Phase.SELECTING_ATTACK && playerAttacking) ||
                                         (phase == Phase.SELECTING_DEFENSE && !playerAttacking);
            if (playerShouldSelect) {
                int required = battleState.getRequiredPlayerDiceCount();
                int remaining = battleState.getRemainingRerolls();
                String rerollHint = remaining > 0 ? Strings.format("phase.reroll_hint", remaining) : "";
                instructionText = Strings.format("phase.select_dice", required) + rerollHint;
            } else {
                instructionText = Strings.get("phase.opponent_selecting");
            }
        } else if (phase == Phase.WAITING_NEXT_TURN) {
            instructionText = Strings.get("phase.click_continue");
        }

        instructionLabel.setText(instructionText);

        if (phase == Phase.ENDED) {
            resultLabel.setText(battleState.getWinner().equals("player") ? Strings.get("battle.you_won") : Strings.get("battle.you_lost"));
            resultLabel.setOpacity(1f);
        } else {
            resultLabel.setOpacity(0f);
        }

        updateButtons(phase);
        updateSelectionDisplayLabels();
    }

    private void updateButtons(Phase phase) {
        if (confirmButton == null || rerollButton == null) return;

        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean playerShouldSelect = (playerIsAttacker && phase == Phase.SELECTING_ATTACK) ||
                                      (!playerIsAttacker && phase == Phase.SELECTING_DEFENSE);

        String confirmText = switch (phase) {
            case SELECTING_ATTACK -> playerIsAttacker ? Strings.get("battle.confirm_attack") : Strings.get("phase.waiting");
            case SELECTING_DEFENSE -> playerIsAttacker ? Strings.get("phase.waiting") : Strings.get("battle.confirm_defense");
            case RESOLVING_PRE_CLASH -> Strings.get("phase.resolving");
            case RESOLVING -> Strings.get("phase.resolving");
            case WAITING_NEXT_TURN -> Strings.get("phase.continue");
            case ENDED -> Strings.get("phase.close");
            default -> Strings.get("phase.waiting");
        };
        
        confirmButton.setText(confirmText);
        confirmButton.setCustomData(switch (phase) {
            case SELECTING_ATTACK, SELECTING_DEFENSE -> playerShouldSelect ? ACTION_END_TURN : "none";
            case WAITING_NEXT_TURN -> ACTION_CONTINUE;
            case ENDED -> "close";
            default -> "none";
        });

        int selectedCount = battleState.countSelectedDice(true);
        int requiredCount = battleState.getRequiredPlayerDiceCount();
        boolean hasRerolls = battleState.getRemainingRerolls(true) > 0;

        boolean canConfirm = playerShouldSelect && selectedCount == requiredCount 
                             && battleState.canConfirmPrismaticSelection(true);
        confirmButton.setEnabled(canConfirm || phase == Phase.WAITING_NEXT_TURN || phase == Phase.ENDED);

        rerollButton.setEnabled(playerShouldSelect && selectedCount > 0 && hasRerolls);

        updatePrismaticButton();
    }

    private void updateSelectionDisplayLabels() {
        if (battleState == null || attackerSelectionLabel == null) return;
        
        Phase phase = battleState.getCurrentPhase();
        boolean inSelectionPhase = phase == Phase.SELECTING_ATTACK || phase == Phase.SELECTING_DEFENSE;
        
        if (!inSelectionPhase) {
            attackerSelectionLabel.setOpacity(0f);
            attackerEffectLabel.setOpacity(0f);
            defenderSelectionLabel.setOpacity(0f);
            defenderEffectLabel.setOpacity(0f);
            return;
        }
        
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean attackerSelecting = phase == Phase.SELECTING_ATTACK;
        boolean defenderSelecting = phase == Phase.SELECTING_DEFENSE;
        
        updateSelectionLabelPositions();
        
        if (attackerSelecting) {
            boolean attackerIsPlayer = playerIsAttacker;
            String valuesStr = battleState.getSelectedDiceValuesFormatted(attackerIsPlayer);
            List<String> effects = battleState.getSelectedPrismaticEffectStrings(attackerIsPlayer);
            
            if (valuesStr.isEmpty()) {
                attackerSelectionLabel.setText(Strings.get("battle.no_selection"));
            } else {
                attackerSelectionLabel.setText(Strings.format("battle.selected_values", valuesStr));
            }
            
            if (!effects.isEmpty()) {
                attackerEffectLabel.setText(String.join(" ", effects));
            } else {
                attackerEffectLabel.setText("");
            }
            
            attackerSelectionLabel.setOpacity(1f);
            attackerEffectLabel.setOpacity(!effects.isEmpty() ? 1f : 0f);
            
            defenderSelectionLabel.setOpacity(0f);
            defenderEffectLabel.setOpacity(0f);
        } else if (defenderSelecting) {
            boolean defenderIsPlayer = !playerIsAttacker;
            String valuesStr = battleState.getSelectedDiceValuesFormatted(defenderIsPlayer);
            List<String> effects = battleState.getSelectedPrismaticEffectStrings(defenderIsPlayer);
            
            if (valuesStr.isEmpty()) {
                defenderSelectionLabel.setText(Strings.get("battle.no_selection"));
            } else {
                defenderSelectionLabel.setText(Strings.format("battle.selected_values", valuesStr));
            }
            
            if (!effects.isEmpty()) {
                defenderEffectLabel.setText(String.join(" ", effects));
            } else {
                defenderEffectLabel.setText("");
            }
            
            defenderSelectionLabel.setOpacity(1f);
            defenderEffectLabel.setOpacity(!effects.isEmpty() ? 1f : 0f);
            
            attackerSelectionLabel.setOpacity(0f);
            attackerEffectLabel.setOpacity(0f);
        }
    }

    private void updateSelectionLabelPositions() {
        if (battleState == null || attackerSelectionLabel == null) return;
        
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float labelWidth = 200f;
        
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        
        float atkCenterY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defCenterY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;
        
        float atkSelectionY = atkCenterY > halfH ? atkCenterY + iconSize / 2f + 25f : atkCenterY - iconSize / 2f - 25f;
        float atkEffectY = atkCenterY > halfH ? atkCenterY + iconSize / 2f + 5f : atkCenterY - iconSize / 2f - 5f;
        float defSelectionY = defCenterY > halfH ? defCenterY + iconSize / 2f + 25f : defCenterY - iconSize / 2f - 25f;
        float defEffectY = defCenterY > halfH ? defCenterY + iconSize / 2f + 5f : defCenterY - iconSize / 2f - 5f;
        
        attackerSelectionLabel.getPosition().inTL(centerX - labelWidth / 2f, atkSelectionY);
        attackerEffectLabel.getPosition().inTL(centerX - labelWidth / 2f, atkEffectY);
        defenderSelectionLabel.getPosition().inTL(centerX - labelWidth / 2f, defSelectionY);
        defenderEffectLabel.getPosition().inTL(centerX - labelWidth / 2f, defEffectY);
    }

    private void updateConfirmedSelectionLabels() {
        if (battleState == null || attackerConfirmedSelectionLabel == null) return;
        
        Phase phase = battleState.getCurrentPhase();
        
        boolean shouldShowAttacker = phase == Phase.SELECTING_DEFENSE || 
                             (phase == Phase.ROLLING && battleState.isDefenderRolling()) ||
                             phase == Phase.RESOLVING_PRE_CLASH;
        
        boolean shouldShowDefender = phase == Phase.RESOLVING_PRE_CLASH;
        
        if (phase == Phase.RESOLVING) {
            attackerConfirmedSelectionLabel.setOpacity(0f);
            attackerConfirmedEffectLabel.setOpacity(0f);
            defenderSelectionLabel.setOpacity(0f);
            defenderEffectLabel.setOpacity(0f);
            return;
        }
        
        updateConfirmedLabelPositions();
        
        if (shouldShowAttacker) {
            String confirmedText = battleState.getAttackerConfirmedSelectionText();
            List<String> confirmedEffects = battleState.getAttackerConfirmedEffects();
            
            if (confirmedText != null && !confirmedText.isEmpty()) {
                attackerConfirmedSelectionLabel.setText(Strings.format("battle.selected_values", confirmedText));
                attackerConfirmedSelectionLabel.setOpacity(1f);
                
                if (confirmedEffects != null && !confirmedEffects.isEmpty()) {
                    attackerConfirmedEffectLabel.setText(String.join(" ", confirmedEffects));
                    attackerConfirmedEffectLabel.setOpacity(1f);
                } else {
                    attackerConfirmedEffectLabel.setText("");
                    attackerConfirmedEffectLabel.setOpacity(0f);
                }
            } else {
                attackerConfirmedSelectionLabel.setOpacity(0f);
                attackerConfirmedEffectLabel.setOpacity(0f);
            }
        } else {
            attackerConfirmedSelectionLabel.setOpacity(0f);
            attackerConfirmedEffectLabel.setOpacity(0f);
        }
        
        if (shouldShowDefender) {
            String confirmedText = battleState.getDefenderConfirmedSelectionText();
            List<String> confirmedEffects = battleState.getDefenderConfirmedEffects();
            
            if (confirmedText != null && !confirmedText.isEmpty()) {
                defenderSelectionLabel.setText(Strings.format("battle.selected_values", confirmedText));
                defenderSelectionLabel.setOpacity(1f);
                
                if (confirmedEffects != null && !confirmedEffects.isEmpty()) {
                    defenderEffectLabel.setText(String.join(" ", confirmedEffects));
                    defenderEffectLabel.setOpacity(1f);
                } else {
                    defenderEffectLabel.setText("");
                    defenderEffectLabel.setOpacity(0f);
                }
            } else {
                defenderSelectionLabel.setOpacity(0f);
                defenderEffectLabel.setOpacity(0f);
            }
        } else {
            defenderSelectionLabel.setOpacity(0f);
            defenderEffectLabel.setOpacity(0f);
        }
    }
    
    private void updateConfirmedLabelPositions() {
        if (battleState == null) return;
        
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        float atkCenterY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defCenterY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;
        
        float rightX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH / 2f - BattleRenderingUtils.MARGIN;
        float leftX = BattleRenderingUtils.CARD_WIDTH / 2f + BattleRenderingUtils.MARGIN;
        float confirmedLabelWidth = 150f;
        
        float attackerX = playerIsAttacker ? rightX : leftX;
        float defenderX = playerIsAttacker ? leftX : rightX;
        
        float atkSelectionY = atkCenterY > halfH ? atkCenterY + iconSize / 2f + 25f : atkCenterY - iconSize / 2f - 25f;
        float atkEffectY = atkCenterY > halfH ? atkCenterY + iconSize / 2f + 5f : atkCenterY - iconSize / 2f - 5f;
        float defSelectionY = defCenterY > halfH ? defCenterY + iconSize / 2f + 25f : defCenterY - iconSize / 2f - 25f;
        float defEffectY = defCenterY > halfH ? defCenterY + iconSize / 2f + 5f : defCenterY - iconSize / 2f - 5f;
        
        attackerConfirmedSelectionLabel.getPosition().inTL(attackerX - confirmedLabelWidth / 2f, atkSelectionY);
        attackerConfirmedEffectLabel.getPosition().inTL(attackerX - confirmedLabelWidth / 2f, atkEffectY);
        
        defenderSelectionLabel.getPosition().inTL(defenderX - confirmedLabelWidth / 2f, defSelectionY);
        defenderEffectLabel.getPosition().inTL(defenderX - confirmedLabelWidth / 2f, defEffectY);
    }
    
    private void updateIconValueLabels() {
        if (battleState == null || attackerIconValueLabel == null || defenderIconValueLabel == null) return;
        
        Phase phase = battleState.getCurrentPhase();
        
        attackerIconValueLabel.setOpacity(0f);
        defenderIconValueLabel.setOpacity(0f);
        
        if (phase == Phase.RESOLVING) return;
        
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float labelW = 80f;
        float labelH = 50f;
        
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        
        float topIconCenterY = halfH / 2f;
        float bottomIconCenterY = halfH + halfH / 2f;
        
        float attackerLabelY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defenderLabelY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;
        
        attackerIconValueLabel.getPosition().inTL(centerX - labelW / 2f, attackerLabelY - labelH / 2f);
        defenderIconValueLabel.getPosition().inTL(centerX - labelW / 2f, defenderLabelY - labelH / 2f);
        
        if (phase == Phase.SELECTING_ATTACK) {
            boolean playerIsSelecting = playerIsAttacker;
            int runningTotal = battleState.calculateSelectedSum(playerIsSelecting);
            if (runningTotal > 0) {
                attackerIconValueLabel.setText(String.valueOf(runningTotal));
                attackerIconValueLabel.setOpacity(playerIsSelecting ? 1f : 0f);
            }
            defenderIconValueLabel.setOpacity(0f);
            return;
        }
        
        if (phase == Phase.SELECTING_DEFENSE || (phase == Phase.ROLLING && battleState.isDefenderRolling())) {
            int attackerValue = getAttackerTotalValue();
            attackerIconValueLabel.setText(String.valueOf(attackerValue));
            attackerIconValueLabel.setOpacity(attackerValue > 0 ? 1f : 0f);
            
            if (phase == Phase.SELECTING_DEFENSE) {
                boolean playerIsSelecting = !playerIsAttacker;
                int runningTotal = battleState.calculateSelectedSum(playerIsSelecting);
                if (runningTotal > 0) {
                    defenderIconValueLabel.setText(String.valueOf(runningTotal));
                    defenderIconValueLabel.setOpacity(playerIsSelecting ? 1f : 0f);
                }
            }
            return;
        }
        
        if (phase == Phase.RESOLVING_PRE_CLASH) {
            int attackerValue = getAttackerTotalValue();
            int defenderValue = getDefenderTotalValue();
            
            attackerIconValueLabel.setText(String.valueOf(attackerValue));
            attackerIconValueLabel.setOpacity(attackerValue > 0 ? 1f : 0f);
            defenderIconValueLabel.setText(String.valueOf(defenderValue));
            defenderIconValueLabel.setOpacity(defenderValue > 0 ? 1f : 0f);
        }
    }
    
    private int getAttackerTotalValue() {
        int baseValue = battleState.getAttackValue();
        StatusEffectProcessor attackerEffects = battleState.isPlayerAttacker() 
            ? battleState.getPlayerEffects() : battleState.getOpponentEffects();
        int bonus = attackerEffects.calculateAttackBonus(TurnType.ATTACK);
        int prismaticValue = battleState.getPrismaticDiceTotalValue(battleState.isPlayerAttacker());
        return baseValue + bonus + prismaticValue;
    }
    
    private int getDefenderTotalValue() {
        int baseValue = battleState.getDefenseValue();
        StatusEffectProcessor defenderEffects = battleState.isPlayerAttacker() 
            ? battleState.getOpponentEffects() : battleState.getPlayerEffects();
        int bonus = defenderEffects.calculateDefenseBonus(TurnType.DEFENSE);
        int prismaticValue = battleState.getPrismaticDiceTotalValue(!battleState.isPlayerAttacker());
        return baseValue + bonus + prismaticValue;
    }

    private void updatePrismaticButton() {
        if (playerPrismaticUsesLabel == null || battleState == null) return;

        int uses = battleState.getPlayerPrismaticUses();
        playerPrismaticUsesLabel.setText(String.valueOf(uses));

        if (battleState.isPlayerPrismaticModeActive()) {
            playerPrismaticUsesLabel.setColor(ColorHelper.OPPONENT_NAME);
        } else if (uses > 0) {
            playerPrismaticUsesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        } else {
            playerPrismaticUsesLabel.setColor(ColorHelper.PRISMATIC_DISABLED);
        }
        
        updatePrismaticFaceMappingDisplay();
        updateOpponentPrismaticDisplay();
    }
    
    private void updatePrismaticFaceMappingDisplay() {
        if (playerPrismaticFaceMappingLabel == null || battleState == null) return;
        
        CharacterCard playerCard = battleState.getPlayerCard();
        if (playerCard == null) {
            playerPrismaticFaceMappingLabel.setOpacity(0f);
            playerPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        java.util.Map<String, Integer> prismaticIds = playerCard.getPrismaticDiceIds();
        if (prismaticIds == null || prismaticIds.isEmpty()) {
            playerPrismaticFaceMappingLabel.setOpacity(0f);
            playerPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        String firstDiceId = prismaticIds.keySet().iterator().next();
        PrismaticDiceType type = PrismaticDiceRegistry.get(firstDiceId);
        if (type == null) {
            playerPrismaticFaceMappingLabel.setOpacity(0f);
            playerPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        int uses = battleState.getPlayerPrismaticUses();
        if (uses <= 0) {
            playerPrismaticFaceMappingLabel.setOpacity(0.4f);
            playerPrismaticEffectLabel.setOpacity(0.4f);
        } else {
            playerPrismaticFaceMappingLabel.setOpacity(1f);
            playerPrismaticEffectLabel.setOpacity(1f);
        }
        
        String mappingText = PrismaticFaceDisplay.formatFaceMappingCompact(type, false);
        playerPrismaticFaceMappingLabel.setText(mappingText);
        
        String effectText = PrismaticFaceDisplay.getEffectDescription(type);
        playerPrismaticEffectLabel.setText(effectText);
    }
    
    private void updateOpponentPrismaticDisplay() {
        if (opponentPrismaticUsesLabel == null || battleState == null) return;
        
        int uses = battleState.getOpponentPrismaticUses();
        opponentPrismaticUsesLabel.setText(String.valueOf(uses));
        opponentPrismaticUsesLabel.setColor(uses > 0 ? ColorHelper.PRISMATIC_GOLD : ColorHelper.PRISMATIC_DISABLED);
        
        CharacterCard opponentCard = battleState.getOpponentCard();
        if (opponentCard == null) {
            opponentPrismaticFaceMappingLabel.setOpacity(0f);
            opponentPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        java.util.Map<String, Integer> prismaticIds = opponentCard.getPrismaticDiceIds();
        if (prismaticIds == null || prismaticIds.isEmpty()) {
            opponentPrismaticFaceMappingLabel.setOpacity(0f);
            opponentPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        String firstDiceId = prismaticIds.keySet().iterator().next();
        PrismaticDiceType type = PrismaticDiceRegistry.get(firstDiceId);
        if (type == null) {
            opponentPrismaticFaceMappingLabel.setOpacity(0f);
            opponentPrismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        float opacity = uses > 0 ? 1f : 0.4f;
        opponentPrismaticFaceMappingLabel.setOpacity(opacity);
        opponentPrismaticEffectLabel.setOpacity(opacity);
        
        String mappingText = PrismaticFaceDisplay.formatFaceMappingCompact(type, false);
        opponentPrismaticFaceMappingLabel.setText(mappingText);
        
        String effectText = PrismaticFaceDisplay.getEffectDescription(type);
        opponentPrismaticEffectLabel.setText(effectText);
    }
    
    private void updatePrismaticRolledLabel() {
        if (playerPrismaticRolledLabel == null) return;
        
        if (pendingPrismaticInstance == null || pendingPrismaticAnimatorIndex < 0) {
            playerPrismaticRolledLabel.setOpacity(0f);
            return;
        }
        
        if (diceRollManager == null || pendingPrismaticAnimatorIndex >= diceRollManager.getAnimatorCount()) {
            playerPrismaticRolledLabel.setOpacity(0f);
            return;
        }
        
        float diceX = diceRollManager.getAnimatorVisualX(pendingPrismaticAnimatorIndex);
        float diceY = diceRollManager.getAnimatorVisualY(pendingPrismaticAnimatorIndex);
        
        String rolledText = PrismaticFaceDisplay.formatRolledResult(pendingPrismaticInstance);
        playerPrismaticRolledLabel.setText(rolledText);
        playerPrismaticRolledLabel.setOpacity(1f);
        
        float labelWidth = 60f;
        playerPrismaticRolledLabel.getPosition().inTL(diceX + DiceAnimator.DICE_SIZE / 2f - labelWidth / 2f, 
                                                 diceY + DiceAnimator.DICE_SIZE + PRISMATIC_ROLLED_LABEL_OFFSET_Y);
    }
    
    private void clearPrismaticRolledLabel() {
        if (playerPrismaticRolledLabel != null) {
            playerPrismaticRolledLabel.setOpacity(0f);
        }
        pendingPrismaticInstance = null;
        pendingPrismaticAnimatorIndex = -1;
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            String action = (String) btn.getCustomData();

            switch (action) {
                case ACTION_REROLL -> {
                    CosmiconLogger.info("Player clicked Reroll button");
                    if (battleController != null) {
                        battleController.onPlayerReroll();
                    }
                }
                case ACTION_END_TURN -> {
                    CosmiconLogger.info("Player clicked Confirm button");
                    if (battleController != null) {
                        battleController.onPlayerConfirmSelection();
                    }
                }
                case ACTION_CONTINUE -> {
                    CosmiconLogger.info("Player clicked Continue button");
                    if (battleController != null) {
                        battleController.onContinueToNextTurn();
                    }
                }
                case "close" -> {
                    CosmiconLogger.info("Player clicked Close button - dismissing dialog");
                    if (callbacks != null) {
                        callbacks.dismissDialog();
                    }
                }
                case ACTION_EXIT -> {
                    CosmiconLogger.info("Player clicked Exit button - dismissing dialog");
                    if (callbacks != null) {
                        callbacks.dismissDialog();
                    }
                }
            }
        }
    }

    private void showPrismaticSelectionPopup() {
        prismaticPopupActive = true;
        prismaticPopup = new PrismaticDiceSelectionPopup(battleState, new PrismaticDiceSelectionPopup.PrismaticDiceSelectionCallback() {
            @Override
            public void onPrismaticDiceSelected(PrismaticDiceType type, PrismaticDiceInstance instance) {
                CosmiconLogger.info("Player selected Prismatic dice: %s, face: %d", type.getId(), instance.rolledFace);
                battleState.addPrismaticDiceToPool(instance, true);
                pendingPrismaticInstance = instance;
                pendingPrismaticAnimatorIndex = diceRollManager.getAnimatorCount();
                diceRollManager.appendInstantDice(DiceType.PRISMATIC, instance.faceIndex, diceZoneCenterX, diceZoneCenterY);
                closePrismaticPopup();
                createDiceHitboxes(battleState.getPlayerDiceTypes());
                updateButtons(battleState.getCurrentPhase());
                updatePrismaticFaceMappingDisplay();
            }

            @Override
            public void onPopupClosed() {
                CosmiconLogger.info("Prismatic popup closed without selection");
                closePrismaticPopup();
            }
        });

        prismaticPopupPanel = panel.createCustomPanel(400f, 300f, prismaticPopup);
        float popupX = (BattleRenderingUtils.PANEL_WIDTH - 400f) / 2f;
        float popupY = (BattleRenderingUtils.PANEL_HEIGHT - 300f) / 2f;
        panel.addComponent(prismaticPopupPanel).inTL(popupX, popupY);
        prismaticPopup.init(prismaticPopupPanel);
    }

    private void closePrismaticPopup() {
        prismaticPopupActive = false;
        if (prismaticPopupPanel != null && panel != null) {
            panel.removeComponent(prismaticPopupPanel);
        }
        prismaticPopupPanel = null;
        prismaticPopup = null;
    }

    @Override
    public void onPhaseChange(Phase newPhase) {
        if (newPhase == Phase.ROLLING) {
            rollAnimationDelay = 0.3f;
            diceAnimating = true;
            clearPrismaticRolledLabel();
            
            if (diceRollManager != null) {
                diceRollManager.clear();
                diceRollManager.clearOpponentAnimators();
            }
        }
        
        if (newPhase == Phase.RESOLVING_PRE_CLASH) {
            boolean hasAttackerChanges = !battleState.getPendingValueChanges(battleState.isPlayerAttacker()).isEmpty();
            boolean hasDefenderChanges = !battleState.getPendingValueChanges(!battleState.isPlayerAttacker()).isEmpty();
            
            if (hasAttackerChanges || hasDefenderChanges) {
                startValueChangeAnimations();
            }
        }

        if (phaseLabel == null) return;
        updatePhaseLabel();
    }

    @Override
    public void onDiceRerolled(boolean isPlayer, List<Integer> newValues, List<Integer> rerolledIndices) {
        if (diceRollManager == null || rerolledIndices.isEmpty()) return;
        
        float animDuration = DiceAnimator.getTotalDuration() + 0.1f;
        
        if (isPlayer) {
            diceRollManager.partialReroll(rerolledIndices, newValues);
            rollAnimationDelay = animDuration;
            diceAnimating = true;
        } else {
            diceRollManager.rerollOpponentDice(rerolledIndices, newValues);
            opponentRollDelay = animDuration;
            opponentDiceAnimating = true;
        }

        if (phaseLabel == null) return;
        updateButtons(battleState.getCurrentPhase());
        updatePhaseLabel();
    }

    @Override
    public void onDamageResolved(int damage, int playerHp, int opponentHp) {
        updateLabelsFromState();
    }

    @Override
    public void onBattleEnd(String winner) {
        updatePhaseLabel();
    }

    @Override
    public void onDiceRolled(boolean isPlayer, List<DiceType> types, List<Integer> values) {
        if (isPlayer && types != null) {
            DicePoolCounts counts = DicePoolCounts.fromPool(types);
            playerPrismaticLabel.setText(String.valueOf(counts.getCount(DiceType.PRISMATIC)));
            playerOrangeLabel.setText(String.valueOf(counts.getCount(DiceType.ORANGE_D8)));
            playerPurpleLabel.setText(String.valueOf(counts.getCount(DiceType.PURPLE_D6)));
            playerBlueLabel.setText(String.valueOf(counts.getCount(DiceType.BLUE_D4)));
        } else if (!isPlayer && types != null) {
            DicePoolCounts counts = DicePoolCounts.fromPool(types);
            opponentPrismaticLabel.setText(String.valueOf(counts.getCount(DiceType.PRISMATIC)));
            opponentOrangeLabel.setText(String.valueOf(counts.getCount(DiceType.ORANGE_D8)));
            opponentPurpleLabel.setText(String.valueOf(counts.getCount(DiceType.PURPLE_D6)));
            opponentBlueLabel.setText(String.valueOf(counts.getCount(DiceType.BLUE_D4)));
            
            triggerOpponentDiceRoll();
        }
    }

    @Override
    public void onWeatherChange(WeatherType newWeather) {
    }

    @Override
    public void onDamageAnimationStart(DamageResolver.DamageResult result) {
        if (battleState == null) return;
        
        pendingDamageResult = result;
        
        boolean hasAttackerChanges = !battleState.getPendingValueChanges(battleState.isPlayerAttacker()).isEmpty();
        boolean hasDefenderChanges = !battleState.getPendingValueChanges(!battleState.isPlayerAttacker()).isEmpty();
        
        if (hasAttackerChanges || hasDefenderChanges) {
            startValueChangeAnimations();
            valueAnimationPending = true;
        } else {
            startDamageResolutionAnimation(battleState, result);
        }
    }

    private void startValueChangeAnimations() {
        if (battleState == null || panel == null) return;
        
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        
        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        
        float attackerCenterX = playerIsAttacker ? bottomIconCenterX : topIconCenterX;
        float attackerCenterY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defenderCenterX = playerIsAttacker ? topIconCenterX : bottomIconCenterX;
        float defenderCenterY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;
        
        int attackValue = battleState.getAttackValue();
        int defenseValue = battleState.getDefenseValue();
        
        List<BattleState.ValueChangeRecord> attackerChanges = battleState.getPendingValueChanges(playerIsAttacker);
        List<BattleState.ValueChangeRecord> defenderChanges = battleState.getPendingValueChanges(!playerIsAttacker);
        
        if (!attackerChanges.isEmpty()) {
            attackerValueAnimator.start(attackerCenterX, attackerCenterY, attackValue, panel, true);
        }
        if (!defenderChanges.isEmpty()) {
            defenderValueAnimator.start(defenderCenterX, defenderCenterY, defenseValue, panel, false);
        }
        
        battleState.clearPendingValueChanges();
    }

    @Override
    public void onDamageAnimationComplete() {
        updateLabelsFromState();
    }

    @Override
    public void onValueChange(boolean isPlayer, String changeType, int oldValue, int newValue, int delta) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean affectsAttackerValue = (playerIsAttacker == isPlayer && 
            (changeType.equals("ATTACK_LEVEL_UP") || changeType.equals("ATTACK"))) ||
            (playerIsAttacker != isPlayer && 
            (changeType.equals("DEFENSE_LEVEL_UP") || changeType.equals("DEFENSE")));
        
        ValueChangeAnimator targetAnimator = affectsAttackerValue ? attackerValueAnimator : defenderValueAnimator;
        java.awt.Color color = getChangeColor(changeType, delta);
        String displayText = delta >= 0 ? "+" + delta : String.valueOf(delta);
        targetAnimator.queueChange(displayText, color);
    }

    private java.awt.Color getChangeColor(String changeType, int delta) {
        return switch (changeType) {
            case "AWAKENING", "LEVEL_UP", "ATTACK_LEVEL_UP", "DEFENSE_LEVEL_UP" -> ColorHelper.PRISMATIC_GOLD;
            case "WEATHER" -> ColorHelper.WEATHER_BONUS;
            case "PRISMATIC" -> ColorHelper.PRISMATIC_BRIGHT;
            case "PASSIVE" -> delta > 0 ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE;
            default -> delta >= 0 ? java.awt.Color.GREEN : java.awt.Color.RED;
        };
    }

    @Override
    public void advance(float amount) {
        if (battleState == null) return;

        updateRoleTransition(amount);
        updateSelectionDisplayLabels();
        updateConfirmedSelectionLabels();
        updateIconValueLabels();

        if (battleController != null) {
            battleController.advanceAiSelection(amount);
        }

        if (battleState.getCurrentPhase() == Phase.RESOLVING_PRE_CLASH) {
            preClashTimer += amount;
            
            if (attackerValueAnimator != null) {
                attackerValueAnimator.advance(amount);
            }
            if (defenderValueAnimator != null) {
                defenderValueAnimator.advance(amount);
            }
            
            if (preClashTimer >= PRE_CLASH_DELAY) {
                boolean attackerComplete = attackerValueAnimator == null || attackerValueAnimator.isComplete();
                boolean defenderComplete = defenderValueAnimator == null || defenderValueAnimator.isComplete();
                
                if (attackerComplete && defenderComplete) {
                    preClashTimer = 0f;
                    if (battleController != null) {
                        battleController.proceedToClash();
                    }
                }
            }
            return;
        }

        if (diceAnimating) {
            rollAnimationDelay -= amount;
            
            boolean hasPlayerAnimators = diceRollManager.hasAnimators();
            boolean hasOpponentAnimators = diceRollManager.hasOpponentAnimators();
            
            if (hasPlayerAnimators || hasOpponentAnimators) {
                boolean playerComplete = !hasPlayerAnimators || diceRollManager.isComplete();
                boolean opponentComplete = !hasOpponentAnimators || diceRollManager.isOpponentComplete();
                
                if (playerComplete && opponentComplete && rollAnimationDelay <= 0f) {
                    diceAnimating = false;
                    updatePrismaticRolledLabel();
                    List<DiceType> playerTypes = battleState.getPlayerDiceTypes();
                    if (playerTypes != null && !playerTypes.isEmpty() && hasPlayerAnimators) {
                        createDiceHitboxes(playerTypes);
                    }
                    if (battleState.getCurrentPhase() == Phase.ROLLING) {
                        dicePreviewActive = true;
                        dicePreviewDelay = CosmiconConfig.DICE_PREVIEW_DELAY;
                    }
                }
            } else if (rollAnimationDelay <= 0f) {
                diceAnimating = false;
                
                boolean isDefenderRolling = battleState.isDefenderRolling();
                boolean showPlayerDice = isDefenderRolling != battleState.isPlayerAttacker();
                boolean anyDiceStarted = false;
                
                CosmiconLogger.debug("[ANIM] Starting stationary preview - isDefenderRolling=%s, showPlayerDice=%s, playerIsAttacker=%s",
                    isDefenderRolling, showPlayerDice, battleState.isPlayerAttacker());
                
                if (showPlayerDice) {
                    List<DiceType> types = battleState.getPlayerDiceTypes();
                    List<Integer> values = battleState.getPlayerDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        diceRollManager.startStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
                        waitingForClickToRoll = true;
                        anyDiceStarted = true;
                        CosmiconLogger.debug("[ANIM] Player stationary preview started - %d dice, waiting for click", types.size());
                    }
                } else {
                    List<DiceType> types = battleState.getOpponentDiceTypes();
                    List<Integer> values = battleState.getOpponentDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
                        opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
                        anyDiceStarted = true;
                        CosmiconLogger.debug("[ANIM] Opponent stationary preview started - %d dice, auto-roll in %.1fs", types.size(), OPPONENT_AUTO_ROLL_DELAY);
                    }
                }

                if (!anyDiceStarted && battleState.getCurrentPhase() == Phase.ROLLING) {
                    if (isDefenderRolling) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }
                }
            }
        }

        if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
            if (clickHintLabel != null) {
                clickHintLabel.setText(Strings.get("battle.click_to_roll"));
                clickHintLabel.setOpacity(0.8f);
            }
        }

        if (opponentAutoRollDelay > 0f) {
            opponentAutoRollDelay -= amount;
            if (opponentAutoRollDelay <= 0f && diceRollManager.isOpponentWaitingForRollTrigger()) {
                diceRollManager.triggerOpponentRollFromStationary();
                opponentDiceAnimating = true;
                opponentRollDelay = DiceAnimator.getTotalDuration() + 0.1f;
                CosmiconLogger.debug("[ANIM] Opponent auto-roll triggered");
            }
        }

        if (dicePreviewActive) {
            dicePreviewDelay -= amount;
            if (dicePreviewDelay <= 0f) {
                dicePreviewActive = false;
                if (battleState.isDefenderRolling()) {
                    battleController.advanceToDefenderSelectPhase();
                } else {
                    battleController.advanceToSelectPhase();
                }
            }
        }

        if (opponentDiceAnimating) {
            opponentRollDelay -= amount;
            
            if (diceRollManager.hasOpponentAnimators()) {
                if (diceRollManager.isOpponentComplete() && opponentRollDelay <= 0f) {
                    opponentDiceAnimating = false;
                }
            } else if (opponentRollDelay <= 0f) {
                startOpponentDiceAnimation();
            }
        }

        diceRollManager.advance(amount);
        
        if (attackerValueAnimator != null) {
            attackerValueAnimator.advance(amount);
        }
        if (defenderValueAnimator != null) {
            defenderValueAnimator.advance(amount);
        }
        
        if (valueAnimationPending) {
            boolean attackerComplete = attackerValueAnimator == null || attackerValueAnimator.isComplete();
            boolean defenderComplete = defenderValueAnimator == null || defenderValueAnimator.isComplete();
            
            if (attackerComplete && defenderComplete) {
                valueAnimationPending = false;
                startDamageResolutionAnimation(battleState, pendingDamageResult);
            }
        }
        
        if (damageAnimator != null) {
            damageAnimator.advance(amount);
            
            if (!damageAnimator.isComplete()) {
                clickHintLabel.setText(Strings.get("battle.click_to_continue"));
                clickHintLabel.setOpacity(0.6f);
            } else {
                clickHintLabel.setOpacity(0f);
            }
            
            if (damageAnimator.isComplete() && damageAnimationPending) {
                damageAnimationPending = false;
                damageAnimator.cleanup();
                damageAnimator = null;
                clickHintLabel.setOpacity(0f);
                
                if (battleState != null) {
                    battleState.notifyDamageAnimationComplete();
                }
            }
        } else if (clickHintLabel != null) {
            clickHintLabel.setOpacity(0f);
        }
        
        if (prismaticPopupActive && prismaticPopup != null) {
            prismaticPopup.advance(amount);
        }

        PositionAPI pos = panel.getPosition();
        panelX = pos.getX();
        panelY = pos.getY();

        handleMouseInput();
    }

    private void updateRoleTransition(float amount) {
        boolean currentPlayerIsAttacker = battleState.isPlayerAttacker();
        
        if (currentPlayerIsAttacker != lastPlayerWasAttacker) {
            lastPlayerWasAttacker = currentPlayerIsAttacker;
            targetRoleTransition = currentPlayerIsAttacker ? 0f : 1f;
        }

        if (roleTransitionProgress != targetRoleTransition) {
            float diff = targetRoleTransition - roleTransitionProgress;
            float step = Math.abs(diff) / ROLE_TRANSITION_DURATION * amount;
            
            if (Math.abs(diff) <= step) {
                roleTransitionProgress = targetRoleTransition;
            } else {
                roleTransitionProgress += diff > 0 ? step : -step;
            }
        }
        
        cachedRotationAngle = getSmoothedTransition() * 180f;
    }
    
    private float getSmoothedTransition() {
        float t = roleTransitionProgress;
        return t * t * (3f - 2f * t);
    }

    private boolean shouldShowOpponentDice() {
        if (battleState == null) return false;
        
        BattleState.Phase phase = battleState.getCurrentPhase();
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();
        
        boolean aiIsSelecting = (phase == BattleState.Phase.SELECTING_ATTACK && !battleState.isPlayerAttacker()) ||
                                (phase == BattleState.Phase.SELECTING_DEFENSE && battleState.isPlayerAttacker());
        
        boolean vizActive = viz != null && viz.hasStarted();
        
        boolean opponentRolling = phase == BattleState.Phase.ROLLING && 
                                  diceRollManager != null && 
                                  diceRollManager.hasOpponentAnimators();
        
        return aiIsSelecting || vizActive || opponentRolling;
    }

private void handleMouseInput() {
        int currentButton = Mouse.isButtonDown(0) ? 1 : 0;
        
        if (currentButton == 1 && lastMouseButtonState == 0) {
            CosmiconLogger.debug("[CLICK] Mouse clicked - Phase: %s, panelX=%.0f, panelY=%.0f",
                battleState.getCurrentPhase().name(), panelX, panelY);

            int mouseX = Mouse.getX();
            int mouseY = Mouse.getY();
            float[] uiPos = CoordHelper.mouseToPanelUi(mouseX, mouseY,
                panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT);
            float panelUiX = uiPos[0];
            float panelUiY = uiPos[1];
            
            if (!prismaticPopupActive) {
                boolean playerShouldSelect = (battleState.isAttacker(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                    (battleState.isDefender(true) &&
                    battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);
                
                int uses = battleState.getPlayerPrismaticUses();
                
                if (playerShouldSelect && uses > 0) {
                    boolean insidePrismatic = CoordHelper.isInsideUiRect(panelUiX, panelUiY,
                        playerPrismaticBtnX, playerPrismaticBtnY, PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
                    
                    if (insidePrismatic) {
                        CosmiconLogger.info("Player clicked Prismatic button via processInput");
                        showPrismaticSelectionPopup();
                        lastMouseButtonState = currentButton;
                        return;
                    }
                }
            }
            
            if (battleState.getCurrentPhase() == Phase.ROLLING) {
                if (waitingForClickToRoll && diceRollManager.isWaitingForRollTrigger()) {
                    CosmiconLogger.info("Player clicked to trigger dice roll");
                    diceRollManager.triggerRollFromStationary();
                    waitingForClickToRoll = false;
                    diceAnimating = true;
                    rollAnimationDelay = DiceAnimator.getTotalDuration() + 0.1f;
                    if (clickHintLabel != null) {
                        clickHintLabel.setOpacity(0f);
                    }
                } else {
                    CosmiconLogger.debug("Player clicked to skip dice roll animation");
                    diceRollManager.forceCompleteAll();
                    rollAnimationDelay = 0f;
                    dicePreviewDelay = 0f;
                    dicePreviewActive = false;
                    diceAnimating = false;
                    waitingForClickToRoll = false;
                    if (clickHintLabel != null) {
                        clickHintLabel.setOpacity(0f);
                    }
                    
                    boolean showPlayerDice = battleState.isDefenderRolling() != battleState.isPlayerAttacker();
                    if (showPlayerDice && diceHitboxes.isEmpty()) {
                        List<DiceType> types = battleState.getPlayerDiceTypes();
                        if (types != null && !types.isEmpty()) {
                            createDiceHitboxes(types);
                        }
                    }
                    
                    updatePrismaticRolledLabel();
                    
                    if (battleState.isDefenderRolling()) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }
                }
                
                lastMouseButtonState = currentButton;
                return;
            }
            
            if (damageAnimator != null) {
                CosmiconLogger.debug("Player clicked to skip damage animation");
                damageAnimator.forceComplete();
                lastMouseButtonState = currentButton;
                return;
            }
            
            if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
                battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) {
                CosmiconLogger.debug("[CLICK] Wrong phase for dice selection: %s", battleState.getCurrentPhase().name());
                lastMouseButtonState = currentButton;
                return;
            }

            boolean playerShouldSelect = (battleState.isAttacker(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
                                          (battleState.isDefender(true) &&
                                           battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);

            if (!playerShouldSelect) {
                CosmiconLogger.debug("[CLICK] Player should not select");
                lastMouseButtonState = currentButton;
                return;
            }

            CosmiconLogger.debug("[CLICK] Mouse screen: (%d, %d), Panel UI: (%.0f, %.0f), hitboxCount=%d",
                mouseX, mouseY, panelUiX, panelUiY, diceHitboxes.size());

            for (int i = 0; i < diceHitboxes.size(); i++) {
                float[] hb = diceHitboxes.get(i);
                boolean inside = CoordHelper.isInsideUiRect(panelUiX, panelUiY, hb[0], hb[1], hb[2], hb[3]);
                CosmiconLogger.debug("[CLICK] Hitbox %d: UI(%.0f,%.0f) size(%.0f,%.0f) - point (%.0f,%.0f) inside=%s",
                    i, hb[0], hb[1], hb[2], hb[3], panelUiX, panelUiY, inside);
                if (inside) {
                    logDiceSelection(i);
                    battleController.onPlayerSelectDice(i);
                    updateButtons(battleState.getCurrentPhase());
                    updateSelectionDisplayLabels();
                    break;
                }
            }
        }
        lastMouseButtonState = currentButton;
    }

    private void logDiceSelection(int diceIndex) {
        List<DiceType> types = battleState.getPlayerDiceTypes();
        List<Integer> values = battleState.getPlayerDiceValues();
        List<Boolean> selected = battleState.getPlayerDiceSelected();
        
        if (types == null || values == null || diceIndex >= types.size()) {
            CosmiconLogger.debug("Player clicked dice %d", diceIndex);
            return;
        }
        
        DiceType type = types.get(diceIndex);
        int value = values.get(diceIndex);
        boolean wasSelected = selected != null && diceIndex < selected.size() && selected.get(diceIndex);
        String action = wasSelected ? "deselected" : "selected";
        
        CosmiconLogger.info("Player %s dice %d (%s, value: %d)", action, diceIndex, type.name(), value);
    }

    private void createDiceHitboxes(List<DiceType> types) {
        diceHitboxes.clear();
        int count = types.size();
        float maxDiceSize = 80f;
        float hbSize = maxDiceSize + DICE_CLICK_PADDING * 2;
        
        int animatorCount = diceRollManager.getAnimatorCount();
        
        for (int i = 0; i < count; i++) {
            float x, y;
            
            if (i < animatorCount) {
                x = diceRollManager.getAnimatorVisualX(i);
                y = diceRollManager.getAnimatorVisualY(i);
            } else {
                float totalWidth = DICE_SPACING * (count - 1) + maxDiceSize;
                x = diceZoneCenterX - totalWidth / 2f + i * DICE_SPACING;
                y = diceZoneCenterY - maxDiceSize / 2f;
            }
            
            if (x < 0 || y < 0) {
                float totalWidth = DICE_SPACING * (count - 1) + maxDiceSize;
                x = diceZoneCenterX - totalWidth / 2f + i * DICE_SPACING;
                y = diceZoneCenterY - maxDiceSize / 2f;
            }
            
            diceHitboxes.add(new float[]{x, y, hbSize, hbSize});
        }
    }

    private void startOpponentDiceAnimation() {
        if (battleState == null || diceRollManager == null) return;
        
        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();
        
        if (types == null || values == null || types.isEmpty()) {
            opponentDiceAnimating = false;
            return;
        }
        
        diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
        opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
    }

    public void triggerOpponentDiceRoll() {
        if (battleState == null || diceRollManager == null) return;
        
        List<DiceType> types = battleState.getOpponentDiceTypes();
        List<Integer> values = battleState.getOpponentDiceValues();
        
        if (types != null && values != null && !types.isEmpty()) {
            diceRollManager.startOpponentStationaryPreview(types, values, diceZoneCenterX, diceZoneCenterY);
            opponentAutoRollDelay = OPPONENT_AUTO_ROLL_DELAY;
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (battleState == null || diceRollManager == null) return;
        
        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GLStateUtil.resetBlendState();

        boolean hideRoleIcons = damageAnimator != null && damageAnimator.isIconClashActive();

        BattleRenderingUtils.renderBattleBackground(x, y, w, h, 
            cachedRotationAngle, alphaMult, battleState != null, hideRoleIcons);

        renderPlayerCard(x, y, alphaMult);
        renderOpponentCard(x, y, alphaMult);
        renderDiceZone(x, y, alphaMult);
        
        if (attackerValueAnimator != null && !attackerValueAnimator.isComplete()) {
            attackerValueAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }
        if (defenderValueAnimator != null && !defenderValueAnimator.isComplete()) {
            defenderValueAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }

        diceRollManager.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        
        if (damageAnimator != null && !damageAnimator.isComplete()) {
            damageAnimator.render(panelX, panelY, BattleRenderingUtils.PANEL_HEIGHT, alphaMult);
        }

        if (shouldShowOpponentDice()) {
            renderOpponentDiceZone(alphaMult);
        }

        renderDiceSelectionHighlights(alphaMult);
        renderOpponentPrismaticButton(alphaMult);
        renderPrismaticButton(alphaMult);

        if (prismaticPopupActive && prismaticPopup != null) {
            renderPrismaticPopup(alphaMult);
        }

        GLStateUtil.resetColor();
    }

    private void renderPlayerCard(float panelX, float panelY, float alphaMult) {
        float cardX = panelX + BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float cardY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN,
            BattleRenderingUtils.CARD_HEIGHT);

        if (battleState != null) {
            CharacterCard card = battleState.getPlayerCard();
            BattleRenderingUtils.renderCharacterCard(cardX, cardY, card, alphaMult);

            float passiveX = cardX - 20;
            float passiveY = cardY + BattleRenderingUtils.CARD_HEIGHT + 10;
            BattleRenderingUtils.renderPassiveBox(passiveX, passiveY, 
                BattleRenderingUtils.CARD_WIDTH + 40, 60, alphaMult);
        } else {
            Color playerCardColor = ColorHelper.PLAYER_CARD_PLACEHOLDER;
            BattleRenderingUtils.renderCardPlaceholder(cardX, cardY, BattleRenderingUtils.CARD_WIDTH, 
                BattleRenderingUtils.CARD_HEIGHT, playerCardColor, alphaMult);
        }
    }

    private void renderOpponentCard(float panelX, float panelY, float alphaMult) {
        float cardX = panelX + BattleRenderingUtils.MARGIN;
        float cardY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            BattleRenderingUtils.MARGIN, BattleRenderingUtils.CARD_HEIGHT);

        if (battleState != null) {
            CharacterCard card = battleState.getOpponentCard();
            BattleRenderingUtils.renderCharacterCard(cardX, cardY, card, alphaMult);

            float passiveX = cardX - 20;
            float passiveY = cardY - 70;
            BattleRenderingUtils.renderPassiveBox(passiveX, passiveY, 
                BattleRenderingUtils.CARD_WIDTH + 40, 60, alphaMult);
        } else {
            Color opponentCardColor = ColorHelper.OPPONENT_CARD_PLACEHOLDER;
            BattleRenderingUtils.renderCardPlaceholder(cardX, cardY, BattleRenderingUtils.CARD_WIDTH, 
                BattleRenderingUtils.CARD_HEIGHT, opponentCardColor, alphaMult);
        }
    }

    private void renderDiceZone(float panelX, float panelY, float alphaMult) {
        float zoneW = 400f;
        float zoneH = 160f;
        float zoneX = panelX + (BattleRenderingUtils.PANEL_WIDTH - zoneW) / 2f;
        float zoneY = panelY + (BattleRenderingUtils.PANEL_HEIGHT - zoneH) / 2f - 40f;

        BattleRenderingUtils.renderDiceZone(zoneX, zoneY, zoneW, zoneH, alphaMult);
    }

    private void renderDiceSelectionHighlights(float alphaMult) {
        if (battleState == null) return;
        if (battleState.getCurrentPhase() != Phase.SELECTING_ATTACK &&
            battleState.getCurrentPhase() != Phase.SELECTING_DEFENSE) return;

        List<Boolean> selected = battleState.getPlayerDiceSelected();
        if (selected == null) return;

        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(ColorHelper.SELECTION_HIGHLIGHT, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);

        for (int i = 0; i < Math.min(selected.size(), diceHitboxes.size()); i++) {
            if (selected.get(i)) {
                float[] hb = diceHitboxes.get(i);
                float hx = panelX + hb[0];
                float hy = CoordHelper.uiToGlY(panelY, BattleRenderingUtils.PANEL_HEIGHT, hb[1] + hb[3] - DICE_CLICK_PADDING * 2);
                float hw = hb[2] - DICE_CLICK_PADDING * 2;
                float hh = hb[3] - DICE_CLICK_PADDING * 2;

                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex2f(hx, hy);
                GL11.glVertex2f(hx + hw, hy);
                GL11.glVertex2f(hx + hw, hy + hh);
                GL11.glVertex2f(hx, hy + hh);
                GL11.glEnd();
            }
        }

        GLStateUtil.resetColor();
    }

    private void renderOpponentDiceZone(float alphaMult) {
        float zoneX = panelX + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_OFFSET_X;
        float zoneY = panelY + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_Y_OFFSET;
        
        BattleRenderingUtils.renderOpponentDiceZone(zoneX, zoneY, alphaMult);
        
        diceRollManager.renderOpponentDice(panelX, panelY, alphaMult);
        
        renderOpponentSelectionHighlights(alphaMult);
    }

    private void renderOpponentSelectionHighlights(float alphaMult) {
        if (battleState == null) return;
        
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();
        if (viz == null || !viz.hasStarted() || viz.isRerollPhase()) return;
        
        List<Integer> visibleIndices = viz.getVisibleIndices();
        if (visibleIndices.isEmpty()) return;
        
        List<DiceAnimator> opponentAnimators = diceRollManager.getOpponentAnimators();
        if (opponentAnimators == null || opponentAnimators.isEmpty()) return;
        
        GLStateUtil.resetBlendState();
        
        Color highlightColor = viz.isRerollPhase() ? 
            ColorHelper.OPPONENT_REROLL_HIGHLIGHT : 
            ColorHelper.OPPONENT_SELECTION_HIGHLIGHT;
        
        float[] c = ColorHelper.toGLComponents(highlightColor, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);
        
        for (int idx : visibleIndices) {
            if (idx >= 0 && idx < opponentAnimators.size()) {
                DiceAnimator animator = opponentAnimators.get(idx);
                if (animator != null) {
                    float diceSize = animator.getDisplaySize();
                    float diceX = panelX + animator.getVisualX();
                    float diceY = CoordHelper.uiToGlY(panelY, BattleRenderingUtils.PANEL_HEIGHT, 
                        animator.getVisualY() + diceSize);
                    
                    GL11.glBegin(GL11.GL_LINE_LOOP);
                    GL11.glVertex2f(diceX, diceY);
                    GL11.glVertex2f(diceX + diceSize, diceY);
                    GL11.glVertex2f(diceX + diceSize, diceY + diceSize);
                    GL11.glVertex2f(diceX, diceY + diceSize);
                    GL11.glEnd();
                }
            }
        }
        
        GLStateUtil.resetColor();
    }

    private void renderOpponentPrismaticButton(float alphaMult) {
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        float renderX = panelX + opponentPrismaticBtnX;
        float renderY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            opponentPrismaticBtnY, PRISMATIC_BTN_SIZE);

        int uses = battleState.getOpponentPrismaticUses();
        float btnAlpha = uses > 0 ? alphaMult : alphaMult * 0.4f;

        sprite.setSize(PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
        sprite.setAlphaMult(btnAlpha);
        sprite.render(renderX, renderY);

        GLStateUtil.disableTexturing();
    }

    private void renderPrismaticButton(float alphaMult) {
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        float renderX = panelX + playerPrismaticBtnX;
        float renderY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            playerPrismaticBtnY, PRISMATIC_BTN_SIZE);

        int uses = battleState.getPlayerPrismaticUses();
        boolean playerShouldSelect = (battleState.isAttacker(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_ATTACK) ||
            (battleState.isDefender(true) &&
            battleState.getCurrentPhase() == Phase.SELECTING_DEFENSE);
        boolean prismaticEnabled = uses > 0 && playerShouldSelect && !prismaticPopupActive;

        float btnAlpha = alphaMult;
        if (!prismaticEnabled) {
            btnAlpha *= 0.4f;
        }

        sprite.setSize(PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
        sprite.setAlphaMult(btnAlpha);
        sprite.render(renderX, renderY);

        GLStateUtil.disableTexturing();
    }

    private void renderPrismaticPopup(float alphaMult) {
        if (prismaticPopup == null) return;

        prismaticPopup.renderBelow(alphaMult);

        GLStateUtil.resetColor();
    }
}

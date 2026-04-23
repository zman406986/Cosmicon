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
import data.scripts.cosmicon.battle.DamageResolver.DamageResult;
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
    private LabelAPI prismaticUsesLabel;
    private LabelAPI prismaticFaceMappingLabel;
    private LabelAPI prismaticEffectLabel;
    private LabelAPI prismaticRolledLabel;
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

    private float panelX;
    private float panelY;
    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float rollAnimationDelay;
    private boolean diceAnimating;
    private boolean dicePreviewActive;
    private float dicePreviewDelay;

    private boolean opponentDiceAnimating;
    private float opponentRollDelay;

    private float roleTransitionProgress;
    private float targetRoleTransition;
    private static final float ROLE_TRANSITION_DURATION = 0.4f;
    private boolean lastPlayerWasAttacker;

    private int lastMouseButtonState;

    private float prismaticBtnX;
    private float prismaticBtnY;
    private PrismaticDiceInstance pendingPrismaticInstance;
    private int pendingPrismaticAnimatorIndex = -1;

    private final List<float[]> diceHitboxes;
    
    private DamageResolutionAnimator damageAnimator;
    private DamageResolver.DamageResult pendingDamageResult;
    private boolean damageAnimationPending;
    
    private ValueChangeAnimator attackerValueAnimator;
    private ValueChangeAnimator defenderValueAnimator;
    private LabelAPI attackerValueLabel;
    private LabelAPI defenderValueLabel;
    private boolean valueAnimationPending;

    public BattlePanelUI() {
        this.diceHitboxes = new ArrayList<>();
        this.lastMouseButtonState = 0;
        this.roleTransitionProgress = 0f;
        this.targetRoleTransition = 0f;
        this.lastPlayerWasAttacker = true;
        this.opponentDiceAnimating = false;
        this.opponentRollDelay = 0f;
        this.dicePreviewActive = false;
        this.dicePreviewDelay = 0f;
        this.valueAnimationPending = false;
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

        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.MARGIN;

        prismaticBtnX = playerCardX - 60f;
        prismaticBtnY = playerCardY + 40f;

        prismaticUsesLabel = settings.createLabel("2", Fonts.DEFAULT_SMALL);
        prismaticUsesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        prismaticUsesLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) prismaticUsesLabel)
            .setSize(40, 20)
            .inTL(playerCardX - 55f, playerCardY + 85f);

        prismaticFaceMappingLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        prismaticFaceMappingLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        prismaticFaceMappingLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) prismaticFaceMappingLabel)
            .setSize(180, 20)
            .inTL(prismaticBtnX + PRISMATIC_FACE_MAPPING_OFFSET_X, prismaticBtnY);
        prismaticFaceMappingLabel.setOpacity(0f);

        prismaticEffectLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        prismaticEffectLabel.setColor(Color.LIGHT_GRAY);
        prismaticEffectLabel.setAlignment(Alignment.LMID);
        panel.addComponent((UIComponentAPI) prismaticEffectLabel)
            .setSize(180, 20)
            .inTL(prismaticBtnX + PRISMATIC_FACE_MAPPING_OFFSET_X, prismaticBtnY + 20f);
        prismaticEffectLabel.setOpacity(0f);

        prismaticRolledLabel = settings.createLabel("", Fonts.DEFAULT_SMALL);
        prismaticRolledLabel.setColor(ColorHelper.PRISMATIC_BRIGHT);
        prismaticRolledLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) prismaticRolledLabel)
            .setSize(60, 20);
        prismaticRolledLabel.setOpacity(0f);

        TooltipMakerAPI exitTp = panel.createUIElement(btnWidth, btnHeight, false);
        exitTp.setActionListenerDelegate(this);
        panel.addUIElement(exitTp).inTL(BattleRenderingUtils.PANEL_WIDTH - btnWidth - 10f, 10f);
        ButtonAPI exitButton = exitTp.addButton("Exit", ACTION_EXIT, btnWidth, btnHeight, 0f);
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

        attackerValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
        attackerValueLabel.setColor(ColorHelper.ATTACK_VALUE);
        attackerValueLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) attackerValueLabel)
            .setSize(60, 40);
        attackerValueLabel.setOpacity(0f);

        defenderValueLabel = settings.createLabel("", Fonts.INSIGNIA_LARGE);
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
    }

    private void updateButtons(Phase phase) {
        if (confirmButton == null || rerollButton == null) return;

        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean playerShouldSelect = (playerIsAttacker && phase == Phase.SELECTING_ATTACK) ||
                                      (!playerIsAttacker && phase == Phase.SELECTING_DEFENSE);

        String confirmText = switch (phase) {
            case SELECTING_ATTACK -> playerIsAttacker ? Strings.get("battle.confirm_attack") : Strings.get("phase.waiting");
            case SELECTING_DEFENSE -> playerIsAttacker ? Strings.get("phase.waiting") : Strings.get("battle.confirm_defense");
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

    private void updatePrismaticButton() {
        if (prismaticUsesLabel == null || battleState == null) return;

        int uses = battleState.getPlayerPrismaticUses();
        prismaticUsesLabel.setText(String.valueOf(uses));

        if (battleState.isPlayerPrismaticModeActive()) {
            prismaticUsesLabel.setColor(ColorHelper.OPPONENT_NAME);
        } else if (uses > 0) {
            prismaticUsesLabel.setColor(ColorHelper.PRISMATIC_GOLD);
        } else {
            prismaticUsesLabel.setColor(ColorHelper.PRISMATIC_DISABLED);
        }
        
        updatePrismaticFaceMappingDisplay();
    }
    
    private void updatePrismaticFaceMappingDisplay() {
        if (prismaticFaceMappingLabel == null || battleState == null) return;
        
        CharacterCard playerCard = battleState.getPlayerCard();
        if (playerCard == null) {
            prismaticFaceMappingLabel.setOpacity(0f);
            prismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        java.util.Map<String, Integer> prismaticIds = playerCard.getPrismaticDiceIds();
        if (prismaticIds == null || prismaticIds.isEmpty()) {
            prismaticFaceMappingLabel.setOpacity(0f);
            prismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        String firstDiceId = prismaticIds.keySet().iterator().next();
        PrismaticDiceType type = PrismaticDiceRegistry.get(firstDiceId);
        if (type == null) {
            prismaticFaceMappingLabel.setOpacity(0f);
            prismaticEffectLabel.setOpacity(0f);
            return;
        }
        
        int uses = battleState.getPlayerPrismaticUses();
        if (uses <= 0) {
            prismaticFaceMappingLabel.setOpacity(0.4f);
            prismaticEffectLabel.setOpacity(0.4f);
        } else {
            prismaticFaceMappingLabel.setOpacity(1f);
            prismaticEffectLabel.setOpacity(1f);
        }
        
        String mappingText = PrismaticFaceDisplay.formatFaceMappingCompact(type, false);
        prismaticFaceMappingLabel.setText(mappingText);
        
        String effectText = PrismaticFaceDisplay.getEffectDescription(type);
        prismaticEffectLabel.setText(effectText);
    }
    
    private void updatePrismaticRolledLabel() {
        if (prismaticRolledLabel == null) return;
        
        if (pendingPrismaticInstance == null || pendingPrismaticAnimatorIndex < 0) {
            prismaticRolledLabel.setOpacity(0f);
            return;
        }
        
        if (diceRollManager == null || pendingPrismaticAnimatorIndex >= diceRollManager.getAnimatorCount()) {
            prismaticRolledLabel.setOpacity(0f);
            return;
        }
        
        float diceX = diceRollManager.getAnimatorVisualX(pendingPrismaticAnimatorIndex);
        float diceY = diceRollManager.getAnimatorVisualY(pendingPrismaticAnimatorIndex);
        
        String rolledText = PrismaticFaceDisplay.formatRolledResult(pendingPrismaticInstance);
        prismaticRolledLabel.setText(rolledText);
        prismaticRolledLabel.setOpacity(1f);
        
        float labelWidth = 60f;
        prismaticRolledLabel.getPosition().inTL(diceX + DiceAnimator.DICE_SIZE / 2f - labelWidth / 2f, 
                                                 diceY + DiceAnimator.DICE_SIZE + PRISMATIC_ROLLED_LABEL_OFFSET_Y);
    }
    
    private void clearPrismaticRolledLabel() {
        if (prismaticRolledLabel != null) {
            prismaticRolledLabel.setOpacity(0f);
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
                boolean isDefenderRolling = battleState.isDefenderRolling();
                boolean showPlayerDice = isDefenderRolling != battleState.isPlayerAttacker();
                if (showPlayerDice) {
                    diceRollManager.clearOpponentAnimators();
                } else {
                    diceRollManager.clear();
                }
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
            
            createDiceHitboxes(types);
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
        
        float bottomIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float topIconCenterX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float topIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        
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

        if (battleController != null) {
            battleController.advanceAiSelection(amount);
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
                boolean anyDiceRolled = false;
                
                CosmiconLogger.debug("[ANIM] Starting animation - isDefenderRolling=%s, showPlayerDice=%s, playerIsAttacker=%s",
                    isDefenderRolling, showPlayerDice, battleState.isPlayerAttacker());
                
                if (showPlayerDice) {
                    List<DiceType> types = battleState.getPlayerDiceTypes();
                    List<Integer> values = battleState.getPlayerDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        createDiceHitboxes(types);
                        diceRollManager.startRoll(types, values, diceZoneCenterX, diceZoneCenterY);
                        anyDiceRolled = true;
                        CosmiconLogger.debug("[ANIM] Player dice animation started - %d dice", types.size());
                    }
                } else {
                    List<DiceType> types = battleState.getOpponentDiceTypes();
                    List<Integer> values = battleState.getOpponentDiceValues();
                    if (types != null && values != null && !types.isEmpty()) {
                        float zoneX = panelX + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_OFFSET_X;
                        float zoneY = panelY + BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_Y_OFFSET;
                        diceRollManager.startOpponentRoll(types, values, zoneX, zoneY);
                        anyDiceRolled = true;
                        CosmiconLogger.debug("[ANIM] Opponent dice animation started - %d dice (opponent zone)", types.size());
                    }
                }

                if (anyDiceRolled) {
                    rollAnimationDelay = DiceAnimator.getTotalDuration() + 0.1f;
                    diceAnimating = true;
                } else if (battleState.getCurrentPhase() == Phase.ROLLING) {
                    if (isDefenderRolling) {
                        battleController.advanceToDefenderSelectPhase();
                    } else {
                        battleController.advanceToSelectPhase();
                    }
                }
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
            
            if (damageAnimator.isWaitingForClick()) {
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
                        prismaticBtnX, prismaticBtnY, PRISMATIC_BTN_SIZE, PRISMATIC_BTN_SIZE);
                    
                    if (insidePrismatic) {
                        CosmiconLogger.info("Player clicked Prismatic button via processInput");
                        showPrismaticSelectionPopup();
                        lastMouseButtonState = currentButton;
                        return;
                    }
                }
            }
            
            if (battleState.getCurrentPhase() == Phase.ROLLING) {
                CosmiconLogger.debug("Player clicked to skip dice roll animation");
                diceRollManager.forceCompleteAll();
                
                boolean showPlayerDice = battleState.isDefenderRolling() != battleState.isPlayerAttacker();
                if (showPlayerDice && diceHitboxes.isEmpty()) {
                    List<DiceType> types = battleState.getPlayerDiceTypes();
                    if (types != null && !types.isEmpty()) {
                        createDiceHitboxes(types);
                    }
                }
                
                lastMouseButtonState = currentButton;
                return;
            }
            
            if (damageAnimator != null && damageAnimator.isWaitingForClick()) {
                CosmiconLogger.debug("Player clicked to skip damage animation pause");
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
                if (i == 0) {
                    x = diceZoneCenterX - maxDiceSize / 2f;
                } else if (i - 1 < animatorCount) {
                    x = diceRollManager.getAnimatorVisualX(i - 1) + DICE_SPACING;
                } else {
                    float[] lastHb = diceHitboxes.get(i - 1);
                    x = lastHb[0] + DICE_SPACING;
                }
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
        
        float zoneX = BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_OFFSET_X;
        float zoneY = BattleRenderingUtils.MARGIN + BattleRenderingUtils.OPPONENT_DICE_ZONE_Y_OFFSET;
        
        diceRollManager.startOpponentRoll(types, values, zoneX, zoneY);
        opponentRollDelay = DiceAnimator.getTotalDuration() + 0.1f;
        opponentDiceAnimating = true;
    }

    public void triggerOpponentDiceRoll() {
        opponentRollDelay = 0.3f;
        opponentDiceAnimating = true;
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

        BattleRenderingUtils.renderBattleBackground(x, y, w, h, 
            roleTransitionProgress, alphaMult, battleState != null);

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
        if (viz == null || !viz.hasStarted()) return;
        
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

    private void renderPrismaticButton(float alphaMult) {
        SpriteAPI sprite = CosmiconSprites.getPrismaticButtonSprite();
        if (sprite == null) return;

        GLStateUtil.enableTexturingWithBlend();

        float renderX = panelX + prismaticBtnX;
        float renderY = CoordHelper.uiTopLeftToGlSpriteY(panelY, BattleRenderingUtils.PANEL_HEIGHT,
            prismaticBtnY, PRISMATIC_BTN_SIZE);

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

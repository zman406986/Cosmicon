package data.scripts.cosmicon.battle;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.ui.PrismaticDiceSelectionPopup;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.UIComponentFactory;

public class BattleUIButtons implements ActionListenerDelegate {
    private static final String ACTION_END_TURN = "end_turn";
    private static final String ACTION_CONTINUE = "continue";
    private static final String ACTION_REROLL = "reroll";
    private static final String ACTION_EXIT = "exit";
    private static final float PASSIVE_BTN_WIDTH = 150f;
    private static final float PASSIVE_BTN_HEIGHT = 25f;

    private CustomPanelAPI panel;
    private DialogCallbacks callbacks;
    private BattleController battleController;
    private BattleState battleState;
    private DiceRollManager diceRollManager;
    private BattleUILabels labels;
    private BattleInputHandler inputHandler;

    private ButtonAPI rerollButton;
    private ButtonAPI confirmButton;
    private boolean buttonsCreated = false;

    private PrismaticDiceSelectionPopup prismaticPopup;
    private CustomPanelAPI prismaticPopupPanel;
    private boolean prismaticPopupActive;

    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float playerPrismaticBtnX;
    private float playerPrismaticBtnY;

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks, BattleController controller,
            BattleState state, DiceRollManager diceRollManager, BattleUILabels labels,
            BattleInputHandler inputHandler, float diceZoneCenterX, float diceZoneCenterY) {
        this.panel = panel;
        this.callbacks = callbacks;
        this.battleController = controller;
        this.battleState = state;
        this.diceRollManager = diceRollManager;
        this.labels = labels;
        this.inputHandler = inputHandler;
        this.diceZoneCenterX = diceZoneCenterX;
        this.diceZoneCenterY = diceZoneCenterY;

        createButtons();
    }

    private void createButtons() {
        if (buttonsCreated) return;

        PositionAPI pos = panel.getPosition();
        TooltipMakerAPI btnTp = UIComponentFactory.createTooltipForButtons(panel, this, pos.getWidth(), pos.getHeight(), 0f, 0f);

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

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;

        float opponentPrismaticBtnX = opponentCardX - 60f;
        float opponentPrismaticBtnY = opponentCardY + 40f;

        TooltipMakerAPI exitTp = UIComponentFactory.createTooltipForButtons(panel, this, btnWidth, btnHeight, 
            BattleRenderingUtils.PANEL_WIDTH - btnWidth - 10f, 10f);
        ButtonAPI exitButton = exitTp.addButton(Strings.get("phase.close"), ACTION_EXIT, btnWidth, btnHeight, 0f);
        exitButton.setQuickMode(true);

        TooltipMakerAPI abilityTp = UIComponentFactory.createTooltipForButtons(panel, this, 
            PASSIVE_BTN_WIDTH, PASSIVE_BTN_HEIGHT, 0f, 0f);

        float opponentCardUiX = BattleRenderingUtils.MARGIN;
        float opponentCardUiY = BattleRenderingUtils.MARGIN;
        float opponentAbilityY = opponentCardUiY + BattleRenderingUtils.CARD_HEIGHT + 15f;

        ButtonAPI opponentAbilityButton = abilityTp.addButton(Strings.get("battle.ability_btn"), "opponent_ability",
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

        ButtonAPI playerAbilityButton = abilityTp.addButton(Strings.get("battle.ability_btn"), "player_ability",
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

        playerPrismaticBtnX = BattleRenderingUtils.PANEL_WIDTH - opponentPrismaticBtnX - 40f;
        playerPrismaticBtnY = BattleRenderingUtils.PANEL_HEIGHT - opponentPrismaticBtnY - 40f;

        buttonsCreated = true;
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

    public void updateButtons(Phase phase) {
        if (confirmButton == null || rerollButton == null || battleState == null) return;

        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean playerShouldSelect = (playerIsAttacker && phase == Phase.SELECTING_ATTACK) ||
                                      (!playerIsAttacker && phase == Phase.SELECTING_DEFENSE);

        String confirmText = switch (phase) {
            case SELECTING_ATTACK -> playerIsAttacker ? Strings.get("battle.confirm_attack") : Strings.get("phase.waiting");
            case SELECTING_DEFENSE -> playerIsAttacker ? Strings.get("phase.waiting") : Strings.get("battle.confirm_defense");
            case RESOLVING_PRE_CLASH, RESOLVING -> Strings.get("phase.resolving");
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
    }

    public void showPrismaticSelectionPopup() {
        prismaticPopupActive = true;
        prismaticPopup = new PrismaticDiceSelectionPopup(battleState, new PrismaticDiceSelectionPopup.PrismaticDiceSelectionCallback() {
            @Override
            public void onPrismaticDiceSelected(PrismaticDiceType type, PrismaticDiceInstance instance) {
                CosmiconLogger.info("Player selected Prismatic dice: %s, face: %d", type.getId(), instance.rolledFace);
                battleState.addPrismaticDiceToPool(instance, true);
                int animatorIndex = diceRollManager.getAnimatorCount();
                labels.setPendingPrismatic(instance, animatorIndex);
                diceRollManager.appendInstantDice(DiceType.PRISMATIC, instance.faceIndex, diceZoneCenterX, diceZoneCenterY);
                inputHandler.createDiceHitboxes(battleState.getPlayerDiceTypes());
                closePrismaticPopup();
                updateButtons(battleState.getCurrentPhase());
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

    public void closePrismaticPopup() {
        prismaticPopupActive = false;
        if (prismaticPopupPanel != null && panel != null) {
            panel.removeComponent(prismaticPopupPanel);
        }
        prismaticPopupPanel = null;
        prismaticPopup = null;
    }

    public void cleanup() {
        if (prismaticPopupPanel != null && panel != null) {
            panel.removeComponent(prismaticPopupPanel);
            prismaticPopupPanel = null;
            prismaticPopup = null;
        }
        prismaticPopupActive = false;
        panel = null;
        callbacks = null;
        battleController = null;
        battleState = null;
        diceRollManager = null;
        labels = null;
        buttonsCreated = false;
    }

    public void setBattleState(BattleState state) {
        this.battleState = state;
    }

    public void advance(float amount) {
        if (prismaticPopupActive && prismaticPopup != null) {
            prismaticPopup.advance(amount);
        }
    }

    public boolean isPrismaticPopupActive() {
        return prismaticPopupActive;
    }

    public PrismaticDiceSelectionPopup getPrismaticPopup() {
        return prismaticPopup;
    }

    public float getPlayerPrismaticBtnX() {
        return playerPrismaticBtnX;
    }

    public float getPlayerPrismaticBtnY() {
        return playerPrismaticBtnY;
    }

    }
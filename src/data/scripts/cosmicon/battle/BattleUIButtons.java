package data.scripts.cosmicon.battle;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.tutorial.TutorialController;
import data.scripts.cosmicon.ui.PrismaticDiceSelectionPopup;
import data.scripts.cosmicon.util.UIComponentFactory;

public class BattleUIButtons implements ActionListenerDelegate {
    private static final String ACTION_END_TURN = "end_turn";
    private static final String ACTION_CONTINUE = "continue";
    private static final String ACTION_REROLL = "reroll";
    private static final String ACTION_EXIT = "exit";
    private static final String ACTION_DEBUG_WIN = "debug_win";
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
    private LabelAPI weatherLabel;
    private LabelAPI weatherDescLabel;
    private LabelAPI weatherTitleLabel;
    private boolean buttonsCreated = false;

    private PrismaticDiceSelectionPopup prismaticPopup;
    private CustomPanelAPI prismaticPopupPanel;
    private boolean prismaticPopupActive;

    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float playerPrismaticBtnX;
    private float playerPrismaticBtnY;
    private TutorialController tutorialController;

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

        float btnWidth = 120f;
        float btnHeight = 30f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float bottomY = BattleRenderingUtils.PANEL_HEIGHT - 60f;

        float btnTpX = centerX - btnWidth - 40f;
        float btnTpY = bottomY - 10f;
        float btnTpW = btnWidth * 2f + 80f;
        float btnTpH = btnHeight + 20f;
        TooltipMakerAPI btnTp = UIComponentFactory.createTooltipForButtons(panel, this, btnTpW, btnTpH, btnTpX, btnTpY);

        confirmButton = btnTp.addButton(Strings.get("battle.confirm_attack"), ACTION_END_TURN,
            btnWidth, btnHeight, 0f);
        confirmButton.setQuickMode(true);
        confirmButton.setShortcut(Keyboard.KEY_SPACE, false);
        confirmButton.getPosition().inTL(20f, 10f);

        rerollButton = btnTp.addButton(Strings.get("phase.reroll_selected"), ACTION_REROLL,
            btnWidth, btnHeight, 0f);
        rerollButton.setQuickMode(true);
        rerollButton.getPosition().inTL(btnWidth + 60f, 10f);

        TooltipMakerAPI exitTp = UIComponentFactory.createTooltipForButtons(panel, this, btnWidth, btnHeight, 
            BattleRenderingUtils.PANEL_WIDTH - btnWidth - 10f, 10f);
        ButtonAPI exitButton = exitTp.addButton(Strings.get("phase.close"), ACTION_EXIT, btnWidth, btnHeight, 0f);
        exitButton.setQuickMode(true);

        float debugBtnWidth = 90f;
        TooltipMakerAPI debugTp = UIComponentFactory.createTooltipForButtons(panel, this, debugBtnWidth, btnHeight,
            BattleRenderingUtils.PANEL_WIDTH - btnWidth - debugBtnWidth - 20f, 10f);
        ButtonAPI debugWinButton = debugTp.addButton("[Win]", ACTION_DEBUG_WIN, debugBtnWidth, btnHeight, 0f);
        debugWinButton.setQuickMode(true);

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

        playerPrismaticBtnX = BattleRenderingUtils.MARGIN + 60f;
        playerPrismaticBtnY = BattleRenderingUtils.PANEL_HEIGHT - 100f;

        createWeatherLabel();

        createStatusTooltips();

        buttonsCreated = true;
    }

    private void createStatusTooltips() {
        float btnWidth = BattleRenderingUtils.STATUS_BOX_WIDTH - 20f;
        float btnHeight = 18f;
        float spacing = 20f;

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        float opponentBoxX = opponentCardX + BattleRenderingUtils.CARD_WIDTH + 20f;
        float opponentBtnX = opponentBoxX + BattleRenderingUtils.STATUS_BOX_PADDING;

        TooltipMakerAPI opponentTp = UIComponentFactory.createTooltipForButtons(panel, this,
            BattleRenderingUtils.STATUS_BOX_WIDTH, BattleRenderingUtils.CARD_HEIGHT,
            opponentBtnX, opponentCardY + BattleRenderingUtils.STATUS_BOX_PADDING);

        for (int i = 0; i < BattleUILabels.MAX_STATUS_EFFECTS; i++) {
            float yOffset = i * spacing;
            ButtonAPI btn = opponentTp.addButton("", "status_opp_" + i, btnWidth, btnHeight, 0f);
            btn.setQuickMode(true);
            btn.setOpacity(0f);
            btn.getPosition().inTL(0f, yOffset);

            final int index = i;
            opponentTp.addTooltipToPrevious(new TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) { return false; }
                @Override
                public float getTooltipWidth(Object tooltipParam) { return 350f; }
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    String desc = getEffectDescriptionAtIndex(index, false);
                    if (desc != null) {
                        tooltip.addPara(desc, 5f);
                    }
                }
            }, TooltipLocation.LEFT, false);
        }

        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        float playerBoxX = playerCardX - BattleRenderingUtils.STATUS_BOX_WIDTH - 20f;
        float playerBtnX = playerBoxX + BattleRenderingUtils.STATUS_BOX_PADDING;

        TooltipMakerAPI playerTp = UIComponentFactory.createTooltipForButtons(panel, this,
            BattleRenderingUtils.STATUS_BOX_WIDTH, BattleRenderingUtils.CARD_HEIGHT,
            playerBtnX, playerCardY + BattleRenderingUtils.STATUS_BOX_PADDING);

        for (int i = 0; i < BattleUILabels.MAX_STATUS_EFFECTS; i++) {
            float yOffset = i * spacing;
            ButtonAPI btn = playerTp.addButton("", "status_plr_" + i, btnWidth, btnHeight, 0f);
            btn.setQuickMode(true);
            btn.setOpacity(0f);
            btn.getPosition().inTL(0f, yOffset);

            final int index = i;
            playerTp.addTooltipToPrevious(new TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) { return false; }
                @Override
                public float getTooltipWidth(Object tooltipParam) { return 350f; }
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    String desc = getEffectDescriptionAtIndex(index, true);
                    if (desc != null) {
                        tooltip.addPara(desc, 5f);
                    }
                }
            }, TooltipLocation.RIGHT, false);
        }
    }

    private void createWeatherLabel() {
        float boxWidth = 170f;
        float boxHeight = 70f;
        float titleHeight = 18f;
        float playerCardUiX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;

        float weatherX = playerCardUiX + 5f;
        float titleY = playerCardY - PASSIVE_BTN_HEIGHT - titleHeight - boxHeight - 15f;
        float boxY = titleY + titleHeight + 2f;

        weatherTitleLabel = UIComponentFactory.createLabelSmall(panel, Strings.get("battle.weather_effects"),
            java.awt.Color.LIGHT_GRAY, com.fs.starfarer.api.ui.Alignment.MID, boxWidth, titleHeight, weatherX, titleY);

        TooltipMakerAPI weatherTp = panel.createUIElement(boxWidth, boxHeight, false);
        panel.addUIElement(weatherTp).inTL(weatherX, boxY);

        weatherLabel = weatherTp.addPara(Strings.get("battle.weather_none"), 0f);
        weatherLabel.setAlignment(com.fs.starfarer.api.ui.Alignment.MID);

        weatherDescLabel = weatherTp.addPara("", java.awt.Color.GRAY, 3f);
        weatherDescLabel.setAlignment(com.fs.starfarer.api.ui.Alignment.MID);

        updateWeatherLabel();
    }

    public void updateWeatherLabel() {
        if (weatherLabel == null || battleState == null) return;
        WeatherController wc = battleState.getWeatherController();
        if (wc == null) {
            weatherLabel.setText(Strings.get("battle.weather_none"));
            return;
        }
        WeatherType weather = wc.getCurrentWeather();
        if (weather == null) {
            weatherLabel.setText(Strings.get("battle.weather_none"));
        } else {
            weatherLabel.setText(Strings.get("weather." + weather.name().toLowerCase()));
        }
    }

    private String getEffectDescriptionAtIndex(int targetIndex, boolean forPlayer) {
        if (battleState == null) return null;
        StatusEffectProcessor effects = battleState.getEffects(forPlayer);
        int idx = 0;
        for (StatusEffect effect : StatusEffect.values()) {
            if (effects.getLayers(effect) > 0) {
                if (idx == targetIndex) {
                    String key = "status_desc." + effect.name().toLowerCase() + "_desc";
                    try {
                        return Strings.get(key);
                    } catch (Exception e) {
                        return null;
                    }
                }
                idx++;
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            String action = (String) btn.getCustomData();

            switch (action) {
                case ACTION_REROLL -> {
                    if (battleController != null) {
                        if (tutorialController != null && !tutorialController.isRerollAllowed()) {
                            break;
                        }
                        battleController.onPlayerReroll();
                        if (tutorialController != null) {
                            tutorialController.onRerolled();
                        }
                    }
                }
                case ACTION_END_TURN -> {
                    if (battleController != null) {
                        if (tutorialController != null && !tutorialController.isConfirmAllowed()) {
                            break;
                        }
                        if (tutorialController != null) {
                            tutorialController.onConfirmed();
                        }
                        battleController.onPlayerConfirmSelection();
                    }
                }
                case ACTION_CONTINUE -> {
                    if (battleController != null) {
                        if (tutorialController != null && tutorialController.isContinueAllowed()) {
                            break;
                        }
                        if (tutorialController != null) {
                            tutorialController.onContinueClicked();
                        }
                        battleController.onContinueToNextTurn();
                    }
                }
                case "close", ACTION_EXIT -> {
                    if (callbacks != null) {
                        callbacks.dismissDialog();
                    }
                }
                case ACTION_DEBUG_WIN -> {
                    if (battleController != null && battleState.getCurrentPhase() != Phase.ENDED) {
                        battleController.forcePlayerWin();
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
        if (tutorialController != null) {
            canConfirm = canConfirm && tutorialController.isConfirmAllowed();
        }
        boolean canContinue = phase == Phase.WAITING_NEXT_TURN || phase == Phase.ENDED;
        if (tutorialController != null && tutorialController.isContinueAllowed()) {
            canContinue = false;
        }
        confirmButton.setEnabled(canConfirm || canContinue);

        boolean canReroll = playerShouldSelect && selectedCount > 0 && hasRerolls;
        if (tutorialController != null) {
            canReroll = canReroll && tutorialController.isRerollAllowed();
        }
        rerollButton.setEnabled(canReroll);
    }

    public void showPrismaticSelectionPopup() {
        prismaticPopupActive = true;
        prismaticPopup = new PrismaticDiceSelectionPopup(battleState, new PrismaticDiceSelectionPopup.PrismaticDiceSelectionCallback() {
            @Override
            public void onPrismaticDiceSelected(PrismaticDiceType type, PrismaticDiceInstance instance) {
                battleState.addPrismaticDiceToPool(instance, true);
                int animatorIndex = diceRollManager.getAnimatorCount();
                labels.setPendingPrismatic(instance, animatorIndex);
                diceRollManager.appendInstantDice(DiceType.PRISMATIC, instance.faceIndex, diceZoneCenterX, diceZoneCenterY);
                inputHandler.createDiceHitboxes(battleState.getPlayerDiceTypes());
                if (tutorialController != null) {
                    tutorialController.onPrismaticUsed();
                }
                closePrismaticPopup();
                updateButtons(battleState.getCurrentPhase());
            }

            @Override
            public void onPopupClosed() {
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
        weatherLabel = null;
        weatherTitleLabel = null;
        buttonsCreated = false;
    }

    public void setBattleState(BattleState state) {
        this.battleState = state;
    }

    public void setTutorialController(TutorialController controller) {
        this.tutorialController = controller;
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
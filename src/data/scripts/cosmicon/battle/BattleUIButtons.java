package data.scripts.cosmicon.battle;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import com.fs.starfarer.api.util.Misc;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.TurnState.Phase;
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
    private LabelAPI weatherTitleLabel;
    private LabelAPI weatherDescLabel;
    private float weatherDescBoxX;
    private float weatherDescBoxY;
    private float weatherDescBoxW;
    private float weatherDescBoxH;
    private boolean buttonsCreated = false;

    private PrismaticDiceSelectionPopup prismaticPopup;
    private CustomPanelAPI prismaticPopupPanel;
    private boolean prismaticPopupActive;

    private float diceZoneCenterX;
    private float diceZoneCenterY;
    private float playerPrismaticBtnX;
    private float playerPrismaticBtnY;
    private TutorialController tutorialController;

    private List<ButtonAPI> opponentStatusButtons;
    private List<ButtonAPI> playerStatusButtons;

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

        TooltipMakerAPI abilityTp = UIComponentFactory.createTooltipForButtons(panel, this, 
            PASSIVE_BTN_WIDTH, PASSIVE_BTN_HEIGHT, 0f, 0f);

        float opponentCardUiX = BattleRenderingUtils.getOpponentCardX();
        float opponentCardUiY = BattleRenderingUtils.getOpponentCardY();
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

        float playerCardUiX = BattleRenderingUtils.getPlayerCardX();
        float playerCardUiY = BattleRenderingUtils.getPlayerCardY();
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
        opponentStatusButtons = new ArrayList<>();
        playerStatusButtons = new ArrayList<>();

        float btnWidth = BattleRenderingUtils.STATUS_BOX_WIDTH - 20f;
        float btnHeight = 18f;
        float spacing = 20f;

        float opponentCardX = BattleRenderingUtils.getOpponentCardX();
        float opponentCardY = BattleRenderingUtils.getOpponentCardY();
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
            opponentStatusButtons.add(btn);

            final int index = i;
            opponentTp.addTooltipToPrevious(new TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) { return false; }
                @Override
                public float getTooltipWidth(Object tooltipParam) { return 350f; }
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    StatusEffect effect = getEffectAtIndex(index, false);
                    if (effect == null) return;
                    String name = Strings.get("status." + effect.name().toLowerCase());
                    if (effect == StatusEffect.CYRENE_TALLY && battleState != null) {
                        int cumulative = battleState.getCumulativeAtkDef(false);
                        String desc = Strings.format("status_desc.cyrene_tally_desc", cumulative);
                        tooltip.addPara(name, Misc.getHighlightColor(), 5f);
                        tooltip.addPara(desc, Misc.getGrayColor(), 3f);
                    } else {
                        String desc = Strings.get("status_desc." + effect.name().toLowerCase() + "_desc");
                        tooltip.addPara(name, Misc.getHighlightColor(), 5f);
                        tooltip.addPara(desc, Misc.getGrayColor(), 3f);
                    }
                }
            }, TooltipLocation.LEFT, false);
        }

        float playerCardX = BattleRenderingUtils.getPlayerCardX();
        float playerCardY = BattleRenderingUtils.getPlayerCardY();
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
            playerStatusButtons.add(btn);

            final int index = i;
            playerTp.addTooltipToPrevious(new TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object tooltipParam) { return false; }
                @Override
                public float getTooltipWidth(Object tooltipParam) { return 350f; }
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    StatusEffect effect = getEffectAtIndex(index, true);
                    if (effect == null) return;
                    String name = Strings.get("status." + effect.name().toLowerCase());
                    if (effect == StatusEffect.CYRENE_TALLY && battleState != null) {
                        int cumulative = battleState.getCumulativeAtkDef(true);
                        String desc = Strings.format("status_desc.cyrene_tally_desc", cumulative);
                        tooltip.addPara(name, Misc.getHighlightColor(), 5f);
                        tooltip.addPara(desc, Misc.getGrayColor(), 3f);
                    } else {
                        String desc = Strings.get("status_desc." + effect.name().toLowerCase() + "_desc");
                        tooltip.addPara(name, Misc.getHighlightColor(), 5f);
                        tooltip.addPara(desc, Misc.getGrayColor(), 3f);
                    }
                }
            }, TooltipLocation.RIGHT, false);
        }
    }

    private void createWeatherLabel() {
        float boxWidth = 255f;
        float descBoxHeight = 55f;
        float descBoxPadding = 10f;
        float titleHeight = 18f;
        float playerCardY = BattleRenderingUtils.getPlayerCardY();

        float weatherX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.MARGIN - boxWidth;
        float titleY = playerCardY - PASSIVE_BTN_HEIGHT - titleHeight - descBoxHeight - 15f;
        float descBoxY = titleY + titleHeight + 4f;

        weatherTitleLabel = UIComponentFactory.createLabelSmall(panel, Strings.get("battle.weather_effects"),
            java.awt.Color.LIGHT_GRAY, com.fs.starfarer.api.ui.Alignment.LMID, boxWidth, titleHeight, weatherX, titleY);

        weatherDescBoxX = weatherX;
        weatherDescBoxY = descBoxY;
        weatherDescBoxW = boxWidth;
        weatherDescBoxH = descBoxHeight;

        TooltipMakerAPI weatherTp = panel.createUIElement(boxWidth, descBoxHeight, false);
        panel.addUIElement(weatherTp).inTL(weatherX, descBoxY + descBoxPadding + 2f);

        weatherDescLabel = weatherTp.addPara("", java.awt.Color.GRAY, 0f);
        weatherDescLabel.setAlignment(com.fs.starfarer.api.ui.Alignment.MID);

        updateWeatherLabel();
    }

    public void updateWeatherLabel() {
        if (weatherTitleLabel == null || battleState == null) return;
        WeatherController wc = battleState.getWeatherController();
        if (wc == null) {
            weatherTitleLabel.setText(Strings.get("battle.weather_effects") + ": " + Strings.get("battle.weather_none"));
            weatherTitleLabel.setColor(java.awt.Color.LIGHT_GRAY);
            if (weatherDescLabel != null) weatherDescLabel.setText("");
            return;
        }
        WeatherType weather = wc.getCurrentWeather();
        if (weather == null) {
            weatherTitleLabel.setText(Strings.get("battle.weather_effects") + ": " + Strings.get("battle.weather_none"));
            weatherTitleLabel.setColor(java.awt.Color.LIGHT_GRAY);
            if (weatherDescLabel != null) weatherDescLabel.setText("");
        } else {
            weatherTitleLabel.setText(Strings.get("battle.weather_effects") + ": " + Strings.get("weather." + weather.name().toLowerCase()));
            weatherTitleLabel.setColor(weather.getColor());
            if (weatherDescLabel != null) {
                weatherDescLabel.setText(weather.getDescription());
            }
        }
    }

    private StatusEffect getEffectAtIndex(int targetIndex, boolean forPlayer) {
        if (battleState == null) return null;
        StatusEffectProcessor effects = battleState.getEffects(forPlayer);
        int idx = 0;
        for (StatusEffect effect : StatusEffect.values()) {
            if (effects.getLayers(effect) > 0) {
                if (idx == targetIndex) {
                    return effect;
                }
                idx++;
            }
        }
        return null;
    }

    public void updateStatusTooltipButtons() {
        if (opponentStatusButtons != null) {
            for (int i = 0; i < opponentStatusButtons.size(); i++) {
                opponentStatusButtons.get(i).setOpacity(getEffectAtIndex(i, false) != null ? 0.01f : 0f);
            }
        }
        if (playerStatusButtons != null) {
            for (int i = 0; i < playerStatusButtons.size(); i++) {
                playerStatusButtons.get(i).setOpacity(getEffectAtIndex(i, true) != null ? 0.01f : 0f);
            }
        }
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
                            tutorialController.setRerollPending();
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
                    if (battleController != null && battleController.isGatekeeperEarlyExit()) {
                        battleController.getState().setWinner("player");
                    }
                    if (callbacks != null) {
                        callbacks.dismissDialog();
                    }
                }
            }
        }
    }

    public void updateButtons(Phase phase) {
        boolean playerIsAttacker = battleState.isPlayerAttacker();
        boolean playerShouldSelect = (playerIsAttacker && phase == Phase.SELECTING_ATTACK) ||
                                      (!playerIsAttacker && phase == Phase.SELECTING_DEFENSE);

        String confirmText = switch (phase) {
            case SELECTING_ATTACK -> playerIsAttacker ? Strings.get("battle.confirm_attack") : Strings.get("phase.waiting");
            case SELECTING_DEFENSE -> playerIsAttacker ? Strings.get("phase.waiting") : Strings.get("battle.confirm_defense");
            case RESOLVING_PRE_CLASH, RESOLVING_MODIFICATION, RESOLVING -> Strings.get("phase.resolving");
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
            canReroll = canReroll && tutorialController.canRerollWithCurrentSelection();
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
                inputHandler.consumeClick();
                updateButtons(battleState.getCurrentPhase());
            }

            @Override
            public void onPopupClosed() {
                if (tutorialController != null) {
                    tutorialController.onPrismaticPopupClosed();
                }
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
        weatherTitleLabel = null;
        weatherDescLabel = null;
        weatherDescBoxX = 0f;
        weatherDescBoxY = 0f;
        weatherDescBoxW = 0f;
        weatherDescBoxH = 0f;
        if (opponentStatusButtons != null) opponentStatusButtons.clear();
        opponentStatusButtons = null;
        if (playerStatusButtons != null) playerStatusButtons.clear();
        playerStatusButtons = null;
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

    public float getWeatherDescBoxX() { return weatherDescBoxX; }
    public float getWeatherDescBoxY() { return weatherDescBoxY; }
    public float getWeatherDescBoxW() { return weatherDescBoxW; }
    public float getWeatherDescBoxH() { return weatherDescBoxH; }

    }
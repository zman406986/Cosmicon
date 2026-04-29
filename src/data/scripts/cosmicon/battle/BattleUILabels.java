package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState.Phase;
import data.scripts.cosmicon.prismatic.PrismaticDiceInstance;
import data.scripts.cosmicon.prismatic.PrismaticDiceRegistry;
import data.scripts.cosmicon.prismatic.PrismaticDiceType;
import data.scripts.cosmicon.prismatic.PrismaticFaceDisplay;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.UIComponentFactory;

public class BattleUILabels {
    private static final float PRISMATIC_ROLLED_LABEL_OFFSET_Y = 20f;

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
    private LabelAPI playerPrismaticUsesLabel;
    private LabelAPI playerPrismaticFaceMappingLabel;
    private LabelAPI playerPrismaticEffectLabel;
    private LabelAPI playerPrismaticRolledLabel;
    private LabelAPI opponentPrismaticUsesLabel;
    private LabelAPI opponentPrismaticFaceMappingLabel;
    private LabelAPI opponentPrismaticEffectLabel;
    private LabelAPI playerPrismaticClickHintLabel;

    private List<LabelAPI> playerStatusLabels;
    private List<LabelAPI> opponentStatusLabels;
    private static final int MAX_STATUS_EFFECTS = 12;

    private ValueChangeAnimator attackerValueAnimator;
    private ValueChangeAnimator defenderValueAnimator;

    private float opponentPrismaticBtnX;
    private float opponentPrismaticBtnY;
    private float playerPrismaticBtnX;
    private float playerPrismaticBtnY;

    private CustomPanelAPI panel;
    private BattleState battleState;

    private PrismaticDiceInstance pendingPrismaticInstance;
    private int pendingPrismaticAnimatorIndex = -1;
    private DiceRollManager diceRollManager;

    public void init(CustomPanelAPI panel, BattleState battleState, DiceRollManager diceRollManager,
            float opponentPrismaticBtnX, float opponentPrismaticBtnY,
            float playerPrismaticBtnX, float playerPrismaticBtnY) {
        this.panel = panel;
        this.battleState = battleState;
        this.diceRollManager = diceRollManager;
        this.opponentPrismaticBtnX = opponentPrismaticBtnX;
        this.opponentPrismaticBtnY = opponentPrismaticBtnY;
        this.playerPrismaticBtnX = playerPrismaticBtnX;
        this.playerPrismaticBtnY = playerPrismaticBtnY;
        
        createLabels();
        createValueAnimators();
    }

    public void cleanup() {
        if (attackerValueAnimator != null) {
            attackerValueAnimator.cleanup();
            attackerValueAnimator = null;
        }
        if (defenderValueAnimator != null) {
            defenderValueAnimator.cleanup();
            defenderValueAnimator = null;
        }

        removeAndNullLabel(phaseLabel);
        phaseLabel = null;
        removeAndNullLabel(instructionLabel);
        instructionLabel = null;
        removeAndNullLabel(playerHpLabel);
        playerHpLabel = null;
        removeAndNullLabel(opponentHpLabel);
        opponentHpLabel = null;
        removeAndNullLabel(playerNameLabel);
        playerNameLabel = null;
        removeAndNullLabel(opponentNameLabel);
        opponentNameLabel = null;
        removeAndNullLabel(resultLabel);
        resultLabel = null;
        removeAndNullLabel(playerAtkLabel);
        playerAtkLabel = null;
        removeAndNullLabel(playerDefLabel);
        playerDefLabel = null;
        removeAndNullLabel(opponentAtkLabel);
        opponentAtkLabel = null;
        removeAndNullLabel(opponentDefLabel);
        opponentDefLabel = null;
        removeAndNullLabel(playerPrismaticLabel);
        playerPrismaticLabel = null;
        removeAndNullLabel(playerOrangeLabel);
        playerOrangeLabel = null;
        removeAndNullLabel(playerPurpleLabel);
        playerPurpleLabel = null;
        removeAndNullLabel(playerBlueLabel);
        playerBlueLabel = null;
        removeAndNullLabel(opponentPrismaticLabel);
        opponentPrismaticLabel = null;
        removeAndNullLabel(opponentOrangeLabel);
        opponentOrangeLabel = null;
        removeAndNullLabel(opponentPurpleLabel);
        opponentPurpleLabel = null;
        removeAndNullLabel(opponentBlueLabel);
        opponentBlueLabel = null;
        removeAndNullLabel(clickHintLabel);
        clickHintLabel = null;
        removeAndNullLabel(attackerSelectionLabel);
        attackerSelectionLabel = null;
        removeAndNullLabel(attackerEffectLabel);
        attackerEffectLabel = null;
        removeAndNullLabel(defenderSelectionLabel);
        defenderSelectionLabel = null;
        removeAndNullLabel(defenderEffectLabel);
        defenderEffectLabel = null;
        removeAndNullLabel(attackerConfirmedSelectionLabel);
        attackerConfirmedSelectionLabel = null;
        removeAndNullLabel(attackerConfirmedEffectLabel);
        attackerConfirmedEffectLabel = null;
        removeAndNullLabel(attackerIconValueLabel);
        attackerIconValueLabel = null;
        removeAndNullLabel(defenderIconValueLabel);
        defenderIconValueLabel = null;
        removeAndNullLabel(playerPrismaticUsesLabel);
        playerPrismaticUsesLabel = null;
        removeAndNullLabel(playerPrismaticFaceMappingLabel);
        playerPrismaticFaceMappingLabel = null;
        removeAndNullLabel(playerPrismaticEffectLabel);
        playerPrismaticEffectLabel = null;
        removeAndNullLabel(playerPrismaticRolledLabel);
        playerPrismaticRolledLabel = null;
        removeAndNullLabel(opponentPrismaticUsesLabel);
        opponentPrismaticUsesLabel = null;
        removeAndNullLabel(opponentPrismaticFaceMappingLabel);
        opponentPrismaticFaceMappingLabel = null;
        removeAndNullLabel(opponentPrismaticEffectLabel);
        opponentPrismaticEffectLabel = null;
        removeAndNullLabel(playerPrismaticClickHintLabel);
        playerPrismaticClickHintLabel = null;

        if (playerStatusLabels != null) {
            for (LabelAPI label : playerStatusLabels) {
                removeAndNullLabel(label);
            }
            playerStatusLabels.clear();
            playerStatusLabels = null;
        }
        if (opponentStatusLabels != null) {
            for (LabelAPI label : opponentStatusLabels) {
                removeAndNullLabel(label);
            }
            opponentStatusLabels.clear();
            opponentStatusLabels = null;
        }

        panel = null;
        battleState = null;
        diceRollManager = null;
        pendingPrismaticInstance = null;
        pendingPrismaticAnimatorIndex = -1;
    }

    private void removeAndNullLabel(LabelAPI label) {
        if (label != null && panel != null) {
            panel.removeComponent((UIComponentAPI) label);
        }
    }

    public void setBattleState(BattleState state) {
        this.battleState = state;
    }

    private void createLabels() {
        phaseLabel = UIComponentFactory.createLabelLarge(panel, "", 
            ColorHelper.PHASE_LABEL, Alignment.MID, 400f, 30f, 
            BattleRenderingUtils.PANEL_WIDTH / 2f - 200f, 30f);

        instructionLabel = UIComponentFactory.createLabelSmall(panel, "", 
            BattleRenderingUtils.COLOR_TEXT, Alignment.MID, 400f, 25f, 
            BattleRenderingUtils.PANEL_WIDTH / 2f - 200f, 60f);

        playerNameLabel = UIComponentFactory.createLabelSmall(panel, Strings.get("battle.player"), 
            ColorHelper.PLAYER_NAME, Alignment.MID, BattleRenderingUtils.CARD_WIDTH, 20f,
            BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN,
            BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN + 5f);

        playerHpLabel = UIComponentFactory.createLabelSmall(panel, "25/25", 
            BattleRenderingUtils.COLOR_HP_TEXT, Alignment.LMID, 50f, 20f,
            BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN + 5f,
            BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN + 5f);

        opponentNameLabel = UIComponentFactory.createLabelSmall(panel, Strings.get("battle.opponent"), 
            ColorHelper.OPPONENT_NAME, Alignment.MID, BattleRenderingUtils.CARD_WIDTH, 20f,
            BattleRenderingUtils.MARGIN, BattleRenderingUtils.MARGIN + 5f);

        opponentHpLabel = UIComponentFactory.createLabelSmall(panel, "30/30", 
            BattleRenderingUtils.COLOR_HP_TEXT, Alignment.LMID, 50f, 20f,
            BattleRenderingUtils.MARGIN + 5f, BattleRenderingUtils.MARGIN + 5f);

        resultLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.INSIGNIA_LARGE, ColorHelper.PRISMATIC_GOLD, Alignment.MID, 400f, 40f,
            BattleRenderingUtils.PANEL_WIDTH / 2f - 200f, BattleRenderingUtils.PANEL_HEIGHT / 2f - 20f, 0f);

        createAtkDefLabels();
        createDiceCountLabels();
        createSelectionLabels();
        createPrismaticLabels();
        createClickHintLabel();
        createStatusEffectLabels();
    }

    private void createStatusEffectLabels() {
        playerStatusLabels = new ArrayList<>();
        opponentStatusLabels = new ArrayList<>();

        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;
        float playerBoxX = playerCardX - BattleRenderingUtils.STATUS_BOX_WIDTH - 20f;

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;
        float opponentBoxX = opponentCardX + BattleRenderingUtils.CARD_WIDTH + 20f;

        for (int i = 0; i < MAX_STATUS_EFFECTS; i++) {
            float yOffset = i * 20f;
            LabelAPI playerLabel = UIComponentFactory.createLabelSmall(panel, "",
                Color.WHITE, Alignment.LMID, BattleRenderingUtils.STATUS_BOX_WIDTH - 20f, 18f,
                playerBoxX + BattleRenderingUtils.STATUS_BOX_PADDING, playerCardY + BattleRenderingUtils.STATUS_BOX_PADDING + yOffset);
            playerLabel.setOpacity(0f);
            playerStatusLabels.add(playerLabel);

            LabelAPI opponentLabel = UIComponentFactory.createLabelSmall(panel, "",
                Color.WHITE, Alignment.LMID, BattleRenderingUtils.STATUS_BOX_WIDTH - 20f, 18f,
                opponentBoxX + BattleRenderingUtils.STATUS_BOX_PADDING, opponentCardY + BattleRenderingUtils.STATUS_BOX_PADDING + yOffset);
            opponentLabel.setOpacity(0f);
            opponentStatusLabels.add(opponentLabel);
        }
    }

    private void createAtkDefLabels() {
        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;

        playerAtkLabel = UIComponentFactory.createLabelSmall(panel, "3", 
            ColorHelper.ATTACK_VALUE, Alignment.MID, 30f, 20f,
            playerCardX + BattleRenderingUtils.ATK_LEFT_MARGIN + 2f,
            playerCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        playerDefLabel = UIComponentFactory.createLabelSmall(panel, "2", 
            ColorHelper.DEFENSE_VALUE, Alignment.MID, 30f, 20f,
            playerCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN - 30f,
            playerCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;

        opponentAtkLabel = UIComponentFactory.createLabelSmall(panel, "3", 
            ColorHelper.ATTACK_VALUE, Alignment.MID, 30f, 20f,
            opponentCardX + BattleRenderingUtils.ATK_LEFT_MARGIN + 2f,
            opponentCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);

        opponentDefLabel = UIComponentFactory.createLabelSmall(panel, "2", 
            ColorHelper.DEFENSE_VALUE, Alignment.MID, 30f, 20f,
            opponentCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DEF_RIGHT_MARGIN - 30f,
            opponentCardY + BattleRenderingUtils.CARD_HEIGHT - 22f);
    }

    private void createDiceCountLabels() {
        float playerCardX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.MARGIN;
        float playerCardY = BattleRenderingUtils.PANEL_HEIGHT - BattleRenderingUtils.CARD_HEIGHT - BattleRenderingUtils.MARGIN;

        float diceX = playerCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
        float diceStartY = playerCardY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

        playerOrangeLabel = createCountLabel(diceX, diceStartY);
        playerPurpleLabel = createCountLabel(diceX, diceStartY + 26);
        playerBlueLabel = createCountLabel(diceX, diceStartY + 52);
        playerPrismaticLabel = createCountLabel(diceX, diceStartY + 78);

        float opponentCardX = BattleRenderingUtils.MARGIN;
        float opponentCardY = BattleRenderingUtils.MARGIN;

        diceX = opponentCardX + BattleRenderingUtils.CARD_WIDTH - BattleRenderingUtils.DICE_POOL_RIGHT_MARGIN - BattleRenderingUtils.DICE_ICON_SIZE / 2f - 11f;
        diceStartY = opponentCardY + BattleRenderingUtils.DICE_POOL_TOP_MARGIN + 3f;

        opponentOrangeLabel = createCountLabel(diceX, diceStartY);
        opponentPurpleLabel = createCountLabel(diceX, diceStartY + 26);
        opponentBlueLabel = createCountLabel(diceX, diceStartY + 52);
        opponentPrismaticLabel = createCountLabel(diceX, diceStartY + 78);
    }

    private LabelAPI createCountLabel(float x, float y) {
        return UIComponentFactory.createLabelSmall(panel, "0", Color.WHITE, Alignment.MID, 22f, 16f, x, y);
    }

    private void createSelectionLabels() {
        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float iconSize = halfH * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        float topIconCenterY = (halfH - iconSize) / 2f + iconSize / 2f;
        float bottomIconCenterY = halfH + (halfH - iconSize) / 2f + iconSize / 2f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float labelWidth = 200f;

        attackerSelectionLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.ATTACK_VALUE, Alignment.MID, labelWidth, 20f,
            centerX - labelWidth / 2f, bottomIconCenterY + iconSize / 2f + 25f, 0f);

        attackerEffectLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.PRISMATIC_GOLD, Alignment.MID, labelWidth, 20f,
            centerX - labelWidth / 2f, bottomIconCenterY + iconSize / 2f + 5f, 0f);

        defenderSelectionLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.DEFENSE_VALUE, Alignment.MID, labelWidth, 20f,
            centerX - labelWidth / 2f, topIconCenterY - iconSize / 2f - 25f, 0f);

        defenderEffectLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.PRISMATIC_GOLD, Alignment.MID, labelWidth, 20f,
            centerX - labelWidth / 2f, topIconCenterY - iconSize / 2f - 5f, 0f);

        float rightCenterX = BattleRenderingUtils.PANEL_WIDTH - BattleRenderingUtils.CARD_WIDTH / 2f - BattleRenderingUtils.MARGIN;
        float confirmedLabelWidth = 150f;

        attackerConfirmedSelectionLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.ATTACK_VALUE, Alignment.MID, confirmedLabelWidth, 20f,
            rightCenterX - confirmedLabelWidth / 2f, bottomIconCenterY + iconSize / 2f + 25f, 0f);

        attackerConfirmedEffectLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.PRISMATIC_GOLD, Alignment.MID, confirmedLabelWidth, 20f,
            rightCenterX - confirmedLabelWidth / 2f, bottomIconCenterY + iconSize / 2f + 5f, 0f);

        attackerIconValueLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.INSIGNIA_VERY_LARGE, ColorHelper.ATTACK_VALUE, Alignment.MID, 80f, 35f, 0f, 0f, 0f);

        defenderIconValueLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.INSIGNIA_VERY_LARGE, ColorHelper.DEFENSE_VALUE, Alignment.MID, 80f, 35f, 0f, 0f, 0f);
    }

    private void createPrismaticLabels() {
        opponentPrismaticUsesLabel = UIComponentFactory.createLabelSmall(panel, "2", 
            ColorHelper.PRISMATIC_GOLD, Alignment.MID, 40f, 20f, opponentPrismaticBtnX + 50f, opponentPrismaticBtnY + 25f);

        float opponentPrismaticDescX = opponentPrismaticBtnX + 50f;
        opponentPrismaticFaceMappingLabel = UIComponentFactory.createLabel(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.PRISMATIC_GOLD, Alignment.LMID, 280f, 20f,
            opponentPrismaticDescX, opponentPrismaticBtnY);
        opponentPrismaticFaceMappingLabel.setOpacity(0f);

        opponentPrismaticEffectLabel = UIComponentFactory.createLabelSmall(panel, "", 
            Color.LIGHT_GRAY, Alignment.LMID, 280f, 20f,
            opponentPrismaticDescX, opponentPrismaticBtnY + 20f);
        opponentPrismaticEffectLabel.setOpacity(0f);

        float playerPrismaticUsesX = BattleRenderingUtils.MARGIN + 110f;
        float playerPrismaticUsesY = BattleRenderingUtils.PANEL_HEIGHT - 115f;

        playerPrismaticUsesLabel = UIComponentFactory.createLabelSmall(panel, "2", 
            ColorHelper.PRISMATIC_GOLD, Alignment.MID, 40f, 20f, playerPrismaticUsesX, playerPrismaticUsesY);

        float playerPrismaticDescX = BattleRenderingUtils.MARGIN + 130f;
        float playerFaceMappingY = BattleRenderingUtils.PANEL_HEIGHT - 100f;

        playerPrismaticFaceMappingLabel = UIComponentFactory.createLabelSmall(panel, "", 
            ColorHelper.PRISMATIC_GOLD, Alignment.LMID, 280f, 20f, playerPrismaticDescX, playerFaceMappingY);
        playerPrismaticFaceMappingLabel.setOpacity(0f);

        float playerEffectY = playerFaceMappingY + 20f;

        playerPrismaticEffectLabel = UIComponentFactory.createLabelSmall(panel, "", 
            Color.LIGHT_GRAY, Alignment.LMID, 280f, 20f, playerPrismaticDescX, playerEffectY);
        playerPrismaticEffectLabel.setOpacity(0f);

        playerPrismaticRolledLabel = UIComponentFactory.createLabel(panel, "", 
            Fonts.DEFAULT_SMALL, ColorHelper.PRISMATIC_BRIGHT, Alignment.MID, 60f, 20f, 0f, 0f);
        playerPrismaticRolledLabel.setOpacity(0f);

        float hintLabelWidth = 110f;
        float hintLabelX = playerPrismaticBtnX - hintLabelWidth - 5f;
        float hintLabelY = playerPrismaticBtnY + 10f;
        playerPrismaticClickHintLabel = UIComponentFactory.createLabelSmall(panel,
            Strings.get("battle.prismatic_click_hint"),
            ColorHelper.PRISMATIC_HINT_ENABLED, Alignment.RMID, hintLabelWidth, 20f,
            hintLabelX, hintLabelY);
        playerPrismaticClickHintLabel.setOpacity(0f);
    }

    private void createClickHintLabel() {
        clickHintLabel = UIComponentFactory.createLabelWithOpacity(panel, "", 
            Fonts.DEFAULT_SMALL, new Color(200, 200, 200, 150), Alignment.MID, 200f, 20f,
            BattleRenderingUtils.PANEL_WIDTH / 2f - 100f, BattleRenderingUtils.PANEL_HEIGHT - 30f, 0f);
    }

    private void createValueAnimators() {
        attackerValueAnimator = new ValueChangeAnimator();
        defenderValueAnimator = new ValueChangeAnimator();
    }

    public void updateStatusEffectLabels() {
        if (battleState == null || playerStatusLabels == null || opponentStatusLabels == null) return;

        StatusEffectProcessor playerEffects = battleState.getPlayerEffects();
        StatusEffectProcessor opponentEffects = battleState.getOpponentEffects();

        int playerIdx = 0;
        for (StatusEffectProcessor.StatusEffect effect : StatusEffectProcessor.StatusEffect.values()) {
            if (playerIdx >= playerStatusLabels.size()) break;
            int layers = playerEffects.getLayers(effect);
            if (layers > 0) {
                LabelAPI label = playerStatusLabels.get(playerIdx);
                int duration = playerEffects.getDuration(effect);
                String effectName = Strings.get("status." + effect.name().toLowerCase());
                String text = duration < StatusEffectProcessor.PERMANENT_DURATION
                    ? String.format("%s (%d, %d turns)", effectName, layers, duration)
                    : String.format("%s (%d)", effectName, layers);
                label.setText(text);
                label.setOpacity(1f);
                playerIdx++;
            }
        }
        for (int i = playerIdx; i < playerStatusLabels.size(); i++) {
            playerStatusLabels.get(i).setOpacity(0f);
        }

        int opponentIdx = 0;
        for (StatusEffectProcessor.StatusEffect effect : StatusEffectProcessor.StatusEffect.values()) {
            if (opponentIdx >= opponentStatusLabels.size()) break;
            int layers = opponentEffects.getLayers(effect);
            if (layers > 0) {
                LabelAPI label = opponentStatusLabels.get(opponentIdx);
                int duration = opponentEffects.getDuration(effect);
                String effectName = Strings.get("status." + effect.name().toLowerCase());
                String text = duration < StatusEffectProcessor.PERMANENT_DURATION
                    ? String.format("%s (%d, %d turns)", effectName, layers, duration)
                    : String.format("%s (%d)", effectName, layers);
                label.setText(text);
                label.setOpacity(1f);
                opponentIdx++;
            }
        }
        for (int i = opponentIdx; i < opponentStatusLabels.size(); i++) {
            opponentStatusLabels.get(i).setOpacity(0f);
        }
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

    public void updatePhaseLabel() {
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
                String valuesStr = battleState.getSelectedDiceValuesFormatted(true);
                if (valuesStr != null && !valuesStr.isEmpty()) {
                    instructionText = instructionText + "  " + Strings.format("battle.selected_values", valuesStr);
                }
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
    }

    public void updateSelectionDisplayLabels() {
        if (battleState == null || attackerSelectionLabel == null) return;

        attackerSelectionLabel.setOpacity(0f);
        attackerEffectLabel.setOpacity(0f);
        defenderSelectionLabel.setOpacity(0f);
        defenderEffectLabel.setOpacity(0f);
    }

    

    public void updateConfirmedSelectionLabels() {
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

            if (confirmedText != null && !confirmedText.isEmpty()) {
                attackerConfirmedSelectionLabel.setText(Strings.format("battle.selected_values", confirmedText));
                attackerConfirmedSelectionLabel.setOpacity(1f);
                attackerConfirmedEffectLabel.setOpacity(0f);
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

            if (confirmedText != null && !confirmedText.isEmpty()) {
                defenderSelectionLabel.setText(Strings.format("battle.selected_values", confirmedText));
                defenderSelectionLabel.setOpacity(1f);
                defenderEffectLabel.setOpacity(0f);
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

    public void updateIconValueLabels() {
        if (battleState == null || attackerIconValueLabel == null || defenderIconValueLabel == null) return;

        Phase phase = battleState.getCurrentPhase();

        attackerIconValueLabel.setOpacity(0f);
        defenderIconValueLabel.setOpacity(0f);

        if (phase == Phase.RESOLVING) return;

        float halfH = BattleRenderingUtils.PANEL_HEIGHT / 2f;
        float centerX = BattleRenderingUtils.PANEL_WIDTH / 2f;
        float labelW = 80f;
        float labelH = 35f;

        boolean playerIsAttacker = battleState.isPlayerAttacker();

        float topIconCenterY = halfH / 2f;
        float bottomIconCenterY = halfH + halfH / 2f;

        float attackerLabelY = playerIsAttacker ? bottomIconCenterY : topIconCenterY;
        float defenderLabelY = playerIsAttacker ? topIconCenterY : bottomIconCenterY;

        attackerIconValueLabel.getPosition().inTL(centerX - labelW / 2f, attackerLabelY - labelH / 2f);
        defenderIconValueLabel.getPosition().inTL(centerX - labelW / 2f, defenderLabelY - labelH / 2f);

        if (phase == Phase.SELECTING_ATTACK) {
            int runningTotal = calculateVisibleSum(playerIsAttacker);
            if (runningTotal > 0) {
                attackerIconValueLabel.setText(String.valueOf(runningTotal));
                attackerIconValueLabel.setOpacity(1f);
            }
            defenderIconValueLabel.setOpacity(0f);
            return;
        }

        if (phase == Phase.SELECTING_DEFENSE || (phase == Phase.ROLLING && battleState.isDefenderRolling())) {
            int attackerValue = getAttackerTotalValue();
            attackerIconValueLabel.setText(String.valueOf(attackerValue));
            attackerIconValueLabel.setOpacity(attackerValue > 0 ? 1f : 0f);

            if (phase == Phase.SELECTING_DEFENSE) {
                int runningTotal = calculateVisibleSum(!playerIsAttacker);
                if (runningTotal > 0) {
                    defenderIconValueLabel.setText(String.valueOf(runningTotal));
                    defenderIconValueLabel.setOpacity(1f);
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

    private int calculateVisibleSum(boolean forPlayer) {
        AISelectionVisualizer viz = battleState.getAiSelectionVisualizer();
        Phase phase = battleState.getCurrentPhase();
        if (viz != null && viz.hasStarted() && !viz.isRerollPhase() && !forPlayer) {
            boolean vizMatchesPhase = (phase == Phase.SELECTING_ATTACK && !battleState.isPlayerAttacker()) ||
                                      (phase == Phase.SELECTING_DEFENSE && battleState.isPlayerAttacker());
            if (vizMatchesPhase) {
                List<Integer> visibleIndices = viz.getVisibleIndices();
                List<Integer> values = battleState.getOpponentDiceValues();
                if (values != null && !visibleIndices.isEmpty()) {
                    int sum = 0;
                    for (int idx : visibleIndices) {
                        if (idx >= 0 && idx < values.size()) {
                            sum += values.get(idx);
                        }
                    }
                    return sum;
                }
            }
        }
        int base = battleState.calculateSelectedSum(forPlayer);
        boolean isAttacking = battleState.isAttacker(forPlayer);
        StatusEffectProcessor effects = battleState.getEffects(forPlayer);
        int bonus = isAttacking
            ? effects.calculateAttackBonus(BattleState.TurnType.ATTACK)
            : effects.calculateDefenseBonus(BattleState.TurnType.DEFENSE);
        int prismatic = battleState.getPrismaticDiceTotalValue(forPlayer);
        return base + bonus + prismatic;
    }

    private int getAttackerTotalValue() {
        int baseValue = battleState.getAttackValue();
        StatusEffectProcessor attackerEffects = battleState.isPlayerAttacker()
            ? battleState.getPlayerEffects() : battleState.getOpponentEffects();
        int bonus = attackerEffects.calculateAttackBonus(BattleState.TurnType.ATTACK);
        int prismaticValue = battleState.getPrismaticDiceTotalValue(battleState.isPlayerAttacker());
        return baseValue + bonus + prismaticValue;
    }

    private int getDefenderTotalValue() {
        int baseValue = battleState.getDefenseValue();
        StatusEffectProcessor defenderEffects = battleState.isPlayerAttacker()
            ? battleState.getOpponentEffects() : battleState.getPlayerEffects();
        int bonus = defenderEffects.calculateDefenseBonus(BattleState.TurnType.DEFENSE);
        int prismaticValue = battleState.getPrismaticDiceTotalValue(!battleState.isPlayerAttacker());
        return baseValue + bonus + prismaticValue;
    }

    public void updatePrismaticButton() {
        if (playerPrismaticUsesLabel == null || battleState == null) return;

        int uses = battleState.getPlayerPrismaticUses();
        playerPrismaticUsesLabel.setText(String.valueOf(uses));

        if (uses > 0) {
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

    public void updatePrismaticRolledLabel() {
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
        playerPrismaticRolledLabel.getPosition().inTL(diceX + AnimationConstants.DICE_SIZE / 2f - labelWidth / 2f,
                                                 diceY + AnimationConstants.DICE_SIZE + PRISMATIC_ROLLED_LABEL_OFFSET_Y);
    }

    public void clearPrismaticRolledLabel() {
        if (playerPrismaticRolledLabel != null) {
            playerPrismaticRolledLabel.setOpacity(0f);
        }
        pendingPrismaticInstance = null;
        pendingPrismaticAnimatorIndex = -1;
    }

    // pendingPrismaticAnimatorIndex becomes stale when dice animators are recreated or repositioned
    public void setPendingPrismatic(PrismaticDiceInstance instance, int animatorIndex) {
        this.pendingPrismaticInstance = instance;
        this.pendingPrismaticAnimatorIndex = animatorIndex;
    }

    public void updateDiceRolledCounts(boolean isPlayer, List<DiceType> types) {
        if (types == null) return;
        
        DicePoolCounts counts = DicePoolCounts.fromPool(types);
        if (isPlayer) {
            playerPrismaticLabel.setText(String.valueOf(counts.getCount(DiceType.PRISMATIC)));
            playerOrangeLabel.setText(String.valueOf(counts.getCount(DiceType.ORANGE_D8)));
            playerPurpleLabel.setText(String.valueOf(counts.getCount(DiceType.PURPLE_D6)));
            playerBlueLabel.setText(String.valueOf(counts.getCount(DiceType.BLUE_D4)));
        } else {
            opponentPrismaticLabel.setText(String.valueOf(counts.getCount(DiceType.PRISMATIC)));
            opponentOrangeLabel.setText(String.valueOf(counts.getCount(DiceType.ORANGE_D8)));
            opponentPurpleLabel.setText(String.valueOf(counts.getCount(DiceType.PURPLE_D6)));
            opponentBlueLabel.setText(String.valueOf(counts.getCount(DiceType.BLUE_D4)));
        }
    }

    public void showClickHint(String text, float opacity) {
        if (clickHintLabel != null) {
            clickHintLabel.setText(text);
            clickHintLabel.setOpacity(opacity);
        }
    }

    public void hideClickHint() {
        if (clickHintLabel != null) {
            clickHintLabel.setOpacity(0f);
        }
    }

    public void updatePrismaticClickHint(boolean enabled) {
        if (playerPrismaticClickHintLabel != null) {
            if (enabled) {
                playerPrismaticClickHintLabel.setColor(ColorHelper.PRISMATIC_HINT_ENABLED);
                playerPrismaticClickHintLabel.setOpacity(1f);
            } else {
                playerPrismaticClickHintLabel.setColor(ColorHelper.PRISMATIC_DISABLED);
                playerPrismaticClickHintLabel.setOpacity(0.4f);
            }
        }
    }

    public ValueChangeAnimator getAttackerValueAnimator() {
        return attackerValueAnimator;
    }

    public ValueChangeAnimator getDefenderValueAnimator() {
        return defenderValueAnimator;
    }

    public LabelAPI getPhaseLabel() {
        return phaseLabel;
    }
}


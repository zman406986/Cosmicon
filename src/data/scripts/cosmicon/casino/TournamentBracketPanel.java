package data.scripts.cosmicon.casino;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.Strings;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UIComponentFactory;
import data.scripts.cosmicon.util.UnifiedCoord;

public class TournamentBracketPanel extends BaseCustomUIPanelPlugin implements ActionListenerDelegate {

    private static final float MATCH_W = 150f;
    private static final float MATCH_H = 64f;
    private static final float WB_TOP_Y = 60f;
    private static final float STATUS_BAR_H = 30f;
    private static final int MAX_NAME_LEN = 12;

    private static final Color COLOR_BG = new Color(20, 22, 35, 220);
    private static final Color COLOR_MATCH_BG = new Color(40, 45, 65, 200);
    private static final Color COLOR_MATCH_BORDER = new Color(80, 90, 120, 200);
    private static final Color COLOR_PLAYER_MATCH = new Color(0, 100, 200, 80);
    private static final Color COLOR_PLAYER_BORDER = new Color(0, 150, 255, 200);
    private static final Color COLOR_CURRENT_BORDER = Color.YELLOW;
    private static final Color COLOR_WINNER_TEXT = new Color(100, 255, 100);
    private static final Color COLOR_SIMULATED = new Color(100, 100, 110);
    private static final Color COLOR_PENDING = new Color(160, 160, 170);
    private static final Color COLOR_LB_DIVIDER = new Color(60, 65, 90, 180);
    private static final Color COLOR_LOSER = new Color(255, 100, 100);

    private static final int WB_TOTAL_MATCHES = 7;
    private static final int LB_TOTAL_MATCHES = 6;
    private static final int GF_TOTAL_MATCHES = 1;
    private static final int TOTAL_MATCH_LABELS = WB_TOTAL_MATCHES + LB_TOTAL_MATCHES + GF_TOTAL_MATCHES;
    private static final int[] WB_COUNTS = {4, 2, 1};
    private static final int[] LB_COUNTS = {2, 2, 1, 1};

    private static float uiToGlX(float uiX) {
        UnifiedCoord.PanelContext ctx = UnifiedCoord.getCurrentOrNull();
        return ctx.panelX() + uiX;
    }

    private static float uiToGlY(float uiY) {
        UnifiedCoord.PanelContext ctx = UnifiedCoord.getCurrentOrNull();
        return ctx.panelY() + ctx.panelHeight() - uiY;
    }

    private static float uiToGlMatchY(float uiY) {
        UnifiedCoord.PanelContext ctx = UnifiedCoord.getCurrentOrNull();
        return ctx.panelY() + ctx.panelHeight() - uiY - MATCH_H;
    }

    private CustomPanelAPI panel;
    private TournamentManager.BracketData data;
    private String[] displayNames;
    private Runnable onDismiss;

    private LabelAPI titleLabel;
    private LabelAPI[] matchLabels;
    private LabelAPI statusLabel;
    private LabelAPI wbHeaderR1;
    private LabelAPI wbHeaderR2;
    private LabelAPI wbHeaderFinal;
    private LabelAPI lbHeaderR1;
    private LabelAPI lbHeaderR2;
    private LabelAPI lbHeaderR3;
    private LabelAPI lbHeaderFinal;
    private LabelAPI gfHeader;

    private float panelW;
    private float panelH;

    private float baseX;
    private float colSpacing;
    private float rowSpacing;
    private float lbBaseY;

    public TournamentBracketPanel(TournamentManager.BracketData bracketData, String[] playerDisplayNames) {
        this.data = bracketData;
        this.displayNames = playerDisplayNames;
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        PositionAPI pos = panel.getPosition();
        this.panelW = pos.getWidth();
        this.panelH = pos.getHeight();

        computeLayout();

        createTitleLabel();
        createColumnHeaders();
        createMatchLabels();
        createStatusLabel();
        createExitButton();
        updateAllLabels();
    }

    private void computeLayout() {
        int numCols = 4;
        float minVisualGap = 80f;
        float minColSpacing = MATCH_W + minVisualGap;
        float minContentSpan = (numCols - 1f) * minColSpacing + MATCH_W;

        if (minContentSpan <= panelW) {
            float extraSpace = panelW - minContentSpan;
            baseX = extraSpace / 2f;
            colSpacing = minColSpacing;
        } else {
            colSpacing = minColSpacing;
            baseX = (panelW - minContentSpan) / 2f;
        }

        float gap = 20f;
        float fixedOverhead = WB_TOP_Y + MATCH_H + gap + getLbRowSpacing() + MATCH_H + STATUS_BAR_H + 20f;
        float available = panelH - fixedOverhead;
        rowSpacing = Math.max(64f, available / 3f);
        lbBaseY = WB_TOP_Y + 3f * rowSpacing + MATCH_H + 20f;
    }

    private float getLbRowSpacing() {
        return MATCH_H + 20f;
    }

    private void createTitleLabel() {
        titleLabel = Global.getSettings().createLabel(
            Strings.format("casino.tournament_bracket_title", 8), Fonts.INSIGNIA_LARGE);
        titleLabel.setColor(Color.YELLOW);
        titleLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) titleLabel)
            .setSize(panelW, 30f)
            .inTL(0f, 8f);
    }

    private void createColumnHeaders() {
        wbHeaderR1 = createHeaderLabel(Strings.get("casino.tournament_header_wb_r1"), baseX, 38f);
        wbHeaderR2 = createHeaderLabel(Strings.get("casino.tournament_header_wb_r2"), baseX + colSpacing, 38f);
        wbHeaderFinal = createHeaderLabel(Strings.get("casino.tournament_header_wb_final"), baseX + 2f * colSpacing, 38f);
        gfHeader = createHeaderLabel(Strings.get("casino.tournament_header_grand_final"), baseX + 3f * colSpacing, 38f);

        float lbHeaderY = lbBaseY - 18f;
        lbHeaderR1 = createHeaderLabel(Strings.get("casino.tournament_header_lb_r1"), baseX, lbHeaderY);
        lbHeaderR2 = createHeaderLabel(Strings.get("casino.tournament_header_lb_r2"), baseX + colSpacing, lbHeaderY);
        lbHeaderR3 = createHeaderLabel(Strings.get("casino.tournament_header_lb_r3"), baseX + 2f * colSpacing, lbHeaderY);
        lbHeaderFinal = createHeaderLabel(Strings.get("casino.tournament_header_lb_final"), baseX + 3f * colSpacing, lbHeaderY);
    }

    private LabelAPI createHeaderLabel(String text, float x, float y) {
        LabelAPI label = Global.getSettings().createLabel(text, Fonts.DEFAULT_SMALL);
        label.setColor(new Color(180, 185, 200));
        label.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) label)
            .setSize(MATCH_W, 16f)
            .inTL(x, y);
        return label;
    }

    private void createMatchLabels() {
        matchLabels = new LabelAPI[TOTAL_MATCH_LABELS];
        for (int i = 0; i < TOTAL_MATCH_LABELS; i++) {
            matchLabels[i] = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
            matchLabels[i].setColor(COLOR_PENDING);
            matchLabels[i].setAlignment(Alignment.MID);
            panel.addComponent((UIComponentAPI) matchLabels[i])
                .setSize(MATCH_W - 8f, MATCH_H - 4f)
                .inTL(-1000f, -1000f);
            matchLabels[i].setOpacity(0f);
        }
    }

    private void createStatusLabel() {
        statusLabel = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        statusLabel.setColor(Color.WHITE);
        statusLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) statusLabel)
            .setSize(panelW - 20f, 20f)
            .inTL(10f, panelH - STATUS_BAR_H - 5f);
    }

    private void createExitButton() {
        float btnW = 135f;
        float btnH = 28f;
        float btnX = panelW - btnW - 25f;
        float btnY = panelH - STATUS_BAR_H - 5f;

        TooltipMakerAPI exitTp = UIComponentFactory.createTooltipForButtons(panel, this, btnW, btnH, btnX, btnY);
        ButtonAPI exitButton = exitTp.addButton(Strings.get("casino.tournament_back_lounge"), "exit", btnW, btnH, 0f);
        exitButton.setQuickMode(true);
    }

    @Override
    public void actionPerformed(Object input, Object source) {
        if (source instanceof ButtonAPI btn) {
            String action = (String) btn.getCustomData();
            if ("exit".equals(action) && onDismiss != null) {
                onDismiss.run();
            }
        }
    }

    private float getWbMatchUiY(int round, int matchIndex) {
        float offset = (float) ((1 << round) - 1) * rowSpacing / 2f;
        return WB_TOP_Y + offset + matchIndex * rowSpacing * (1 << round);
    }

    private float getLbMatchUiY(int round, int matchIndex) {
        float lbSp = getLbRowSpacing();
        if (round <= 1) {
            return lbBaseY + matchIndex * lbSp;
        }
        return lbBaseY + lbSp * 0.5f;
    }

    private float getGfUiY() {
        return getWbMatchUiY(2, 0);
    }

    private void updateAllLabels() {
        if (data == null) {
            titleLabel.setText(Strings.get("casino.tournament_no_data_title"));
            statusLabel.setText(Strings.get("casino.tournament_no_data_desc"));
            return;
        }

        int labelIdx = 0;

        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < WB_COUNTS[r]; m++) {
                if (labelIdx >= matchLabels.length) break;
                float lx = baseX + r * colSpacing;
                float ly = getWbMatchUiY(r, m);
                int slot0 = getSlot(data.wbMatchups, r, m, 0);
                int slot1 = getSlot(data.wbMatchups, r, m, 1);
                int winner = getResult(data.wbResults, r, m);
                boolean involvesPlayer = (slot0 == 0 || slot1 == 0);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_WB, r, m);
                boolean completed = winner >= 0;

                configureMatchLabel(labelIdx, lx, ly, slot0, slot1, winner, involvesPlayer, isCurrent, completed);
                labelIdx++;
            }
        }

        for (int r = 0; r < 4; r++) {
            for (int m = 0; m < LB_COUNTS[r]; m++) {
                if (labelIdx >= matchLabels.length) break;
                float lx = baseX + r * colSpacing;
                float ly = getLbMatchUiY(r, m);
                int slot0 = getSlot(data.lbMatchups, r, m, 0);
                int slot1 = getSlot(data.lbMatchups, r, m, 1);
                int winner = getResult(data.lbResults, r, m);
                boolean involvesPlayer = (slot0 == 0 || slot1 == 0);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_LB, r, m);
                boolean completed = winner >= 0;

                configureMatchLabel(labelIdx, lx, ly, slot0, slot1, winner, involvesPlayer, isCurrent, completed);
                labelIdx++;
            }
        }

        float gfX = baseX + 3f * colSpacing;
        float gfY = getGfUiY();
        if (labelIdx < matchLabels.length) {
            int wbFinalWinner = getResult(data.wbResults, 2, 0);
            int lbFinalWinner = getResult(data.lbResults, 3, 0);
            boolean involvesPlayer = (wbFinalWinner == 0 || lbFinalWinner == 0);
            boolean isCurrent = data.currentBracket == TournamentManager.BRACKET_GF;
            boolean participantsKnown = wbFinalWinner >= 0 && lbFinalWinner >= 0;

            LabelAPI lbl = matchLabels[labelIdx];
            lbl.getPosition().inTL(gfX + 4f, gfY + 2f);
            lbl.setOpacity(1f);

            if (involvesPlayer && participantsKnown) {
                String playerName = getDisplayName(0);
                String opponentName = wbFinalWinner == 0
                    ? getDisplayName(lbFinalWinner)
                    : getDisplayName(wbFinalWinner);
                String score = data.gfPlayerWins + " : " + data.gfOpponentWins;
                lbl.setText(truncate(playerName) + Strings.get("casino.tournament_vs_separator") + truncate(opponentName) + "\n" + score);

                if (data.playerChampion) {
                    lbl.setColor(COLOR_WINNER_TEXT);
                } else if (data.playerEliminated) {
                    lbl.setColor(COLOR_LOSER);
                } else if (isCurrent) {
                    lbl.setColor(Color.YELLOW);
                } else {
                    lbl.setColor(ColorHelper.PLAYER_NAME);
                }
            } else {
                String n0 = getDisplayName(wbFinalWinner);
                String n1 = getDisplayName(lbFinalWinner);
                lbl.setText(truncate(n0) + Strings.get("casino.tournament_vs_separator") + truncate(n1));
                if (isCurrent) {
                    lbl.setColor(Color.YELLOW);
                } else if (involvesPlayer) {
                    lbl.setColor(ColorHelper.PLAYER_NAME);
                } else {
                    lbl.setColor(COLOR_PENDING);
                }
            }
            labelIdx++;
        }

        for (int i = labelIdx; i < matchLabels.length; i++) {
            matchLabels[i].setOpacity(0f);
        }

        updateStatusLabel();
    }

    private void configureMatchLabel(int idx, float x, float y, int slot0, int slot1, int winner,
                                     boolean involvesPlayer, boolean isCurrent, boolean completed) {
        if (idx >= matchLabels.length) return;
        LabelAPI lbl = matchLabels[idx];
        lbl.getPosition().inTL(x + 4f, y + 2f);
        lbl.setOpacity(1f);

        if (completed) {
            String winnerName = getDisplayName(winner);
            String loserName = getDisplayName(winner == slot0 ? slot1 : slot0);
                lbl.setText(truncate(winnerName) + Strings.get("casino.tournament_vs_separator") + truncate(loserName));
            if (involvesPlayer) {
                lbl.setColor(winner == 0 ? COLOR_WINNER_TEXT : COLOR_LOSER);
            } else {
                lbl.setColor(COLOR_SIMULATED);
            }
        } else if (slot0 >= 0 && slot1 >= 0) {
            String name0 = getDisplayName(slot0);
            String name1 = getDisplayName(slot1);
            lbl.setText(truncate(name0) + Strings.get("casino.tournament_vs_separator") + truncate(name1));
            if (isCurrent) {
                lbl.setColor(Color.YELLOW);
            } else if (involvesPlayer) {
                lbl.setColor(ColorHelper.PLAYER_NAME);
            } else {
                lbl.setColor(COLOR_PENDING);
            }
        } else {
            lbl.setText(Strings.get("casino.tournament_tbd"));
            lbl.setColor(COLOR_SIMULATED);
        }
    }

    private void updateStatusLabel() {
        if (statusLabel == null || data == null) return;

        String playerName = getDisplayName(0);
        String position = getPlayerBracketPosition();
        statusLabel.setText(Strings.format("casino.tournament_status",
            playerName, data.playerWins, data.playerLosses, position));

        if (data.playerChampion) {
            statusLabel.setColor(COLOR_WINNER_TEXT);
        } else if (data.playerEliminated) {
            statusLabel.setColor(COLOR_LOSER);
        } else {
            statusLabel.setColor(Color.WHITE);
        }
    }

    private String getPlayerBracketPosition() {
        if (data.playerChampion) return Strings.get("casino.tournament_position_champion");
        if (data.playerEliminated) return Strings.get("casino.tournament_position_eliminated");
        return switch (data.currentBracket) {
            case TournamentManager.BRACKET_WB -> {
                if (data.currentRound == 2) yield Strings.get("casino.tournament_position_wb_final");
                yield Strings.format("casino.tournament_position_wb_round", data.currentRound + 1);
            }
            case TournamentManager.BRACKET_LB -> {
                if (data.currentRound == 3) yield Strings.get("casino.tournament_position_lb_final");
                yield Strings.format("casino.tournament_position_lb_round", data.currentRound + 1);
            }
            case TournamentManager.BRACKET_GF -> Strings.get("casino.tournament_position_grand_final");
            default -> Strings.get("casino.tournament_position_unknown");
        };
    }

    private int getSlot(int[][] matchups, int round, int match, int side) {
        if (matchups == null || round >= matchups.length) return -1;
        int idx = match * 2 + side;
        if (idx >= matchups[round].length) return -1;
        return matchups[round][idx];
    }

    private int getResult(int[][] results, int round, int match) {
        if (results == null || round >= results.length) return -1;
        if (match >= results[round].length) return -1;
        return results[round][match];
    }

    private boolean isCurrentMatch(int bracket, int round, int matchIndex) {
        return data.currentBracket == bracket
            && data.currentRound == round
            && data.currentMatchIndex == matchIndex
            && !data.playerEliminated
            && !data.playerChampion;
    }

    private String getDisplayName(int slot) {
        if (slot < 0 || slot >= 8) return Strings.get("casino.tournament_placeholder_name");
        if (displayNames != null && slot < displayNames.length && displayNames[slot] != null) {
            return displayNames[slot];
        }
        if (data.playerNames != null && slot < data.playerNames.length && data.playerNames[slot] != null) {
            return data.playerNames[slot];
        }
        return Strings.format("casino.tournament_slot_name", slot + 1);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() <= MAX_NAME_LEN ? text : text.substring(0, MAX_NAME_LEN - 1) + ".";
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panel == null || data == null) return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float h = pos.getHeight();

        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContext = existingCtx == null;
        if (needsContext) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(x, y, panelW, h));
        }

        try {
            GLStateUtil.resetBlendState();
            Misc.renderQuad(x, y, panelW, h, COLOR_BG, alphaMult);

            renderAllMatchBoxes(alphaMult);
            renderLbDivider(alphaMult);
            renderGfBox(alphaMult);
            renderBracketLines(alphaMult);

            GLStateUtil.resetColor();
        } finally {
            if (needsContext) {
                UnifiedCoord.clearCurrent();
            }
        }
    }

    private void renderAllMatchBoxes(float alpha) {
        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);

        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < WB_COUNTS[r]; m++) {
                float uiX = baseX + r * colSpacing;
                float uiY = getWbMatchUiY(r, m);
                boolean involvesPlayer = isPlayerInMatch(data.wbMatchups, r, m);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_WB, r, m);
                renderMatchBox(uiToGlX(uiX), uiToGlMatchY(uiY), involvesPlayer, isCurrent, alpha);
            }
        }

        for (int r = 0; r < 4; r++) {
            for (int m = 0; m < LB_COUNTS[r]; m++) {
                float uiX = baseX + r * colSpacing;
                float uiY = getLbMatchUiY(r, m);
                boolean involvesPlayer = isPlayerInMatch(data.lbMatchups, r, m);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_LB, r, m);
                renderMatchBox(uiToGlX(uiX), uiToGlMatchY(uiY), involvesPlayer, isCurrent, alpha);
            }
        }

        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    private boolean isPlayerInMatch(int[][] matchups, int round, int match) {
        int slot0 = getSlot(matchups, round, match, 0);
        int slot1 = getSlot(matchups, round, match, 1);
        return slot0 == 0 || slot1 == 0;
    }

    private void renderMatchBox(float mx, float my, boolean involvesPlayer, boolean isCurrent, float alpha) {
        Color bgColor = involvesPlayer ? COLOR_PLAYER_MATCH : COLOR_MATCH_BG;
        Misc.renderQuad(mx, my, MATCH_W, MATCH_H, bgColor, alpha);

        Color borderColor = isCurrent ? COLOR_CURRENT_BORDER
            : (involvesPlayer ? COLOR_PLAYER_BORDER : COLOR_MATCH_BORDER);
        renderRectBorder(mx, my, borderColor, alpha);
    }

    private void renderGfBox(float alpha) {
        float gfX = baseX + 3f * colSpacing;
        float gfY = getGfUiY();

        int wbFinalWinner = getResult(data.wbResults, 2, 0);
        int lbFinalWinner = getResult(data.lbResults, 3, 0);
        boolean involvesPlayer = wbFinalWinner == 0 || lbFinalWinner == 0;
        boolean isCurrent = data.currentBracket == TournamentManager.BRACKET_GF;

        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        renderMatchBox(uiToGlX(gfX), uiToGlMatchY(gfY), involvesPlayer, isCurrent, alpha);
        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    private void renderLbDivider(float alpha) {
        float divY = lbBaseY - 10f;
        GLStateUtil.resetBlendState();
        Misc.renderQuad(uiToGlX(10f), uiToGlY(divY), panelW - 20f, 2f, COLOR_LB_DIVIDER, alpha);
    }

    private void renderBracketLines(float alpha) {
        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);
        GL11.glBegin(GL11.GL_LINES);

        for (int r = 0; r < 2; r++) {
            float x1 = uiToGlX(baseX + r * colSpacing + MATCH_W);
            float x2 = uiToGlX(baseX + (r + 1) * colSpacing);
            float midX = uiToGlX((baseX + r * colSpacing + MATCH_W + baseX + (r + 1) * colSpacing) / 2f);
            for (int m = 0; m < WB_COUNTS[r]; m += 2) {
                float y0 = uiToGlY(getWbMatchUiY(r, m) + MATCH_H / 2f);
                float y1 = uiToGlY(getWbMatchUiY(r, m + 1) + MATCH_H / 2f);
                float yMid = (y0 + y1) / 2f;
                float targetY = uiToGlY(getWbMatchUiY(r + 1, m / 2) + MATCH_H / 2f);

                GL11.glVertex2f(x1, y0); GL11.glVertex2f(midX, y0);
                GL11.glVertex2f(x1, y1); GL11.glVertex2f(midX, y1);
                GL11.glVertex2f(midX, y0); GL11.glVertex2f(midX, y1);
                GL11.glVertex2f(midX, yMid); GL11.glVertex2f(x2, targetY);
            }
        }

        float wbFinalX = uiToGlX(baseX + 2f * colSpacing + MATCH_W);
        float wbFinalY = uiToGlY(getWbMatchUiY(2, 0) + MATCH_H / 2f);
        float gfX = uiToGlX(baseX + 3f * colSpacing);
        float gfY = uiToGlY(getGfUiY() + MATCH_H / 2f);
        GL11.glVertex2f(wbFinalX, wbFinalY); GL11.glVertex2f(gfX, gfY);

        float lbR1XEnd = uiToGlX(baseX + MATCH_W);
        float lbR2XStart = uiToGlX(baseX + colSpacing);
        for (int m = 0; m < 2; m++) {
            float y = uiToGlY(getLbMatchUiY(0, m) + MATCH_H / 2f);
            GL11.glVertex2f(lbR1XEnd, y); GL11.glVertex2f(lbR2XStart, y);
        }

        float lbR2XEnd = uiToGlX(baseX + colSpacing + MATCH_W);
        float lbR3XStart = uiToGlX(baseX + 2f * colSpacing);
        float lbMidX = uiToGlX((baseX + colSpacing + MATCH_W + baseX + 2f * colSpacing) / 2f);
        float lbY0 = uiToGlY(getLbMatchUiY(1, 0) + MATCH_H / 2f);
        float lbY1 = uiToGlY(getLbMatchUiY(1, 1) + MATCH_H / 2f);
        float lbYMid = (lbY0 + lbY1) / 2f;
        float lbR3Y = uiToGlY(getLbMatchUiY(2, 0) + MATCH_H / 2f);

        GL11.glVertex2f(lbR2XEnd, lbY0); GL11.glVertex2f(lbMidX, lbY0);
        GL11.glVertex2f(lbR2XEnd, lbY1); GL11.glVertex2f(lbMidX, lbY1);
        GL11.glVertex2f(lbMidX, lbY0); GL11.glVertex2f(lbMidX, lbY1);
        GL11.glVertex2f(lbMidX, lbYMid); GL11.glVertex2f(lbR3XStart, lbR3Y);

        float lbR3XEnd = uiToGlX(baseX + 2f * colSpacing + MATCH_W);
        float lbFinalXStart = uiToGlX(baseX + 3f * colSpacing);
        float lbR3FinalY = uiToGlY(getLbMatchUiY(2, 0) + MATCH_H / 2f);
        GL11.glVertex2f(lbR3XEnd, lbR3FinalY); GL11.glVertex2f(lbFinalXStart, lbR3FinalY);

        float lbFinalX = uiToGlX(baseX + 3f * colSpacing + MATCH_W);
        float lbFinalY = uiToGlY(getLbMatchUiY(3, 0) + MATCH_H / 2f);
        GL11.glVertex2f(lbFinalX, lbFinalY); GL11.glVertex2f(gfX, gfY);

        GL11.glEnd();
        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    private void renderRectBorder(float rx, float ry, Color color, float alpha) {
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
            (color.getAlpha() / 255f) * alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(rx, ry);
        GL11.glVertex2f(rx + MATCH_W, ry);
        GL11.glVertex2f(rx + MATCH_W, ry + MATCH_H);
        GL11.glVertex2f(rx, ry + MATCH_H);
        GL11.glEnd();
    }

    public void cleanup() {
        if (titleLabel != null) {
            panel.removeComponent((UIComponentAPI) titleLabel);
            titleLabel = null;
        }
        if (matchLabels != null) {
            for (LabelAPI lbl : matchLabels) {
                if (lbl != null) panel.removeComponent((UIComponentAPI) lbl);
            }
            matchLabels = null;
        }
        if (statusLabel != null) {
            panel.removeComponent((UIComponentAPI) statusLabel);
            statusLabel = null;
        }
        removeHeader(wbHeaderR1); wbHeaderR1 = null;
        removeHeader(wbHeaderR2); wbHeaderR2 = null;
        removeHeader(wbHeaderFinal); wbHeaderFinal = null;
        removeHeader(lbHeaderR1); lbHeaderR1 = null;
        removeHeader(lbHeaderR2); lbHeaderR2 = null;
        removeHeader(lbHeaderR3); lbHeaderR3 = null;
        removeHeader(lbHeaderFinal); lbHeaderFinal = null;
        removeHeader(gfHeader); gfHeader = null;
        panel = null;
        data = null;
        displayNames = null;
    }

    private void removeHeader(LabelAPI label) {
        if (label != null && panel != null) {
            panel.removeComponent((UIComponentAPI) label);
        }
    }
}

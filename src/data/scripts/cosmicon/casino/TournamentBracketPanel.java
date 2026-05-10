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

    private static final int WB_TOTAL_MATCHES = 7;
    private static final int LB_TOTAL_MATCHES = 6;
    private static final int GF_TOTAL_MATCHES = 1;
    private static final int TOTAL_MATCH_LABELS = WB_TOTAL_MATCHES + LB_TOTAL_MATCHES + GF_TOTAL_MATCHES;

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

        float usableH = panelH - WB_TOP_Y - STATUS_BAR_H - 20f;
        rowSpacing = Math.max(96f, usableH / 7f);
    }

    private float rowSpacingForLb() {
        return rowSpacing * 0.55f;
    }

    private void createTitleLabel() {
        titleLabel = Global.getSettings().createLabel(
            "Tournament Bracket - 8 Players", Fonts.INSIGNIA_LARGE);
        titleLabel.setColor(Color.YELLOW);
        titleLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) titleLabel)
            .setSize(panelW, 30f)
            .inTL(0f, 8f);
    }

    private void createColumnHeaders() {
        wbHeaderR1 = createHeaderLabel("WB R1", baseX, 38f);
        wbHeaderR2 = createHeaderLabel("WB R2", baseX + colSpacing, 38f);
        wbHeaderFinal = createHeaderLabel("WB Final", baseX + 2f * colSpacing, 38f);
        gfHeader = createHeaderLabel("Grand Final", baseX + 3f * colSpacing, 38f);

        float lbHeaderY = getLbBaseY() + 3f * rowSpacing + rowSpacing + 16f;
        lbHeaderR1 = createHeaderLabel("LB R1", baseX, lbHeaderY);
        lbHeaderR2 = createHeaderLabel("LB R2", baseX + colSpacing, lbHeaderY);
        lbHeaderR3 = createHeaderLabel("LB R3", baseX + 2f * colSpacing, lbHeaderY);
        lbHeaderFinal = createHeaderLabel("LB Final", baseX + 3f * colSpacing, lbHeaderY);
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

    private float getLbBaseY() {
        return 20f + STATUS_BAR_H;
    }

    private float getWbMatchUiY(int matchIndex) {
        return WB_TOP_Y + matchIndex * rowSpacing;
    }

    private float getLbMatchUiY(int round, int matchIndex) {
        float baseY = getLbBaseY();
        return baseY + round * rowSpacing + matchIndex * rowSpacingForLb();
    }

    private float getGfUiY() {
        return getWbMatchUiY(0) + 10f;
    }

    private void updateAllLabels() {
        if (data == null) {
            titleLabel.setText("Tournament Bracket - No Data");
            statusLabel.setText("No tournament data available");
            return;
        }

        int labelIdx = 0;

        int[] wbCounts = {4, 2, 1};

        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < wbCounts[r]; m++) {
                if (labelIdx >= matchLabels.length) break;
                float lx = baseX + r * colSpacing;
                float ly = getWbMatchUiY(m);
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

        int[] lbCounts = {2, 2, 1, 1};
        for (int r = 0; r < 4; r++) {
            for (int m = 0; m < lbCounts[r]; m++) {
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
                lbl.setText(truncate(playerName) + " vs " + truncate(opponentName) + "\n" + score);

                if (data.playerChampion) {
                    lbl.setColor(COLOR_WINNER_TEXT);
                } else if (data.playerEliminated) {
                    lbl.setColor(new Color(255, 100, 100));
                } else if (isCurrent) {
                    lbl.setColor(Color.YELLOW);
                } else {
                    lbl.setColor(ColorHelper.PLAYER_NAME);
                }
            } else {
                String n0 = getDisplayName(wbFinalWinner);
                String n1 = getDisplayName(lbFinalWinner);
                lbl.setText(truncate(n0) + " vs " + truncate(n1));
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
            lbl.setText(truncate(winnerName) + " vs " + truncate(loserName));
            if (involvesPlayer) {
                lbl.setColor(winner == 0 ? COLOR_WINNER_TEXT : new Color(255, 100, 100));
            } else {
                lbl.setColor(COLOR_SIMULATED);
            }
        } else if (slot0 >= 0 && slot1 >= 0) {
            String name0 = getDisplayName(slot0);
            String name1 = getDisplayName(slot1);
            lbl.setText(truncate(name0) + " vs " + truncate(name1));
            if (isCurrent) {
                lbl.setColor(Color.YELLOW);
            } else if (involvesPlayer) {
                lbl.setColor(ColorHelper.PLAYER_NAME);
            } else {
                lbl.setColor(COLOR_PENDING);
            }
        } else {
            lbl.setText("TBD");
            lbl.setColor(COLOR_SIMULATED);
        }
    }

    private void updateStatusLabel() {
        if (statusLabel == null || data == null) return;

        String playerName = getDisplayName(0);
        String position = getPlayerBracketPosition();
        statusLabel.setText("Player: " + playerName
            + "  |  Wins: " + data.playerWins
            + "  |  Losses: " + data.playerLosses
            + "  |  Status: " + position);

        if (data.playerChampion) {
            statusLabel.setColor(COLOR_WINNER_TEXT);
        } else if (data.playerEliminated) {
            statusLabel.setColor(new Color(255, 100, 100));
        } else {
            statusLabel.setColor(Color.WHITE);
        }
    }

    private String getPlayerBracketPosition() {
        if (data.playerChampion) return "Champion";
        if (data.playerEliminated) return "Eliminated";
        return switch (data.currentBracket) {
            case TournamentManager.BRACKET_WB -> {
                if (data.currentRound == 2) yield "WB Final";
                yield "WB Round " + (data.currentRound + 1);
            }
            case TournamentManager.BRACKET_LB -> {
                if (data.currentRound == 3) yield "LB Final";
                yield "LB Round " + (data.currentRound + 1);
            }
            case TournamentManager.BRACKET_GF -> "Grand Final";
            default -> "Unknown";
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
        if (slot < 0 || slot >= 8) return "???";
        if (displayNames != null && slot < displayNames.length && displayNames[slot] != null) {
            return displayNames[slot];
        }
        if (data.playerNames != null && slot < data.playerNames.length && data.playerNames[slot] != null) {
            return data.playerNames[slot];
        }
        return "P" + (slot + 1);
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
        int[] wbCounts = {4, 2, 1};
        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < wbCounts[r]; m++) {
                float uiX = baseX + r * colSpacing;
                float uiY = getWbMatchUiY(m);
                UnifiedCoord coord = new UnifiedCoord(uiX, uiY);
                boolean involvesPlayer = isPlayerInMatch(data.wbMatchups, r, m);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_WB, r, m);
                GLStateUtil.resetBlendState();
                renderMatchBox(coord.glX(), coord.glSpriteY(MATCH_H), involvesPlayer, isCurrent, alpha);
            }
        }

        int[] lbCounts = {2, 2, 1, 1};
        for (int r = 0; r < 4; r++) {
            for (int m = 0; m < lbCounts[r]; m++) {
                float uiX = baseX + r * colSpacing;
                float uiY = getLbMatchUiY(r, m);
                UnifiedCoord coord = new UnifiedCoord(uiX, uiY);
                boolean involvesPlayer = isPlayerInMatch(data.lbMatchups, r, m);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_LB, r, m);
                GLStateUtil.resetBlendState();
                renderMatchBox(coord.glX(), coord.glSpriteY(MATCH_H), involvesPlayer, isCurrent, alpha);
            }
        }
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
        UnifiedCoord coord = new UnifiedCoord(gfX, gfY);

        int wbFinalWinner = getResult(data.wbResults, 2, 0);
        int lbFinalWinner = getResult(data.lbResults, 3, 0);
        boolean involvesPlayer = wbFinalWinner == 0 || lbFinalWinner == 0;
        boolean isCurrent = data.currentBracket == TournamentManager.BRACKET_GF;

        GLStateUtil.resetBlendState();
        Color bgColor = involvesPlayer ? COLOR_PLAYER_MATCH : COLOR_MATCH_BG;
        Misc.renderQuad(coord.glX(), coord.glSpriteY(MATCH_H), MATCH_W, MATCH_H, bgColor, alpha);

        Color borderColor = isCurrent ? COLOR_CURRENT_BORDER
            : (involvesPlayer ? COLOR_PLAYER_BORDER : COLOR_MATCH_BORDER);
        renderRectBorder(coord.glX(), coord.glSpriteY(MATCH_H), borderColor, alpha);
    }

    private void renderLbDivider(float alpha) {
        float divY = WB_TOP_Y + 3f * rowSpacing + MATCH_H + 10f;
        UnifiedCoord coord = new UnifiedCoord(10f, divY);
        GLStateUtil.resetBlendState();
        Misc.renderQuad(coord.glX(), coord.glY(), panelW - 20f, 2f, COLOR_LB_DIVIDER, alpha);
    }

    private void renderBracketLines(float alpha) {
        int[] wbCounts = {4, 2, 1};
        for (int r = 0; r < 2; r++) {
            float x1Ui = baseX + r * colSpacing + MATCH_W;
            float x2Ui = baseX + (r + 1) * colSpacing;
            float midXUi = (x1Ui + x2Ui) / 2f;
            UnifiedCoord x1Coord = new UnifiedCoord(x1Ui, 0f);
            UnifiedCoord x2Coord = new UnifiedCoord(x2Ui, 0f);
            UnifiedCoord midXCoord = new UnifiedCoord(midXUi, 0f);
            for (int m = 0; m < wbCounts[r]; m += 2) {
                GLStateUtil.resetBlendState();
                GL11.glLineWidth(1.5f);
                GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);

                float y0Ui = getWbMatchUiY(m) + MATCH_H / 2f;
                float y1Ui = getWbMatchUiY(m + 1) + MATCH_H / 2f;
                float yMidUi = (y0Ui + y1Ui) / 2f;
                float targetYUi = getWbMatchUiY(m / 2) + MATCH_H / 2f;

                UnifiedCoord y0Coord = new UnifiedCoord(0f, y0Ui);
                UnifiedCoord y1Coord = new UnifiedCoord(0f, y1Ui);
                UnifiedCoord yMidCoord = new UnifiedCoord(0f, yMidUi);
                UnifiedCoord targetCoord = new UnifiedCoord(0f, targetYUi);

                drawLine(x1Coord.glX(), y0Coord.glY(), midXCoord.glX(), y0Coord.glY());
                drawLine(x1Coord.glX(), y1Coord.glY(), midXCoord.glX(), y1Coord.glY());
                drawLine(midXCoord.glX(), y0Coord.glY(), midXCoord.glX(), y1Coord.glY());
                drawLine(midXCoord.glX(), yMidCoord.glY(), x2Coord.glX(), targetCoord.glY());
            }
        }

        float wbFinalXUi = baseX + 2f * colSpacing + MATCH_W;
        float wbFinalYUi = getWbMatchUiY(0) + MATCH_H / 2f;
        float gfXUi = baseX + 3f * colSpacing;
        float gfYUi = getGfUiY() + MATCH_H / 2f;
        UnifiedCoord wbFinalCoord = new UnifiedCoord(wbFinalXUi, wbFinalYUi);
        UnifiedCoord gfCoord = new UnifiedCoord(gfXUi, gfYUi);

        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);
        drawLine(wbFinalCoord.glX(), wbFinalCoord.glY(), gfCoord.glX(), gfCoord.glY());

        int[] lbCounts = {2, 2, 1, 1};
        for (int r = 0; r < 3; r++) {
            float x1Ui = baseX + r * colSpacing + MATCH_W;
            float x2Ui = baseX + (r + 1) * colSpacing;
            float midXUi = (x1Ui + x2Ui) / 2f;
            UnifiedCoord x1Coord = new UnifiedCoord(x1Ui, 0f);
            UnifiedCoord x2Coord = new UnifiedCoord(x2Ui, 0f);
            UnifiedCoord midXCoord = new UnifiedCoord(midXUi, 0f);
            for (int m = 0; m < lbCounts[r]; m++) {
                GLStateUtil.resetBlendState();
                GL11.glLineWidth(1.5f);
                GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);

                float ySrcUi = getLbMatchUiY(r, m) + MATCH_H / 2f;
                float yDstUi = getLbMatchUiY(r + 1, 0) + MATCH_H / 2f;

                UnifiedCoord srcCoord = new UnifiedCoord(0f, ySrcUi);
                UnifiedCoord dstCoord = new UnifiedCoord(0f, yDstUi);

                drawLine(x1Coord.glX(), srcCoord.glY(), midXCoord.glX(), srcCoord.glY());
                drawLine(midXCoord.glX(), srcCoord.glY(), midXCoord.glX(), dstCoord.glY());
                drawLine(midXCoord.glX(), dstCoord.glY(), x2Coord.glX(), dstCoord.glY());
            }
        }

        float lbFinalXUi = baseX + 3f * colSpacing + MATCH_W;
        float lbFinalYUi = getLbMatchUiY(3, 0) + MATCH_H / 2f;
        UnifiedCoord lbFinalCoord = new UnifiedCoord(lbFinalXUi, lbFinalYUi);

        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);
        drawLine(lbFinalCoord.glX(), lbFinalCoord.glY(), gfCoord.glX(), gfCoord.glY());

        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    private void drawLine(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    private void renderRectBorder(float rx, float ry, Color color, float alpha) {
        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
            (color.getAlpha() / 255f) * alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(rx, ry);
        GL11.glVertex2f(rx + MATCH_W, ry);
        GL11.glVertex2f(rx + MATCH_W, ry + MATCH_H);
        GL11.glVertex2f(rx, ry + MATCH_H);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
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

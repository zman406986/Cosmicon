package data.scripts.cosmicon.casino;

import java.awt.Color;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class TournamentBracketPanel extends BaseCustomUIPanelPlugin {

    private static final float MATCH_W = 120f;
    private static final float MATCH_H = 40f;
    private static final float COL_SPACING = 140f;
    private static final float ROW_SPACING = 50f;
    private static final float WB_TOP_Y = 60f;
    private static final float STATUS_BAR_H = 30f;

    private static final Color COLOR_BG = new Color(20, 22, 35, 220);
    private static final Color COLOR_MATCH_BG = new Color(40, 45, 65, 200);
    private static final Color COLOR_MATCH_BORDER = new Color(80, 90, 120, 200);
    private static final Color COLOR_PLAYER_MATCH = new Color(0, 100, 200, 80);
    private static final Color COLOR_PLAYER_BORDER = new Color(0, 150, 255, 200);
    private static final Color COLOR_CURRENT_BORDER = Color.YELLOW;
    private static final Color COLOR_WINNER_TEXT = new Color(100, 255, 100);
    private static final Color COLOR_LOSER_TEXT = new Color(120, 120, 120);
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

    public TournamentBracketPanel(TournamentManager.BracketData bracketData, String[] playerDisplayNames) {
        this.data = bracketData;
        this.displayNames = playerDisplayNames;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        PositionAPI pos = panel.getPosition();
        this.panelW = pos.getWidth();
        this.panelH = pos.getHeight();

        createTitleLabel();
        createColumnHeaders();
        createMatchLabels();
        createStatusLabel();
        updateAllLabels();
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
        float baseX = 30f;
        wbHeaderR1 = createHeaderLabel("WB R1", baseX, 38f);
        wbHeaderR2 = createHeaderLabel("WB R2", baseX + COL_SPACING, 38f);
        wbHeaderFinal = createHeaderLabel("WB Final", baseX + 2f * COL_SPACING, 38f);
        gfHeader = createHeaderLabel("Grand Final", baseX + 3f * COL_SPACING, 38f);

        float lbHeaderY = getLbBaseY() + 3f * ROW_SPACING + ROW_SPACING + 8f;
        lbHeaderR1 = createHeaderLabel("LB R1", baseX, lbHeaderY);
        lbHeaderR2 = createHeaderLabel("LB R2", baseX + COL_SPACING, lbHeaderY);
        lbHeaderR3 = createHeaderLabel("LB R3", baseX + 2f * COL_SPACING, lbHeaderY);
        lbHeaderFinal = createHeaderLabel("LB Final", baseX + 3f * COL_SPACING, lbHeaderY);
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
                .setSize(MATCH_W, 16f)
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

    private float getLbBaseY() {
        return 20f + STATUS_BAR_H;
    }

    private float getWbMatchUiY(int matchIndex) {
        return WB_TOP_Y + matchIndex * ROW_SPACING;
    }

    private float getLbMatchUiY(int round, int matchIndex) {
        float baseY = getLbBaseY();
        return baseY + round * ROW_SPACING + matchIndex * (ROW_SPACING / 2f);
    }

    private void updateAllLabels() {
        if (data == null) {
            titleLabel.setText("Tournament Bracket - No Data");
            statusLabel.setText("No tournament data available");
            return;
        }

        int labelIdx = 0;

        int[] wbCounts = {4, 2, 1};
        float baseX = 30f;

        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < wbCounts[r]; m++) {
                if (labelIdx >= matchLabels.length) break;
                float lx = baseX + r * COL_SPACING;
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
                float lx = baseX + r * COL_SPACING;
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

        float gfX = baseX + 3f * COL_SPACING;
        float gfY = getWbMatchUiY(0) - 20f;
        if (labelIdx < matchLabels.length) {
            int wbFinalWinner = getResult(data.wbResults, 2, 0);
            int lbFinalWinner = getResult(data.lbResults, 3, 0);
            boolean gfStarted = data.gfSeries != null && data.gfSeries[0] >= 0;
            boolean involvesPlayer = (wbFinalWinner == 0 || lbFinalWinner == 0);
            boolean isCurrent = data.currentBracket == TournamentManager.BRACKET_GF;

            LabelAPI lbl = matchLabels[labelIdx];
            lbl.getPosition().inTL(gfX, gfY + MATCH_H / 2f - 8f);
            lbl.setOpacity(1f);

            if (gfStarted) {
                String score = data.gfPlayerWins + " - " + data.gfOpponentWins;
                lbl.setText("GF: " + score);
                lbl.setColor(data.playerChampion ? COLOR_WINNER_TEXT : COLOR_LOSER_TEXT);
            } else {
                String n0 = getDisplayName(wbFinalWinner);
                String n1 = getDisplayName(lbFinalWinner);
                lbl.setText(truncate(n0, 8) + " vs " + truncate(n1, 8));
                lbl.setColor(isCurrent ? Color.YELLOW : COLOR_PENDING);
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
        lbl.getPosition().inTL(x, y + MATCH_H / 2f - 8f);
        lbl.setOpacity(1f);

        if (completed) {
            String winnerName = getDisplayName(winner);
            String loserName = getDisplayName(winner == slot0 ? slot1 : slot0);
            lbl.setText(truncate(winnerName, 10) + " vs " + truncate(loserName, 10));
            if (involvesPlayer) {
                lbl.setColor(winner == 0 ? COLOR_WINNER_TEXT : new Color(255, 100, 100));
            } else {
                lbl.setColor(COLOR_SIMULATED);
            }
        } else if (slot0 >= 0 && slot1 >= 0) {
            String name0 = getDisplayName(slot0);
            String name1 = getDisplayName(slot1);
            lbl.setText(truncate(name0, 10) + " vs " + truncate(name1, 10));
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

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + ".";
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panel == null || data == null) return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContext = existingCtx == null;
        if (needsContext) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(x, y, w, h));
        }

        try {
            GLStateUtil.resetBlendState();
            Misc.renderQuad(x, y, w, h, COLOR_BG, alphaMult);

            renderAllMatchBoxes(x, y, w, h, alphaMult);
            renderLbDivider(x, y, w, h, alphaMult);
            renderGfBox(x, y, w, h, alphaMult);
            renderBracketLines(x, y, w, h, alphaMult);

            GLStateUtil.resetColor();
        } finally {
            if (needsContext) {
                UnifiedCoord.clearCurrent();
            }
        }
    }

    private void renderAllMatchBoxes(float px, float py, float pw, float ph, float alpha) {
        float baseX = px + 30f;

        int[] wbCounts = {4, 2, 1};
        for (int r = 0; r < 3; r++) {
            for (int m = 0; m < wbCounts[r]; m++) {
                float mx = baseX + r * COL_SPACING;
                float my = py + ph - WB_TOP_Y - m * ROW_SPACING - MATCH_H;
                int slot0 = getSlot(data.wbMatchups, r, m, 0);
                int slot1 = getSlot(data.wbMatchups, r, m, 1);
                int winner = getResult(data.wbResults, r, m);
                boolean involvesPlayer = (slot0 == 0 || slot1 == 0);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_WB, r, m);
                renderMatchBox(mx, my, slot0, slot1, winner, involvesPlayer, isCurrent, alpha);
            }
        }

        int[] lbCounts = {2, 2, 1, 1};
        for (int r = 0; r < 4; r++) {
            for (int m = 0; m < lbCounts[r]; m++) {
                float mx = baseX + r * COL_SPACING;
                float my = py + ph - getLbMatchUiY(r, m) - MATCH_H;
                int slot0 = getSlot(data.lbMatchups, r, m, 0);
                int slot1 = getSlot(data.lbMatchups, r, m, 1);
                int winner = getResult(data.lbResults, r, m);
                boolean involvesPlayer = (slot0 == 0 || slot1 == 0);
                boolean isCurrent = isCurrentMatch(TournamentManager.BRACKET_LB, r, m);
                renderMatchBox(mx, my, slot0, slot1, winner, involvesPlayer, isCurrent, alpha);
            }
        }
    }

    private void renderMatchBox(float mx, float my, int slot0, int slot1, int winner,
                                boolean involvesPlayer, boolean isCurrent, float alpha) {
        GLStateUtil.resetBlendState();

        Color bgColor = involvesPlayer ? COLOR_PLAYER_MATCH : COLOR_MATCH_BG;
        Misc.renderQuad(mx, my, MATCH_W, MATCH_H, bgColor, alpha);

        Color borderColor = isCurrent ? COLOR_CURRENT_BORDER
            : (involvesPlayer ? COLOR_PLAYER_BORDER : COLOR_MATCH_BORDER);
        renderRectBorder(mx, my, MATCH_W, MATCH_H, borderColor, alpha);
    }

    private void renderGfBox(float px, float py, float pw, float ph, float alpha) {
        float baseX = px + 30f;
        float gfX = baseX + 3f * COL_SPACING;
        float gfY = py + ph - WB_TOP_Y - 20f - MATCH_H;

        int wbFinalWinner = getResult(data.wbResults, 2, 0);
        int lbFinalWinner = getResult(data.lbResults, 3, 0);
        boolean involvesPlayer = (wbFinalWinner == 0 || lbFinalWinner == 0);
        boolean isCurrent = data.currentBracket == TournamentManager.BRACKET_GF;

        GLStateUtil.resetBlendState();
        Color bgColor = involvesPlayer ? COLOR_PLAYER_MATCH : COLOR_MATCH_BG;
        Misc.renderQuad(gfX, gfY, MATCH_W, MATCH_H, bgColor, alpha);

        Color borderColor = isCurrent ? COLOR_CURRENT_BORDER
            : (involvesPlayer ? COLOR_PLAYER_BORDER : COLOR_MATCH_BORDER);
        renderRectBorder(gfX, gfY, MATCH_W, MATCH_H, borderColor, alpha);
    }

    private void renderLbDivider(float px, float py, float pw, float ph, float alpha) {
        float divY = py + ph - WB_TOP_Y - 3f * ROW_SPACING - MATCH_H - 10f;
        GLStateUtil.resetBlendState();
        Misc.renderQuad(px + 10f, divY, pw - 20f, 2f, COLOR_LB_DIVIDER, alpha);
    }

    private void renderBracketLines(float px, float py, float pw, float ph, float alpha) {
        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alpha);

        float baseX = px + 30f;
        float topY = py + ph;

        int[] wbCounts = {4, 2, 1};
        for (int r = 0; r < 2; r++) {
            float x1 = baseX + r * COL_SPACING + MATCH_W;
            float x2 = baseX + (r + 1) * COL_SPACING;
            float midX = (x1 + x2) / 2f;
            for (int m = 0; m < wbCounts[r]; m += 2) {
                float y0 = topY - WB_TOP_Y - m * ROW_SPACING - MATCH_H / 2f;
                float y1 = topY - WB_TOP_Y - (m + 1) * ROW_SPACING - MATCH_H / 2f;
                float yMid = (y0 + y1) / 2f;
                float targetY = topY - WB_TOP_Y - (m / 2) * ROW_SPACING - MATCH_H / 2f;

                drawLine(x1, y0, midX, y0);
                drawLine(x1, y1, midX, y1);
                drawLine(midX, y0, midX, y1);
                drawLine(midX, yMid, x2, targetY);
            }
        }

        float wbFinalX = baseX + 2f * COL_SPACING + MATCH_W;
        float wbFinalY = topY - WB_TOP_Y - MATCH_H / 2f;
        float gfX = baseX + 3f * COL_SPACING;
        float gfY = topY - WB_TOP_Y - 20f - MATCH_H / 2f;
        drawLine(wbFinalX, wbFinalY, gfX, gfY);

        int[] lbCounts = {2, 2, 1, 1};
        for (int r = 0; r < 3; r++) {
            float x1 = baseX + r * COL_SPACING + MATCH_W;
            float x2 = baseX + (r + 1) * COL_SPACING;
            float midX = (x1 + x2) / 2f;
            for (int m = 0; m < lbCounts[r]; m++) {
                float ySrc = topY - getLbMatchUiY(r, m) - MATCH_H / 2f;
                int dstMatch = Math.min(m / 2, lbCounts[r + 1] - 1);
                float yDst = topY - getLbMatchUiY(r + 1, dstMatch) - MATCH_H / 2f;

                drawLine(x1, ySrc, midX, ySrc);
                drawLine(midX, ySrc, midX, yDst);
                drawLine(midX, yDst, x2, yDst);
            }
        }

        float lbFinalX = baseX + 3f * COL_SPACING + MATCH_W;
        float lbFinalY = topY - getLbMatchUiY(3, 0) - MATCH_H / 2f;
        drawLine(lbFinalX, lbFinalY, gfX, gfY);

        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    private void drawLine(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    private void renderRectBorder(float rx, float ry, float rw, float rh, Color color, float alpha) {
        GLStateUtil.resetBlendState();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
            (color.getAlpha() / 255f) * alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(rx, ry);
        GL11.glVertex2f(rx + rw, ry);
        GL11.glVertex2f(rx + rw, ry + rh);
        GL11.glVertex2f(rx, ry + rh);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        GLStateUtil.resetColor();
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    public void refreshData(TournamentManager.BracketData newData) {
        this.data = newData;
        updateAllLabels();
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

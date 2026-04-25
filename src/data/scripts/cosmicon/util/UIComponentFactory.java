package data.scripts.cosmicon.util;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.ActionListenerDelegate;

public final class UIComponentFactory {

    private static final SettingsAPI settings = Global.getSettings();

    private UIComponentFactory() {}

    public static LabelAPI createLabel(CustomPanelAPI panel, String text, String font, 
        Color color, Alignment align, float width, float height, float x, float y) {
        LabelAPI label = settings.createLabel(text, font);
        label.setColor(color);
        label.setAlignment(align);
        panel.addComponent((UIComponentAPI) label)
            .setSize(width, height)
            .inTL(x, y);
        return label;
    }

    public static LabelAPI createLabel(CustomPanelAPI panel, String text, String font,
        Color color, Alignment align, float x, float y) {
        return createLabel(panel, text, font, color, align, 200f, 20f, x, y);
    }

    public static LabelAPI createLabelSmall(CustomPanelAPI panel, String text, 
        Color color, Alignment align, float width, float height, float x, float y) {
        return createLabel(panel, text, Fonts.DEFAULT_SMALL, color, align, width, height, x, y);
    }

    public static LabelAPI createLabelLarge(CustomPanelAPI panel, String text,
        Color color, Alignment align, float width, float height, float x, float y) {
        return createLabel(panel, text, Fonts.INSIGNIA_LARGE, color, align, width, height, x, y);
    }

    public static LabelAPI createLabelWithOpacity(CustomPanelAPI panel, String text, String font,
        Color color, Alignment align, float width, float height, float x, float y, float opacity) {
        LabelAPI label = createLabel(panel, text, font, color, align, width, height, x, y);
        label.setOpacity(opacity);
        return label;
    }

    public static TooltipMakerAPI createTooltipForButtons(CustomPanelAPI panel,
        ActionListenerDelegate listener, float width, float height, float x, float y) {
        TooltipMakerAPI tooltip = panel.createUIElement(width, height, false);
        tooltip.setActionListenerDelegate(listener);
        panel.addUIElement(tooltip).inTL(x, y);
        return tooltip;
    }
}
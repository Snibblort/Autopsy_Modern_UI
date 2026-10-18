package com.modernui.autopsy.theme;

import com.modernui.autopsy.ModernUISettings.Theme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Color palettes inspired by popular editor themes, expressed as FlatLaf
 * variables. FlatLaf derives everything else (hover, borders, disabled text,
 * selection contrast) from these few base colors.
 */
public enum PalettePreset {

    NONE("None (plain theme)", Theme.LIGHT, null, null, null, null, null, null),
    DRACULA("Dracula", Theme.DARK, "#282A36", "#F8F8F2", "#343746", "#21222C", "#44475A", "#BD93F9"),
    NORD("Nord", Theme.DARK, "#2E3440", "#D8DEE9", "#3B4252", "#2B303B", "#434C5E", "#88C0D0"),
    ONE_DARK("One Dark", Theme.DARK, "#282C34", "#ABB2BF", "#21252B", "#21252B", "#3A3F4B", "#61AFEF"),
    SOLARIZED_DARK("Solarized Dark", Theme.DARK, "#002B36", "#93A1A1", "#073642", "#00252F", "#0A4351", "#268BD2"),
    GRUVBOX_DARK("Gruvbox Dark", Theme.DARK, "#282828", "#EBDBB2", "#32302F", "#1D2021", "#3C3836", "#D79921"),
    CATPPUCCIN_MOCHA("Catppuccin Mocha", Theme.DARK, "#1E1E2E", "#CDD6F4", "#313244", "#181825", "#45475A", "#CBA6F7"),
    MONOKAI("Monokai Pro", Theme.DARK, "#2D2A2E", "#FCFCFA", "#363337", "#221F22", "#403E41", "#FFD866"),
    GITHUB_DARK("GitHub Dark", Theme.DARK, "#0D1117", "#C9D1D9", "#161B22", "#010409", "#21262D", "#58A6FF"),
    MATERIAL_OCEAN("Material Ocean", Theme.DARK, "#0F111A", "#A6ACCD", "#1A1C25", "#090B10", "#1F2233", "#80CBC4"),
    PROFESSIONAL_FORENSICS("Professional Forensics", Theme.DARK, "#151C25", "#DCE5EF", "#1C2632", "#111821", "#263342", "#4BA3D9",
            "@selectionBackground", "#29445C", "@selectionForeground", "#F4F8FC",
            "Component.borderColor", "#354352", "Component.focusColor", "@accentColor",
            "Separator.foreground", "#303C49", "TabbedPane.underlineColor", "@accentColor",
            "TabbedPane.selectedBackground", "#202D3A", "ToolBar.background", "#171F29",
            "Table.gridColor", "#2A3642", "Tree.selectionBackground", "@selectionBackground",
            "Button.hoverBackground", "#314354", "ScrollBar.thumb", "#45596D", "ScrollBar.track", "#19222D",
            "Component.arc", "6", "Button.arc", "5", "ScrollBar.thumbArc", "4",
            "TableHeader.background", "#202D3A", "TableHeader.separatorColor", "#4BA3D9",
            "ToolTip.background", "#202D3A"),
    FORENSIC_NAVY("Forensic Navy", Theme.DARK, "#101826", "#D6E2F0", "#172234", "#0C1320", "#1E2B40", "#3DA5FF"),
    // Most Cyber-Sigilism styling comes from sigil.Sigil.extraDefaults(); these are the base colors.
    GOTHIC_SIGILISM("Cyber-Sigilism", Theme.DARK, "#0B0A10", "#F0EDF6", "#111019", "#07070A", "#191522", "#A855F7",
            "TabbedPane.tabType", "underlined", "Table.showVerticalLines", "false"),
    SOLARIZED_LIGHT("Solarized Light", Theme.LIGHT, "#FDF6E3", "#586E75", "#EEE8D5", "#F5EFDC", "#EEE8D5", "#268BD2"),
    GITHUB_LIGHT("GitHub Light", Theme.LIGHT, "#F6F8FA", "#24292F", "#FFFFFF", "#FFFFFF", "#F6F8FA", "#0969DA");

    private final String displayName;
    private final Theme base;
    private final Map<String, String> values;

    PalettePreset(String displayName, Theme base, String background, String foreground,
            String component, String menu, String button, String accent, String... extraValues) {
        this.displayName = displayName;
        this.base = base;
        Map<String, String> map = new LinkedHashMap<>();
        if (background != null) {
            map.put("@background", background);
            map.put("@foreground", foreground);
            map.put("@componentBackground", component);
            map.put("@menuBackground", menu);
            map.put("@buttonBackground", button);
            map.put("@accentColor", accent);
            map.put("@selectionBackground", accent);
            map.put("@selectionForeground", "#FFFFFF");
            map.put("Tree.background", "@background");
            map.put("Table.background", "@componentBackground");
            map.put("List.background", "@componentBackground");
            map.put("TextArea.background", "@componentBackground");
            map.put("TitlePane.background", "@menuBackground");
            map.put("TableHeader.background", "@menuBackground");
            map.put("TableHeader.foreground", "@foreground");
            map.put("TableHeader.separatorColor", "@accentColor");
            map.put("Table.selectionBackground", "@selectionBackground");
            map.put("Table.selectionForeground", "@selectionForeground");
            map.put("Tree.selectionBackground", "@selectionBackground");
            map.put("Tree.selectionForeground", "@selectionForeground");
            map.put("List.selectionBackground", "@selectionBackground");
            map.put("List.selectionForeground", "@selectionForeground");
            map.put("MenuBar.background", "@menuBackground");
            map.put("PopupMenu.background", "@menuBackground");
            map.put("Menu.selectionBackground", "@buttonBackground");
            map.put("MenuItem.selectionBackground", "@buttonBackground");
            map.put("ToolTip.background", "@menuBackground");
            map.put("ToolTip.foreground", "@foreground");
            map.put("ProgressBar.foreground", "@accentColor");
            if (extraValues.length % 2 != 0) {
                throw new IllegalArgumentException("Palette overrides must be key/value pairs");
            }
            for (int i = 0; i < extraValues.length; i += 2) {
                map.put(extraValues[i], extraValues[i + 1]);
            }
        }
        this.values = Collections.unmodifiableMap(map);
    }

    public Theme getBase() {
        return base;
    }

    /** FlatLaf property overrides for this palette (empty for NONE). */
    public Map<String, String> getValues() {
        return values;
    }

    /** The palette's accent, for swatches in the UI. */
    public String getAccentHex() {
        return values.get("@accentColor");
    }

    public String getBackgroundHex() {
        return values.get("@background");
    }

    @Override
    public String toString() {
        return displayName;
    }
}

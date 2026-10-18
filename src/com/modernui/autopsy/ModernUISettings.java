package com.modernui.autopsy;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.LookAndFeel;
import org.openide.util.NbPreferences;

/**
 * Stores the Modern UI choices in Autopsy's user preferences.
 */
public final class ModernUISettings {

    /** Available FlatLaf themes. */
    public enum Theme {
        LIGHT("Light", false),
        DARK("Dark", true),
        INTELLIJ("IntelliJ Light", false),
        DARCULA("Darcula", true);

        private final String displayName;
        private final boolean dark;

        Theme(String displayName, boolean dark) {
            this.displayName = displayName;
            this.dark = dark;
        }

        public boolean isDark() {
            return dark;
        }

        /** Class name NetBeans uses to start in this theme (like --laf). */
        public String lookAndFeelClassName() {
            switch (this) {
                case DARK:
                    return FlatDarkLaf.class.getName();
                case INTELLIJ:
                    return FlatIntelliJLaf.class.getName();
                case DARCULA:
                    return FlatDarculaLaf.class.getName();
                case LIGHT:
                default:
                    return FlatLightLaf.class.getName();
            }
        }

        public LookAndFeel create() {
            switch (this) {
                case DARK:
                    return new FlatDarkLaf();
                case INTELLIJ:
                    return new FlatIntelliJLaf();
                case DARCULA:
                    return new FlatDarculaLaf();
                case LIGHT:
                default:
                    return new FlatLightLaf();
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Accent colors used for selection, focus rings, checkboxes and buttons. */
    public enum Accent {
        DEFAULT("Theme default", null),
        BLUE("Blue", "#2675BF"),
        INDIGO("Indigo", "#4F5BD5"),
        PURPLE("Purple", "#8E44AD"),
        TEAL("Teal", "#0E9384"),
        GREEN("Green", "#2E9E5B"),
        ORANGE("Orange", "#E07A1F"),
        RED("Red", "#D64545"),
        GRAPHITE("Graphite", "#6B7280"),
        CUSTOM("Custom…", null);

        private final String displayName;
        private final String hex;

        Accent(String displayName, String hex) {
            this.displayName = displayName;
            this.hex = hex;
        }

        /** Preset hex color, or null for DEFAULT and CUSTOM. */
        public String getHex() {
            return hex;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** How rounded buttons, fields and scrollbars are. */
    public enum Corners {
        SQUARE("Square"),
        SUBTLE("Subtle"),
        ROUNDED("Rounded");

        private final String displayName;

        Corners(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Row height for tables and trees. */
    public enum Density {
        COMPACT("Compact", 20),
        COMFORTABLE("Comfortable", 24),
        SPACIOUS("Spacious", 28);

        private final String displayName;
        private final int rowHeight;

        Density(String displayName, int rowHeight) {
            this.displayName = displayName;
            this.rowHeight = rowHeight;
        }

        public int getRowHeight() {
            return rowHeight;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Style of tabs in tabbed panels such as the result and content viewers. */
    public enum TabStyle {
        UNDERLINED("Underlined"),
        CARD("Cards");

        private final String displayName;

        TabStyle(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** Grid lines drawn in tables. */
    public enum GridLines {
        NONE("None"),
        HORIZONTAL("Row lines"),
        BOTH("Full grid");

        private final String displayName;

        GridLines(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static final int MIN_FONT_DELTA = -2;
    public static final int MAX_FONT_DELTA = 4;

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_THEME = "theme";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_CUSTOM_ACCENT = "customAccent";
    private static final String KEY_CORNERS = "corners";
    private static final String KEY_DENSITY = "density";
    private static final String KEY_ZEBRA = "zebraRows";
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_DELTA = "fontSizeDelta";
    private static final String KEY_TAB_STYLE = "tabStyle";
    private static final String KEY_GRID = "gridLines";
    private static final String KEY_FIX_DARK = "fixDarkColors";

    private static final String DEFAULT_CUSTOM_ACCENT = "#2675BF";

    /*
     * NetBeans' own FlatLaf integration module reads these keys to decide
     * whether to draw the modern title bar (menu bar embedded in the title bar).
     */
    private static final String NB_FLATLAF_NODE = "org/netbeans/swing/laf/flatlaf";
    private static final String NB_USE_WINDOW_DECORATIONS = "useWindowDecorations";
    private static final String NB_UNIFIED_TITLE_BAR = "unifiedTitleBar";
    private static final String NB_MENU_BAR_EMBEDDED = "menuBarEmbedded";

    private static final ModernUISettings INSTANCE = new ModernUISettings();

    private final Preferences prefs = NbPreferences.forModule(ModernUISettings.class);

    private ModernUISettings() {
    }

    public static ModernUISettings getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        prefs.putBoolean(KEY_ENABLED, enabled);
    }

    public Theme getTheme() {
        return readEnum(KEY_THEME, Theme.class, Theme.LIGHT);
    }

    public void setTheme(Theme theme) {
        prefs.put(KEY_THEME, (theme == null ? Theme.LIGHT : theme).name());
    }

    public Accent getAccent() {
        return readEnum(KEY_ACCENT, Accent.class, Accent.DEFAULT);
    }

    public void setAccent(Accent accent) {
        prefs.put(KEY_ACCENT, (accent == null ? Accent.DEFAULT : accent).name());
    }

    /** The color picked with "Custom…", as #RRGGBB. */
    public String getCustomAccent() {
        String hex = prefs.get(KEY_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT);
        return isHexColor(hex) ? hex.toUpperCase() : DEFAULT_CUSTOM_ACCENT;
    }

    public void setCustomAccent(String hex) {
        prefs.put(KEY_CUSTOM_ACCENT, isHexColor(hex) ? hex.toUpperCase() : DEFAULT_CUSTOM_ACCENT);
    }

    /** The accent actually used, or null to keep the theme's own accent. */
    public String getEffectiveAccentHex() {
        Accent accent = getAccent();
        return accent == Accent.CUSTOM ? getCustomAccent() : accent.getHex();
    }

    public Corners getCorners() {
        return readEnum(KEY_CORNERS, Corners.class, Corners.SUBTLE);
    }

    public void setCorners(Corners corners) {
        prefs.put(KEY_CORNERS, (corners == null ? Corners.SUBTLE : corners).name());
    }

    public Density getDensity() {
        return readEnum(KEY_DENSITY, Density.class, Density.COMFORTABLE);
    }

    public void setDensity(Density density) {
        prefs.put(KEY_DENSITY, (density == null ? Density.COMFORTABLE : density).name());
    }

    public boolean isZebraRows() {
        return prefs.getBoolean(KEY_ZEBRA, true);
    }

    public void setZebraRows(boolean zebraRows) {
        prefs.putBoolean(KEY_ZEBRA, zebraRows);
    }

    /** UI font family, or "" to keep the theme default (Segoe UI on Windows). */
    public String getFontFamily() {
        return prefs.get(KEY_FONT_FAMILY, "");
    }

    public void setFontFamily(String family) {
        prefs.put(KEY_FONT_FAMILY, family == null ? "" : family.trim());
    }

    /** Points added to (or removed from) the default font size. */
    public int getFontSizeDelta() {
        int delta = prefs.getInt(KEY_FONT_DELTA, 0);
        return Math.max(MIN_FONT_DELTA, Math.min(MAX_FONT_DELTA, delta));
    }

    public void setFontSizeDelta(int delta) {
        prefs.putInt(KEY_FONT_DELTA, Math.max(MIN_FONT_DELTA, Math.min(MAX_FONT_DELTA, delta)));
    }

    public TabStyle getTabStyle() {
        return readEnum(KEY_TAB_STYLE, TabStyle.class, TabStyle.UNDERLINED);
    }

    public void setTabStyle(TabStyle style) {
        prefs.put(KEY_TAB_STYLE, (style == null ? TabStyle.UNDERLINED : style).name());
    }

    public GridLines getGridLines() {
        return readEnum(KEY_GRID, GridLines.class, GridLines.HORIZONTAL);
    }

    public void setGridLines(GridLines grid) {
        prefs.put(KEY_GRID, (grid == null ? GridLines.HORIZONTAL : grid).name());
    }

    /** Re-color panels that hard-code white backgrounds when a dark theme is on. */
    public boolean isFixDarkColors() {
        return prefs.getBoolean(KEY_FIX_DARK, true);
    }

    public void setFixDarkColors(boolean fix) {
        prefs.putBoolean(KEY_FIX_DARK, fix);
    }

    /** True when the FlatLaf title bar (with the menu embedded) is used. */
    public boolean isModernTitleBar() {
        return nbFlatLafPrefs().getBoolean(NB_USE_WINDOW_DECORATIONS, true);
    }

    public void setModernTitleBar(boolean modern) {
        Preferences nb = nbFlatLafPrefs();
        nb.putBoolean(NB_USE_WINDOW_DECORATIONS, modern);
        nb.putBoolean(NB_UNIFIED_TITLE_BAR, modern);
        nb.putBoolean(NB_MENU_BAR_EMBEDDED, modern);
    }

    // ---- Studio settings (v2) ----

    private static final String KEY_PALETTE = "palettePreset";
    private static final String KEY_ICON_ONLY = "toolbarIconOnly";
    private static final String KEY_ICON_SIZE = "toolbarIconSize";
    private static final String KEY_WORKSPACES = "workspaces";
    private static final String KEY_NAV_MAX = "navigatorMaxResults";

    /** Community color palette, or NONE to use the plain theme. */
    public com.modernui.autopsy.theme.PalettePreset getPalette() {
        return readEnum(KEY_PALETTE, com.modernui.autopsy.theme.PalettePreset.class,
                com.modernui.autopsy.theme.PalettePreset.NONE);
    }

    public void setPalette(com.modernui.autopsy.theme.PalettePreset palette) {
        prefs.put(KEY_PALETTE, (palette == null ? com.modernui.autopsy.theme.PalettePreset.NONE : palette).name());
    }

    /** The theme actually used: a palette brings its own light or dark base. */
    public Theme getEffectiveTheme() {
        com.modernui.autopsy.theme.PalettePreset palette = getPalette();
        return palette == com.modernui.autopsy.theme.PalettePreset.NONE ? getTheme() : palette.getBase();
    }

    /** Toolbar buttons show only their icon (text moves to the tooltip). */
    public boolean isToolbarIconOnly() {
        return prefs.getBoolean(KEY_ICON_ONLY, false);
    }

    public void setToolbarIconOnly(boolean iconOnly) {
        prefs.putBoolean(KEY_ICON_ONLY, iconOnly);
    }

    /** 16 (small) or 24 (large); 0 leaves Autopsy's default. */
    public int getToolbarIconSize() {
        int size = prefs.getInt(KEY_ICON_SIZE, 0);
        return size == 16 || size == 24 ? size : 0;
    }

    public void setToolbarIconSize(int size) {
        prefs.putInt(KEY_ICON_SIZE, size == 16 || size == 24 ? size : 0);
    }

    private static final String KEY_MERGE_TREE = "mergeAutopsyTree";

    /** Show Autopsy's case tree inside the Navigator and hide its own column. */
    public boolean isMergeAutopsyTree() {
        return prefs.getBoolean(KEY_MERGE_TREE, true);
    }

    public void setMergeAutopsyTree(boolean merge) {
        prefs.putBoolean(KEY_MERGE_TREE, merge);
    }

    private static final String KEY_LOCK_TOOLBARS = "lockToolbars";

    /** Hide the toolbar drag handles (the ⋮ grips) so toolbars stay put. */
    public boolean isLockToolbars() {
        return prefs.getBoolean(KEY_LOCK_TOOLBARS, true);
    }

    public void setLockToolbars(boolean lock) {
        prefs.putBoolean(KEY_LOCK_TOOLBARS, lock);
    }

    /** Workspace names, one per line. */
    public String getWorkspaces() {
        return prefs.get(KEY_WORKSPACES, "");
    }

    public void setWorkspaces(String names) {
        prefs.put(KEY_WORKSPACES, names == null ? "" : names);
    }

    /** Most rows a navigator view loads into the result table. */
    public int getNavigatorMaxResults() {
        return Math.max(100, prefs.getInt(KEY_NAV_MAX, 10000));
    }

    public void setNavigatorMaxResults(int max) {
        prefs.putInt(KEY_NAV_MAX, Math.max(100, max));
    }

    /** Clears every Modern UI setting back to its default. */
    public void resetToDefaults() {
        try {
            prefs.clear();
        } catch (BackingStoreException ex) {
            // Fall back to writing the defaults one by one.
            setEnabled(true);
            setTheme(Theme.LIGHT);
            setAccent(Accent.DEFAULT);
            setCustomAccent(DEFAULT_CUSTOM_ACCENT);
            setCorners(Corners.SUBTLE);
            setDensity(Density.COMFORTABLE);
            setZebraRows(true);
            setFontFamily("");
            setFontSizeDelta(0);
            setTabStyle(TabStyle.UNDERLINED);
            setGridLines(GridLines.HORIZONTAL);
            setFixDarkColors(true);
        }
        setModernTitleBar(true);
    }

    public static boolean isHexColor(String value) {
        return value != null && HEX_COLOR.matcher(value).matches();
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Preferences nbFlatLafPrefs() {
        return NbPreferences.root().node(NB_FLATLAF_NODE);
    }

    private <E extends Enum<E>> E readEnum(String key, Class<E> type, E fallback) {
        String value = prefs.get(key, fallback.name());
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

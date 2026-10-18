package com.modernui.autopsy.sigil;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.ModernUISettings.SigilDetail;
import com.modernui.autopsy.ModernUISettings.SigilFont;
import com.modernui.autopsy.ModernUISettings.SigilPresentation;
import com.modernui.autopsy.theme.PalettePreset;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 * Cyber-Sigilism: the palette, typography and switches every sigil surface
 * reads from, plus installing the custom control delegates into Swing.
 *
 * Everything here is read live, so changing a setting and re-applying the
 * theme updates every surface without a restart.
 */
public final class Sigil {

    private static final Logger LOGGER = Logger.getLogger(Sigil.class.getName());

    // Fixed palette (from the design spec).
    public static final Color VOID = new Color(0x07070A);
    public static final Color BACKGROUND = new Color(0x0B0A10);
    public static final Color PANEL = new Color(0x111019);
    public static final Color ELEVATED = new Color(0x191522);
    public static final Color DEEP_VIOLET = new Color(0x54238F);
    public static final Color TEXT = new Color(0xF0EDF6);
    public static final Color TEXT_SECONDARY = new Color(0x9D96AA);
    public static final Color BORDER = new Color(0x352744);
    public static final Color ALERT = new Color(0xFF4567);
    public static final Color SUCCESS = new Color(0x71E6B2);

    private static final Map<SigilFont, Font> FONTS = new EnumMap<>(SigilFont.class);

    private Sigil() {
    }

    private static ModernUISettings settings() {
        return ModernUISettings.getInstance();
    }

    /** True when the Cyber-Sigilism palette is the active theme. */
    public static boolean isActive() {
        if (Boolean.getBoolean("modernui.disable")) {
            return false;
        }
        ModernUISettings s = settings();
        return s.isEnabled() && s.getPalette() == PalettePreset.GOTHIC_SIGILISM;
    }

    /** Active and showing the full presentation (ornaments, display type, glow). */
    public static boolean isFull() {
        return isActive() && settings().getSigilPresentation() == SigilPresentation.FULL;
    }

    public static SigilDetail detail() {
        return isFull() ? settings().getSigilDetail() : SigilDetail.LOW;
    }

    public static boolean atLeast(SigilDetail level) {
        return isFull() && detail().ordinal() >= level.ordinal();
    }

    /** Glow strength 0..1 (0 in restrained mode). */
    public static float glow() {
        return isFull() ? settings().getSigilGlow() / 100f : 0f;
    }

    public static boolean animate() {
        return isFull() && settings().isSigilAnimation();
    }

    public static Color primary() {
        return Color.decode(settings().getSigilPrimary());
    }

    /** Electric lavender, or a lightened primary when the primary was changed. */
    public static Color lavender() {
        if (ModernUISettings.DEFAULT_SIGIL_PRIMARY.equals(settings().getSigilPrimary())) {
            return new Color(0xC4A1FF);
        }
        return mix(primary(), Color.WHITE, 0.45f);
    }

    /** Cold cyan by default; used sparingly for technical values. */
    public static Color secondary() {
        return Color.decode(settings().getSigilSecondary());
    }

    public static Color alpha(Color c, float a) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), al);
    }

    public static Color mix(Color a, Color b, float t) {
        float u = 1 - t;
        return new Color(Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t));
    }

    static String hex(Color c) {
        return ModernUISettings.toHex(c);
    }

    /**
     * Display typeface at the given size, for headings only. Falls back to a
     * bold serif if the font can't be loaded, or the UI font when the user
     * chose "Interface font" or restrained mode.
     */
    public static Font display(float size) {
        SigilFont choice = settings().getSigilFont();
        if (!isFull() || choice == SigilFont.NONE) {
            Font base = UIManager.getFont("Label.font");
            return (base != null ? base : new Font(Font.SANS_SERIF, Font.PLAIN, 12)).deriveFont(Font.BOLD, size);
        }
        Font font = load(choice);
        return font != null ? font.deriveFont(Font.PLAIN, size) : new Font(Font.SERIF, Font.BOLD, Math.round(size));
    }

    /** Loads (once) and registers a bundled font. Package-visible for previews. */
    static synchronized Font load(SigilFont choice) {
        if (choice.getResource() == null) {
            return null;
        }
        if (FONTS.containsKey(choice)) {
            return FONTS.get(choice);
        }
        Font font = null;
        try (InputStream in = Sigil.class.getResourceAsStream("fonts/" + choice.getResource())) {
            if (in != null) {
                font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Could not load display font " + choice.getResource(), ex);
        }
        FONTS.put(choice, font);
        return font;
    }

    /**
     * FlatLaf properties for the Cyber-Sigilism look. Applied after the
     * palette and Options settings, so they win over zebra/corner choices
     * that would break the look, but before the user's own Theme Studio
     * overrides.
     */
    public static Map<String, String> extraDefaults(ModernUISettings s) {
        Map<String, String> m = new LinkedHashMap<>();
        String primary = s.getSigilPrimary();
        Color p = Color.decode(primary);
        String lavender = ModernUISettings.DEFAULT_SIGIL_PRIMARY.equals(primary) ? "#C4A1FF" : hex(mix(p, Color.WHITE, 0.45f));
        String deep = ModernUISettings.DEFAULT_SIGIL_PRIMARY.equals(primary) ? "#54238F" : hex(mix(p, VOID, 0.45f));
        String deepInactive = hex(mix(Color.decode(deep), VOID, 0.45f));
        String secondary = s.getSigilSecondary();

        m.put("@accentColor", primary);
        m.put("@selectionBackground", deep);
        m.put("@selectionInactiveBackground", deepInactive);
        m.put("@selectionForeground", "#FFFFFF");

        m.put("Component.arc", "0");
        m.put("Button.arc", "0");
        m.put("CheckBox.arc", "0");
        m.put("TextComponent.arc", "0");
        m.put("ProgressBar.arc", "0");
        m.put("Component.focusWidth", "1");
        m.put("Component.innerFocusWidth", "0");
        m.put("Component.focusColor", lavender);
        m.put("Component.focusedBorderColor", primary);
        m.put("Component.borderColor", "#352744");
        m.put("Component.disabledBorderColor", "#221A2C");
        m.put("Component.arrowType", "triangle");
        m.put("Component.linkColor", secondary);
        m.put("Component.error.borderColor", "#FF4567");
        m.put("Component.error.focusedBorderColor", "#FF4567");
        m.put("Actions.Red", "#FF4567");
        m.put("Actions.Green", "#71E6B2");

        m.put("Label.disabledForeground", "#8A8396");
        m.put("TitledBorder.titleColor", lavender);
        m.put("Separator.foreground", "#352744");

        m.put("Button.background", "#191522");
        m.put("Button.hoverBackground", "#241B33");
        m.put("Button.pressedBackground", deep);
        m.put("Button.selectedBackground", deep);
        m.put("Button.borderColor", "#352744");
        m.put("Button.hoverBorderColor", primary);
        m.put("Button.focusedBorderColor", lavender);
        m.put("Button.default.background", deep);
        m.put("Button.default.foreground", "#FFFFFF");
        m.put("Button.default.borderColor", primary);
        m.put("Button.default.hoverBackground", hex(mix(Color.decode(deep), p, 0.35f)));
        m.put("Button.default.focusedBorderColor", lavender);
        m.put("Button.toolbar.hoverBackground", "#241B33");
        m.put("Button.toolbar.pressedBackground", deep);
        m.put("Button.toolbar.selectedBackground", deepInactive);

        m.put("TextField.background", "#07070A");
        m.put("FormattedTextField.background", "#07070A");
        m.put("PasswordField.background", "#07070A");
        m.put("TextArea.background", "#0B0A10");
        m.put("TextPane.background", "#0B0A10");
        m.put("EditorPane.background", "#0B0A10");
        m.put("ComboBox.background", "#07070A");
        m.put("ComboBox.buttonBackground", "#111019");
        m.put("ComboBox.buttonArrowColor", lavender);
        m.put("ComboBox.buttonHoverArrowColor", "#FFFFFF");
        m.put("Spinner.background", "#07070A");
        m.put("Spinner.buttonArrowColor", lavender);

        m.put("Table.background", "#0B0A10");
        m.put("Table.alternateRowColor", s.isZebraRows() ? "#100E17" : "#0B0A10");
        m.put("Table.gridColor", "#1E1828");
        m.put("Table.selectionBackground", deep);
        m.put("Table.selectionInactiveBackground", deepInactive);
        m.put("Table.selectionForeground", "#FFFFFF");
        m.put("Table.selectionInactiveForeground", "#F0EDF6");
        m.put("Table.cellFocusColor", lavender);
        m.put("TableHeader.background", "#111019");
        m.put("TableHeader.foreground", lavender);
        m.put("TableHeader.separatorColor", "#2A2036");
        m.put("TableHeader.bottomSeparatorColor", primary);
        m.put("TableHeader.hoverBackground", "#191522");

        m.put("Tree.background", "#0B0A10");
        m.put("Tree.selectionBackground", deep);
        m.put("Tree.selectionInactiveBackground", deepInactive);
        m.put("Tree.selectionForeground", "#FFFFFF");
        m.put("Tree.paintLines", "true");
        m.put("Tree.hash", "#3B2C4C");
        m.put("List.background", "#0B0A10");
        m.put("List.selectionBackground", deep);
        m.put("List.selectionInactiveBackground", deepInactive);

        m.put("TabbedPane.underlineColor", primary);
        m.put("TabbedPane.inactiveUnderlineColor", deep);
        m.put("TabbedPane.tabSelectionHeight", "2");
        m.put("TabbedPane.cardTabSelectionHeight", "2");
        m.put("TabbedPane.hoverColor", "#191522");
        m.put("TabbedPane.focusColor", "#191522");
        m.put("TabbedPane.selectedBackground", "#140F1C");
        m.put("TabbedPane.selectedForeground", "#FFFFFF");
        m.put("TabbedPane.contentAreaColor", "#352744");
        m.put("TabbedPane.tabHeight", "30");

        m.put("ScrollBar.track", "#07070A");
        m.put("ScrollBar.thumb", "#352744");
        m.put("ScrollBar.hoverThumbColor", deep);
        m.put("ScrollBar.pressedThumbColor", primary);
        m.put("ScrollBar.thumbArc", "0");
        m.put("ScrollBar.thumbInsets", "2,3,2,3");
        m.put("ScrollBar.width", "11");
        m.put("ScrollBar.showButtons", "false");

        m.put("MenuBar.background", "#07070A");
        m.put("MenuBar.borderColor", "#352744");
        m.put("MenuBar.hoverBackground", "#191522");
        m.put("PopupMenu.background", "#0B0A10");
        m.put("PopupMenu.borderColor", deep);
        m.put("Menu.selectionBackground", deep);
        m.put("MenuItem.selectionBackground", deep);
        m.put("MenuItem.selectionForeground", "#FFFFFF");
        m.put("MenuItem.acceleratorForeground", "#9D96AA");
        m.put("MenuItem.acceleratorSelectionForeground", lavender);
        m.put("CheckBoxMenuItem.selectionBackground", deep);
        m.put("RadioButtonMenuItem.selectionBackground", deep);

        m.put("ToolBar.background", "#07070A");
        m.put("ToolBar.separatorColor", "#352744");
        m.put("TitlePane.background", "#07070A");
        m.put("TitlePane.inactiveBackground", "#07070A");
        m.put("TitlePane.foreground", lavender);
        m.put("TitlePane.inactiveForeground", "#9D96AA");
        m.put("TitlePane.embeddedForeground", lavender);
        m.put("RootPane.activeBorderColor", deep);
        m.put("RootPane.inactiveBorderColor", "#352744");

        m.put("ToolTip.background", "#191522");
        m.put("ToolTip.foreground", "#F0EDF6");
        m.put("ProgressBar.background", "#191522");
        m.put("ProgressBar.foreground", primary);
        m.put("ProgressBar.selectionForeground", "#FFFFFF");
        m.put("ProgressBar.selectionBackground", "#F0EDF6");
        m.put("ProgressBar.horizontalSize", "146,10");
        m.put("Slider.trackColor", "#352744");
        m.put("Slider.trackValueColor", primary);
        m.put("Slider.thumbColor", lavender);
        m.put("Slider.focusedColor", hexA(alpha(p, 0.4f)));
        m.put("SplitPaneDivider.draggingColor", primary);
        m.put("CheckBox.icon.checkmarkColor", "#FFFFFF");
        return m;
    }

    /** #RRGGBBAA, which FlatLaf properties accept for translucent colors. */
    private static String hexA(Color c) {
        return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }
}

package com.modernui.autopsy;

import com.formdev.flatlaf.FlatLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import org.openide.util.Lookup;

/**
 * Applies the saved theme.
 *
 * NetBeans reads its look and feel from --laf once, after every module's
 * validate() and before any window exists. We set that same value from
 * validate(), so NetBeans starts in FlatLaf and uses its own FlatLaf window
 * styling. If the hook is missing we switch after startup instead; that path
 * has to rebuild table headers that still carry Windows L&F renderers, since
 * those throw on paint and blank out the window.
 */
public final class ThemeApplier {

    /** Start Autopsy with -J-Dmodernui.disable=true to skip theming. */
    public static final String DISABLE_PROPERTY = "modernui.disable";

    private static final Logger LOGGER = Logger.getLogger(ThemeApplier.class.getName());

    private static final String CLI_OPTIONS_CLASS = "org.netbeans.core.startup.CLIOptions";

    private ThemeApplier() {
    }

    private static boolean isActive(ModernUISettings settings) {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            LOGGER.info("Modern UI disabled by -D" + DISABLE_PROPERTY);
            return false;
        }
        if (!settings.isEnabled()) {
            LOGGER.info("Modern UI is turned off in Tools > Options.");
            return false;
        }
        return true;
    }

    /**
     * Called from ModuleInstall.validate(), before NetBeans chooses its look
     * and feel. Returns true if NetBeans will start in the chosen FlatLaf theme.
     */
    static boolean requestStartupLookAndFeel() {
        ModernUISettings settings = ModernUISettings.getInstance();
        if (!isActive(settings)) {
            return false;
        }
        try {
            FlatLaf.setGlobalExtraDefaults(buildExtraDefaults(settings));
            Class<?> cliOptions = findClass(CLI_OPTIONS_CLASS);
            if (cliOptions == null) {
                LOGGER.info("Startup hook not found; will switch the theme after startup.");
                return false;
            }
            Field uiClass = cliOptions.getDeclaredField("uiClass");
            Field uiClassName = cliOptions.getDeclaredField("uiClassName");
            uiClass.setAccessible(true);
            uiClassName.setAccessible(true);
            if (uiClass.get(null) != null) {
                LOGGER.info("A look and feel was set with --laf; leaving it alone.");
                return false;
            }
            uiClassName.set(null, settings.getEffectiveTheme().lookAndFeelClassName());
            LOGGER.log(Level.INFO, "NetBeans will start with {0}", settings.getEffectiveTheme().lookAndFeelClassName());
            return true;
        } catch (Exception | LinkageError ex) {
            LOGGER.log(Level.INFO, "Startup hook failed; will switch the theme after startup.", ex);
            return false;
        }
    }

    private static Class<?> findClass(String name) {
        List<ClassLoader> loaders = new ArrayList<>();
        try {
            ClassLoader system = Lookup.getDefault().lookup(ClassLoader.class);
            if (system != null) {
                loaders.add(system);
            }
        } catch (RuntimeException ex) {
            // Lookup not ready; try the others.
        }
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ThemeApplier.class.getClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException | LinkageError ex) {
                // try the next loader
            }
        }
        return null;
    }

    /**
     * Called on the Swing thread after all modules have started. Confirms the
     * theme is active (switching now if the startup hook didn't work) and starts
     * the dark-mode color fixer.
     */
    static void finishStartup() {
        ModernUISettings settings = ModernUISettings.getInstance();
        if (!isActive(settings)) {
            return;
        }

        String expected = settings.getEffectiveTheme().lookAndFeelClassName();
        boolean atStartup = expected.equals(UIManager.getLookAndFeel().getClass().getName());

        if (!atStartup) {
            try {
                FlatLaf.setGlobalExtraDefaults(buildExtraDefaults(settings));
                UIManager.setLookAndFeel(settings.getEffectiveTheme().create());
                restoreAutopsyCustomizations();
                refreshExistingWindows();
            } catch (Exception | LinkageError ex) {
                LOGGER.log(Level.WARNING, "Could not apply the Modern UI theme; Autopsy keeps its default look.", ex);
                return;
            }
        }

        LOGGER.log(Level.INFO, "Modern UI applied ({7}): theme={0}, accent={1}, corners={2}, density={3}, "
                + "font={4}, tabs={5}, grid={6}",
                new Object[]{settings.getEffectiveTheme(), settings.getEffectiveAccentHex(),
                    settings.getCorners(), settings.getDensity(), fontSpec(settings),
                    settings.getTabStyle(), settings.getGridLines(),
                    atStartup ? "at startup" : "after startup"});

        if (settings.getEffectiveTheme().isDark() && settings.isFixDarkColors()) {
            try {
                DarkColorFixer.install();
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Dark color fixer could not start.", ex);
            }
        }
    }

    /**
     * Builds FlatLaf property overrides. Values use the same syntax as
     * FlatLaf .properties files, so variables and color functions work.
     */
    static Map<String, String> buildExtraDefaults(ModernUISettings settings) {
        return buildExtraDefaults(settings, com.modernui.autopsy.theme.ThemeOverrides.load());
    }

    /**
     * Order matters: palette, then Options settings, then the user's own
     * Theme Studio overrides, so each layer can override the one before it.
     */
    public static Map<String, String> buildExtraDefaults(ModernUISettings settings, Map<String, String> overrides) {
        Map<String, String> extra = new LinkedHashMap<>(settings.getPalette().getValues());

        String accent = settings.getEffectiveAccentHex();
        if (accent != null) {
            extra.put("@accentColor", accent);
        }

        String font = fontSpec(settings);
        if (font != null) {
            extra.put("defaultFont", font);
        }

        switch (settings.getCorners()) {
            case SQUARE:
                extra.put("Button.arc", "0");
                extra.put("Component.arc", "0");
                extra.put("CheckBox.arc", "0");
                extra.put("ProgressBar.arc", "0");
                extra.put("TextComponent.arc", "0");
                break;
            case ROUNDED:
                extra.put("Button.arc", "12");
                extra.put("Component.arc", "10");
                extra.put("CheckBox.arc", "6");
                extra.put("ProgressBar.arc", "8");
                extra.put("TextComponent.arc", "10");
                putPillScrollBars(extra);
                break;
            case SUBTLE:
            default:
                putPillScrollBars(extra);
                break;
        }

        String rowHeight = Integer.toString(settings.getDensity().getRowHeight());
        extra.put("Table.rowHeight", rowHeight);
        extra.put("Tree.rowHeight", rowHeight);

        if (settings.isZebraRows()) {
            extra.put("Table.alternateRowColor", settings.getEffectiveTheme().isDark()
                    ? "lighten($Table.background,4%)"
                    : "darken($Table.background,3%)");
        }

        switch (settings.getGridLines()) {
            case NONE:
                extra.put("Table.showHorizontalLines", "false");
                extra.put("Table.showVerticalLines", "false");
                extra.put("Table.intercellSpacing", "0,0");
                break;
            case BOTH:
                extra.put("Table.showHorizontalLines", "true");
                extra.put("Table.showVerticalLines", "true");
                extra.put("Table.intercellSpacing", "1,1");
                break;
            case HORIZONTAL:
            default:
                extra.put("Table.showHorizontalLines", "true");
                extra.put("Table.showVerticalLines", "false");
                extra.put("Table.intercellSpacing", "0,1");
                break;
        }

        if (settings.getTabStyle() == ModernUISettings.TabStyle.CARD) {
            extra.put("TabbedPane.tabType", "card");
            extra.put("TabbedPane.showTabSeparators", "true");
        } else {
            extra.put("TabbedPane.tabType", "underlined");
        }

        extra.put("ScrollPane.smoothScrolling", "true");
        if (settings.getPalette() == com.modernui.autopsy.theme.PalettePreset.GOTHIC_SIGILISM) {
            extra.putAll(com.modernui.autopsy.sigil.Sigil.extraDefaults(settings));
        }
        if (overrides != null) {
            extra.putAll(overrides);
        }
        return extra;
    }

    /**
     * FlatLaf font syntax: an optional size change ("+2", "-1") followed by an
     * optional quoted family. FlatLaf falls back to the default family if the
     * font is not installed. Returns null when nothing changes.
     */
    static String fontSpec(ModernUISettings settings) {
        int delta = settings.getFontSizeDelta();
        String family = settings.getFontFamily().replace("\"", "");
        StringBuilder spec = new StringBuilder();
        if (delta != 0) {
            spec.append(delta > 0 ? "+" : "").append(delta);
        }
        if (!family.isEmpty()) {
            if (spec.length() > 0) {
                spec.append(' ');
            }
            spec.append('"').append(family).append('"');
        }
        return spec.length() == 0 ? null : spec.toString();
    }

    private static void putPillScrollBars(Map<String, String> extra) {
        extra.put("ScrollBar.thumbArc", "999");
        extra.put("ScrollBar.thumbInsets", "2,2,2,2");
        extra.put("ScrollBar.width", "12");
    }

    /**
     * Applies the current settings right away, without restarting. Switching
     * themes on a running window is less reliable than starting in the theme,
     * so the Studio still offers "Save and restart" as the dependable option.
     * Must run on the Swing event thread.
     */
    public static void applyLive() {
        ModernUISettings settings = ModernUISettings.getInstance();
        try {
            com.modernui.autopsy.sigil.SigilControls.listen();
            FlatLaf.setGlobalExtraDefaults(buildExtraDefaults(settings));
            UIManager.setLookAndFeel(settings.getEffectiveTheme().create());
            restoreAutopsyCustomizations();
            refreshExistingWindows();
            if (settings.getEffectiveTheme().isDark() && settings.isFixDarkColors()) {
                DarkColorFixer.install();
            }
            com.modernui.autopsy.ui.ToolbarCustomizer.apply();
            com.modernui.autopsy.sigil.SigilClock.update();
            LOGGER.info("Settings applied live.");
        } catch (Exception | LinkageError ex) {
            LOGGER.log(Level.WARNING, "Live apply failed; restart Autopsy to apply.", ex);
            throw new IllegalStateException("Could not apply the theme live: " + ex.getMessage(), ex);
        }
    }

    /**
     * Autopsy hides the tabs on its main view panels. Changing the look and
     * feel makes NetBeans reinstall its default tab UI, so put Autopsy's
     * settings back (same values as Autopsy's corecomponents Installer).
     */
    private static void restoreAutopsyCustomizations() {
        UIManager.put("ViewTabDisplayerUI", "org.sleuthkit.autopsy.corecomponents.NoTabsTabDisplayerUI");
        UIManager.put("TabbedContainer.view.contentBorder", BorderFactory.createEmptyBorder());
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
    }

    /** Re-skins any windows that were created before the switch. */
    private static void refreshExistingWindows() {
        int rebuilt = 0;
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            rebuilt += repairTableHeaders(window);
        }
        if (rebuilt > 0) {
            LOGGER.log(Level.INFO, "Rebuilt {0} table header(s) left over from the old theme.", rebuilt);
        }
    }

    /**
     * Rebuilds table headers whose renderer no longer paints (a renderer from
     * the previous look and feel). Returns how many were rebuilt.
     */
    static int repairTableHeaders(Component root) {
        int rebuilt = 0;
        if (root instanceof JTable) {
            JTable table = (JTable) root;
            JTableHeader header = table.getTableHeader();
            if (header != null && !headerPaints(table, header)) {
                rebuildHeader(table, header);
                rebuilt++;
            }
        }
        if (root instanceof Container) {
            for (Component child : ((Container) root).getComponents()) {
                rebuilt += repairTableHeaders(child);
            }
        }
        return rebuilt;
    }

    private static boolean headerPaints(JTable table, JTableHeader header) {
        TableCellRenderer renderer = header.getDefaultRenderer();
        if (renderer == null || table.getColumnCount() == 0) {
            return true;
        }
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            Component cell = renderer.getTableCellRendererComponent(table, "", false, false, -1, 0);
            if (cell != null) {
                cell.setSize(8, 8);
                cell.paint(g);
            }
            return true;
        } catch (RuntimeException ex) {
            return false;
        } finally {
            g.dispose();
        }
    }

    private static void rebuildHeader(JTable table, JTableHeader old) {
        JTableHeader fresh = createHeader(table);
        fresh.setReorderingAllowed(old.getReorderingAllowed());
        fresh.setResizingAllowed(old.getResizingAllowed());
        fresh.setToolTipText(old.getToolTipText());
        table.setTableHeader(fresh);
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (scroll != null && scroll.getViewport() != null && scroll.getViewport().getView() == table) {
            scroll.setColumnHeaderView(fresh);
        }
        fresh.revalidate();
        fresh.repaint();
    }

    /** JTable.createDefaultTableHeader() is protected; subclasses such as ETable override it. */
    private static JTableHeader createHeader(JTable table) {
        try {
            java.lang.reflect.Method m = JTable.class.getDeclaredMethod("createDefaultTableHeader");
            m.setAccessible(true);
            Object header = m.invoke(table);
            if (header instanceof JTableHeader) {
                return (JTableHeader) header;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.log(Level.FINE, "Using a plain table header", ex);
        }
        return new JTableHeader(table.getColumnModel());
    }
}

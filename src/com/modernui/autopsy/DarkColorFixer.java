package com.modernui.autopsy;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

/**
 * Experimental: fixes Autopsy panels that hard-code light colors.
 *
 * Many Autopsy panels were built with the NetBeans GUI builder and set
 * explicit white backgrounds and black text. Under a dark theme those show up
 * as bright boxes. This watches components as they are added and swaps
 * near-white backgrounds for the theme's background, and near-black text on a
 * dark background for the theme's text color.
 *
 * It leaves alone: colors that came from the theme (UIResource), tinted colors
 * (status colors, highlights, red warnings), table/tree cell renderers, and
 * HTML viewers (JEditorPane), whose text colors come from the document.
 */
final class DarkColorFixer implements AWTEventListener {

    private static final Logger LOGGER = Logger.getLogger(DarkColorFixer.class.getName());

    private static final String WATCHED = "modernui.darkColorFixer.watched";

    /** Gray backgrounds brighter than this are treated as "hard-coded white" (about #E5E5E5 and up). */
    private static final double LIGHT_BACKGROUND = 0.78;

    /** Text darker than this is treated as "hard-coded black". */
    private static final double DARK_TEXT = 0.25;

    /** An effective background below this counts as dark. */
    private static final double DARK_BACKGROUND = 0.5;

    /** Max spread between RGB channels for a color to count as gray (not a highlight color). */
    private static final int MAX_GRAY_SPREAD = 24;

    private final PropertyChangeListener colorListener = evt -> {
        Object source = evt.getSource();
        if (source instanceof JComponent) {
            recolor((JComponent) source);
        }
    };

    private DarkColorFixer() {
    }

    private static DarkColorFixer installed;

    /**
     * Starts watching all current and future windows (once), and re-checks the
     * windows that already exist. Call on the event thread.
     */
    static void install() {
        if (installed == null) {
            installed = new DarkColorFixer();
            Toolkit.getDefaultToolkit().addAWTEventListener(installed, AWTEvent.CONTAINER_EVENT_MASK);
            LOGGER.info("Modern UI: dark color fixer installed.");
        }
        for (Window window : Window.getWindows()) {
            installed.fixTree(window);
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event.getID() == ContainerEvent.COMPONENT_ADDED && event instanceof ContainerEvent) {
            try {
                fixTree(((ContainerEvent) event).getChild());
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "Dark color fixer skipped a component", ex);
            }
        }
    }

    /** Walks top-down so parents are re-colored before their children are judged. */
    void fixTree(Component component) {
        if (component == null || isRenderer(component)) {
            return;
        }
        if (component instanceof JComponent) {
            JComponent jc = (JComponent) component;
            recolor(jc);
            watch(jc);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                fixTree(child);
            }
        }
    }

    private void watch(JComponent component) {
        if (component.getClientProperty(WATCHED) == null) {
            component.putClientProperty(WATCHED, Boolean.TRUE);
            component.addPropertyChangeListener("background", colorListener);
            component.addPropertyChangeListener("foreground", colorListener);
        }
    }

    static void recolor(JComponent component) {
        if (component instanceof JEditorPane || isRenderer(component) || !isDarkTheme()) {
            return;
        }

        Color background = component.getBackground();
        if (isHardCodedGray(background) && luminance(background) > LIGHT_BACKGROUND) {
            component.setBackground(new Color(themeBackground(component).getRGB()));
        }

        Color foreground = component.getForeground();
        if (isHardCodedGray(foreground) && luminance(foreground) < DARK_TEXT
                && luminance(effectiveBackground(component)) < DARK_BACKGROUND) {
            component.setForeground(new Color(themeForeground(component).getRGB()));
        }
    }

    /** Only act while a dark theme is active (the theme can change live from the Studio). */
    private static boolean isDarkTheme() {
        Color panel = UIManager.getColor("Panel.background");
        return panel != null && luminance(panel) < DARK_BACKGROUND;
    }

    private static boolean isRenderer(Component component) {
        return component instanceof CellRendererPane
                || component.getParent() instanceof CellRendererPane;
    }

    /**
     * True for a color the code set explicitly (not from the theme) that is a
     * neutral gray. Tinted colors such as yellow highlights or red warning text
     * carry meaning, so they are never touched.
     */
    private static boolean isHardCodedGray(Color color) {
        if (color == null || color instanceof UIResource || color.getAlpha() != 255) {
            return false;
        }
        int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return max - min <= MAX_GRAY_SPREAD;
    }

    /** Background actually visible behind a component (first opaque ancestor). */
    static Color effectiveBackground(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            if (c.isOpaque() && c.getBackground() != null) {
                return c.getBackground();
            }
        }
        Color panel = UIManager.getColor("Panel.background");
        return panel != null ? panel : Color.DARK_GRAY;
    }

    private static Color themeBackground(JComponent component) {
        return firstColor(component instanceof JTable ? "Table.background"
                : component instanceof JTree ? "Tree.background"
                : component instanceof JList ? "List.background"
                : component instanceof JTextComponent ? "TextArea.background"
                : "Panel.background");
    }

    private static Color themeForeground(JComponent component) {
        return firstColor(component instanceof JTable ? "Table.foreground"
                : component instanceof JTree ? "Tree.foreground"
                : component instanceof JList ? "List.foreground"
                : component instanceof JTextComponent ? "TextArea.foreground"
                : "Label.foreground");
    }

    private static Color firstColor(String key) {
        Color color = UIManager.getColor(key);
        if (color == null) {
            color = UIManager.getColor("Panel.background");
        }
        return color != null ? color : Color.DARK_GRAY;
    }

    /** Relative luminance, 0 (black) to 1 (white). */
    static double luminance(Color color) {
        return (0.2126 * channel(color.getRed())
                + 0.7152 * channel(color.getGreen())
                + 0.0722 * channel(color.getBlue()));
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}

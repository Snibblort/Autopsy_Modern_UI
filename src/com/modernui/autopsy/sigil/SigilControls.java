package com.modernui.autopsy.sigil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Custom-painted Swing controls for Cyber-Sigilism.
 *
 * The delegates subclass FlatLaf's own, so behavior, keyboard handling,
 * sizing and accessibility stay FlatLaf's; only painting changes. They are
 * put in UIManager's developer defaults (which outrank the look and feel)
 * every time the look and feel changes, and removed again when another
 * palette is chosen.
 */
public final class SigilControls {

    private static final Logger LOGGER = Logger.getLogger(SigilControls.class.getName());

    private static final String[] DELEGATE_IDS = {"ButtonUI", "TabbedPaneUI", "ProgressBarUI", "SliderUI", "TableHeaderUI"};
    private static final String[] DELEGATE_CLASSES = {"SigilButtonUI", "SigilTabbedPaneUI", "SigilProgressBarUI",
        "SigilSliderUI", "SigilTableHeaderUI"};
    private static final String FLAT_PACKAGE = "com.modernui.autopsy.sigil.flat.";
    private static final String[] VALUE_KEYS = {
        "Button.border", "CheckBox.icon", "RadioButton.icon", "Tree.expandedIcon", "Tree.collapsedIcon",
        "Table.ascendingSortIcon", "Table.descendingSortIcon", "ETableHeader.ascendingIcon",
        "ETableHeader.descendingIcon", "PopupMenu.border", "ToolTip.border", "TitledBorder.border"};

    private static boolean listening;
    private static boolean installed;

    private SigilControls() {
    }

    /**
     * Starts following look-and-feel changes. Safe to call before Swing is
     * initialized: adding the listener doesn't initialize UIManager.
     */
    public static synchronized void listen() {
        if (listening) {
            return;
        }
        listening = true;
        PropertyChangeListener l = evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                sync();
            }
        };
        UIManager.addPropertyChangeListener(l);
    }

    /** Installs or removes the delegates to match the current settings. */
    public static synchronized void sync() {
        try {
            if (Sigil.isActive()) {
                install();
            } else if (installed) {
                uninstall();
            }
        } catch (RuntimeException | LinkageError ex) {
            LOGGER.log(Level.WARNING, "Could not install the Cyber-Sigilism controls", ex);
        }
    }

    private static void install() {
        // The painted delegates extend FlatLaf classes NetBeans doesn't export,
        // so they come from FlatBridge. If that ever fails, everything else
        // (colors, icons, borders) still applies.
        try {
            for (int i = 0; i < DELEGATE_IDS.length; i++) {
                Class<?> cls = FlatBridge.load(FLAT_PACKAGE + DELEGATE_CLASSES[i]);
                UIManager.put(DELEGATE_IDS[i], cls.getName());
                // A Class value under the class name lets Swing skip class loading.
                UIManager.put(cls.getName(), cls);
            }
            UIManager.put("Button.border", FlatBridge.load(FLAT_PACKAGE + "SigilButtonBorder")
                    .getConstructor().newInstance());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            LOGGER.log(Level.WARNING, "Cyber-Sigilism control painting unavailable; using FlatLaf's", ex);
            removeDelegates();
        }
        UIManager.put("CheckBox.icon", new CheckIcon(false));
        UIManager.put("RadioButton.icon", new CheckIcon(true));
        UIManager.put("Tree.expandedIcon", new ChevronIcon(true));
        UIManager.put("Tree.collapsedIcon", new ChevronIcon(false));
        UIManager.put("Table.ascendingSortIcon", new SortIcon(true));
        UIManager.put("Table.descendingSortIcon", new SortIcon(false));
        UIManager.put("ETableHeader.ascendingIcon", new SortIcon(true));
        UIManager.put("ETableHeader.descendingIcon", new SortIcon(false));
        UIManager.put("PopupMenu.border", new PopupBorder());
        UIManager.put("ToolTip.border", new TipBorder());
        UIManager.put("TitledBorder.border", new EngravedBorder());
        installed = true;
        LOGGER.fine("Cyber-Sigilism controls installed");
    }

    private static void removeDelegates() {
        for (int i = 0; i < DELEGATE_IDS.length; i++) {
            UIManager.put(DELEGATE_IDS[i], null);
            UIManager.put(FLAT_PACKAGE + DELEGATE_CLASSES[i], null);
        }
        UIManager.put("Button.border", null);
    }

    private static void uninstall() {
        removeDelegates();
        for (String key : VALUE_KEYS) {
            UIManager.put(key, null);
        }
        installed = false;
    }

    public static int focusWidth() {
        return Math.max(0, UIManager.getInt("Component.focusWidth"));
    }

    public static boolean plainButton(Component c) {
        if (!(c instanceof AbstractButton) || c instanceof BasicArrowButton) {
            return true;
        }
        AbstractButton b = (AbstractButton) c;
        if (!b.isContentAreaFilled() || !b.isBorderPainted()) {
            return true;
        }
        Object type = b.getClientProperty("JButton.buttonType");
        if (type != null && !"".equals(type)) {
            return true;
        }
        return javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JToolBar.class, b) != null;
    }

    public static int cut(int h) {
        return Math.max(2, Math.min(6, h / 5));
    }

    // ---- buttons -------------------------------------------------------

    // ---- tabs ----------------------------------------------------------

    // ---- progress ------------------------------------------------------

    // ---- slider --------------------------------------------------------

    // ---- table header --------------------------------------------------

    // ---- icons ---------------------------------------------------------

    /** Check box: clipped square with a lit diamond seal. Radio: diamond in a diamond. */
    public static class CheckIcon implements Icon, UIResource {

        private final boolean radio;

        public CheckIcon(boolean radio) {
            this.radio = radio;
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                ButtonModel m = c instanceof AbstractButton ? ((AbstractButton) c).getModel() : null;
                boolean selected = m != null && m.isSelected();
                boolean enabled = c.isEnabled();
                boolean hover = m != null && (m.isRollover() || m.isArmed());
                boolean focus = c instanceof JComponent && ((JComponent) c).hasFocus();
                Object state = c instanceof JComponent ? ((JComponent) c).getClientProperty("JButton.selectedState") : null;
                boolean indeterminate = "indeterminate".equals(state);
                double cx = x + 8;
                double cy = y + 8;
                Shape box = radio ? SigilArt.diamond(cx, cy, 7, 7) : SigilArt.clipped(x + 1.5, y + 1.5, 13, 13, 3);
                g2.setColor(enabled ? Sigil.VOID : Sigil.BACKGROUND);
                g2.fill(box);
                Color edge = !enabled ? Sigil.ELEVATED : focus ? Sigil.lavender()
                        : (hover || selected) ? Sigil.primary() : Sigil.BORDER;
                if (focus && enabled) {
                    SigilArt.glowStroke(g2, box, edge, 1f, Math.max(0.4f, Sigil.glow()));
                } else {
                    g2.setColor(edge);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(box);
                }
                if (indeterminate) {
                    g2.setColor(enabled ? Sigil.lavender() : Sigil.BORDER);
                    g2.fill(SigilArt.thorn(cx - 4.5, cy, cx + 4.5, cy, 2.5));
                    g2.fill(SigilArt.thorn(cx + 4.5, cy, cx - 4.5, cy, 2.5));
                } else if (selected) {
                    Color seal = enabled ? Sigil.lavender() : Sigil.BORDER;
                    SigilArt.glowFill(g2, SigilArt.diamond(cx, cy, radio ? 3.4 : 4, radio ? 3.4 : 4.6), seal,
                            enabled ? Sigil.glow() : 0);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Tree expand/collapse: a thin chevron with a lit point. */
    public static class ChevronIcon implements Icon, UIResource {

        private final boolean expanded;

        public ChevronIcon(boolean expanded) {
            this.expanded = expanded;
        }

        @Override
        public int getIconWidth() {
            return 11;
        }

        @Override
        public int getIconHeight() {
            return 11;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.translate(x, y);
                Path2D p = new Path2D.Double();
                double tipX;
                double tipY;
                if (expanded) {
                    p.moveTo(1.5, 3);
                    p.lineTo(5.5, 7.5);
                    p.lineTo(9.5, 3);
                    tipX = 5.5;
                    tipY = 7.5;
                } else {
                    p.moveTo(3, 1.5);
                    p.lineTo(7.5, 5.5);
                    p.lineTo(3, 9.5);
                    tipX = 7.5;
                    tipY = 5.5;
                }
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g2.setColor(c != null && !c.isEnabled() ? Sigil.BORDER : Sigil.TEXT_SECONDARY);
                g2.draw(p);
                g2.setColor(expanded ? Sigil.primary() : Sigil.lavender());
                g2.fill(SigilArt.diamond(tipX, tipY, 1.8, 1.8));
            } finally {
                g2.dispose();
            }
        }
    }

    /** Column sort direction: a thorn pointing up or down. */
    public static class SortIcon implements Icon, UIResource {

        private final boolean ascending;

        public SortIcon(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int getIconWidth() {
            return 9;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.setColor(Sigil.lavender());
                double cx = x + 4.5;
                if (ascending) {
                    g2.fill(SigilArt.thorn(cx, y + 9, cx, y + 0.5, 7));
                } else {
                    g2.fill(SigilArt.thorn(cx, y + 0.5, cx, y + 9.5, 7));
                }
                g2.setColor(Sigil.VOID);
                g2.fill(SigilArt.diamond(cx, ascending ? y + 6.5 : y + 3.5, 1.2, 1.6));
            } finally {
                g2.dispose();
            }
        }
    }

    // ---- borders -------------------------------------------------------

    /** Popup menus: violet frame, primary top edge, lit corner points. */
    public static class PopupBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(5, 1, 5, 1);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Sigil.DEEP_VIOLET);
                g2.drawRect(x, y, w - 1, h - 1);
                g2.setColor(Sigil.primary());
                g2.fillRect(x, y, w, 1);
                g2.setColor(Sigil.alpha(Sigil.primary(), 0.25f));
                g2.fillRect(x + 1, y + 1, w - 2, 1);
                SigilArt.smooth(g2);
                g2.setColor(Sigil.lavender());
                g2.fill(SigilArt.diamond(x + 1, y + 0.5, 2.5, 2.5));
                g2.fill(SigilArt.diamond(x + w - 1.5, y + 0.5, 2.5, 2.5));
            } finally {
                g2.dispose();
            }
        }
    }

    /** Tooltips: clipped frame with a small lit mark on the left. */
    public static class TipBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(4, 10, 4, 8);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.setColor(Sigil.BORDER);
                g2.draw(SigilArt.clipped(x + 0.5, y + 0.5, w - 1, h - 1, 4));
                g2.setColor(Sigil.primary());
                g2.fill(SigilArt.diamond(x + 5, y + h / 2.0, 2, 3));
            } finally {
                g2.dispose();
            }
        }
    }

    /** Group boxes (titled borders): hairline frame with engraved corner ticks. */
    public static class EngravedBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(2, 2, 2, 2);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Sigil.BORDER);
                g2.drawRect(x, y, w - 1, h - 1);
                g2.setColor(Sigil.primary());
                int t = Math.min(8, Math.min(w, h) / 4);
                g2.fillRect(x, y, t, 1);
                g2.fillRect(x, y, 1, t);
                g2.fillRect(x + w - t, y + h - 1, t, 1);
                g2.fillRect(x + w - 1, y + h - t, 1, t);
            } finally {
                g2.dispose();
            }
        }
    }
}

package com.modernui.autopsy.studio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.openide.LifecycleManager;

/**
 * Small layout and widget helpers shared by the Studio tabs, so every tab
 * looks the same.
 */
public final class StudioUi {

    private StudioUi() {
    }

    /** A two-column label/field form. */
    public static JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.putClientProperty("studio.row", 0);
        return panel;
    }

    private static int nextRow(JPanel form) {
        Object row = form.getClientProperty("studio.row");
        int r = row instanceof Integer ? (Integer) row : 0;
        form.putClientProperty("studio.row", r + 1);
        return r;
    }

    public static void row(JPanel form, String label, Component field) {
        int r = nextRow(form);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = r;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, 0, 3, 12);
        gc.gridx = 0;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(field, gc);
    }

    public static void wide(JPanel form, Component field) {
        int r = nextRow(form);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = r;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(3, 0, 3, 12);
        form.add(field, gc);
    }

    public static void hint(JPanel form, String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.ITALIC));
        label.setForeground(muted());
        wide(form, label);
    }

    /** Bold heading with a rule, like the Options page sections. */
    public static JPanel section(String name) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel(name);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        header.add(label, BorderLayout.WEST);
        JPanel lineHolder = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.HORIZONTAL;
        lc.weightx = 1;
        lineHolder.add(new JSeparator(), lc);
        header.add(lineHolder, BorderLayout.CENTER);
        return header;
    }

    public static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    public static Color muted() {
        Color c = javax.swing.UIManager.getColor("Label.disabledForeground");
        return c != null ? c : Color.GRAY;
    }

    public static Icon swatch(Color color) {
        return new Swatch(color, null);
    }

    public static Icon twoToneSwatch(Color background, Color accent) {
        return new Swatch(background, accent);
    }

    /** Saves nothing itself: marks Autopsy for restart and exits (Autopsy asks to close the case first). */
    public static void restartAutopsy(Component parent) {
        int answer = JOptionPane.showConfirmDialog(parent,
                "Restart Autopsy now to apply the changes?", "Modern UI Studio", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LifecycleManager.getDefault().markForRestart();
        } catch (UnsupportedOperationException ex) {
            JOptionPane.showMessageDialog(parent, "Saved. Close and reopen Autopsy to apply the changes.",
                    "Modern UI Studio", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> LifecycleManager.getDefault().exit());
    }

    private static final class Swatch implements Icon {

        private final Color main;
        private final Color dot;

        Swatch(Color main, Color dot) {
            this.main = main;
            this.dot = dot;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(main);
                g2.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 5, 5);
                g2.setColor(new Color(128, 128, 128, 110));
                g2.drawRoundRect(x, y, getIconWidth() - 1, getIconHeight() - 1, 5, 5);
                if (dot != null) {
                    g2.setColor(dot);
                    g2.fillOval(x + getIconWidth() - 9, y + 3, 6, 6);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return dot == null ? 14 : 22;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }
}

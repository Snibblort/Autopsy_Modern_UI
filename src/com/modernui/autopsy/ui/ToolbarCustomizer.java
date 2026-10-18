package com.modernui.autopsy.ui;

import com.modernui.autopsy.ModernUISettings;
import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.openide.awt.Toolbar;
import org.openide.awt.ToolbarPool;
import org.openide.util.RequestProcessor;

/**
 * Applies toolbar-wide display settings: icon size and icon-only buttons.
 * Toolbars are rebuilt whenever their contents change, so a listener re-applies
 * the settings to new buttons.
 */
public final class ToolbarCustomizer {

    private static final Logger LOGGER = Logger.getLogger(ToolbarCustomizer.class.getName());
    private static final String ORIGINAL_TEXT = "modernui.originalText";
    private static final String ORIGINAL_ICON = "modernui.originalIcon";
    private static final String ORIGINAL_ICON_SAVED = "modernui.originalIconSaved";
    private static final String MODERN_ICON = "modernui.modernIcon";
    private static final String HOVER_EFFECT = "modernui.toolbarHoverEffect";
    private static final String RESTING_COLOR = "modernui.toolbarRestingColor";
    private static final String RESTING_OPAQUE = "modernui.toolbarRestingOpaque";
    private static final String WATCHED = "modernui.toolbarWatched";
    private static final String DRAGGER = "_toolbar_dragger_";
    private static final RequestProcessor RP = new RequestProcessor("Modern UI toolbars", 1);

    private static final ContainerAdapter REAPPLY = new ContainerAdapter() {
        @Override
        public void componentAdded(ContainerEvent e) {
            SwingUtilities.invokeLater(ToolbarCustomizer::apply);
        }
    };

    private ToolbarCustomizer() {
    }

    /** Waits (off the Swing thread) for the toolbars to load, then applies. */
    public static void applyWhenReady() {
        RP.post(() -> {
            try {
                ToolbarPool.getDefault().waitFinished();
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "Toolbar pool not ready", ex);
            }
            SwingUtilities.invokeLater(ToolbarCustomizer::apply);
        });
    }

    /** Applies the saved toolbar settings now. Call on the Swing thread. */
    public static void apply() {
        try {
            ModernUISettings s = ModernUISettings.getInstance();
            ToolbarPool pool = ToolbarPool.getDefault();
            int size = s.getToolbarIconSize();
            if (size != 0 && pool.getPreferredIconSize() != size) {
                pool.setPreferredIconSize(size);
            }
            watch(pool);
            for (Toolbar toolbar : pool.getToolbars()) {
                watch(toolbar);
                styleToolbar(toolbar);
                applyTo(toolbar, s.isToolbarIconOnly());
                setDragHandleVisible(toolbar, !s.isLockToolbars());
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not apply toolbar settings", ex);
        }
    }

    private static void styleToolbar(Toolbar toolbar) {
        Color background = UIManager.getColor("ToolBar.background");
        Color separator = UIManager.getColor("Separator.foreground");
        if (background != null) {
            toolbar.setBackground(background);
            toolbar.setOpaque(true);
        }
        if (separator != null) {
            toolbar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, separator),
                    BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        }
    }

    private static void watch(JComponent c) {
        if (c.getClientProperty(WATCHED) == null) {
            c.putClientProperty(WATCHED, Boolean.TRUE);
            c.addContainerListener(REAPPLY);
        }
    }

    /** NetBeans tags each toolbar's drag grip with the client property "_toolbar_dragger_". */
    static void setDragHandleVisible(Container toolbar, boolean visible) {
        for (Component child : toolbar.getComponents()) {
            if (child instanceof JComponent
                    && Boolean.TRUE.equals(((JComponent) child).getClientProperty(DRAGGER))) {
                if (child.isVisible() != visible) {
                    child.setVisible(visible);
                    toolbar.revalidate();
                    toolbar.repaint();
                }
            }
        }
    }

    /** Hides or restores button text under {@code root}. Text moves into the tooltip. */
    static void applyTo(Container root, boolean iconOnly) {
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) child;
                styleButton(b);
                Object original = b.getClientProperty(ORIGINAL_TEXT);
                if (iconOnly) {
                    String text = b.getText();
                    if (b.getIcon() != null && text != null && !text.trim().isEmpty()) {
                        b.putClientProperty(ORIGINAL_TEXT, text);
                        if (b.getToolTipText() == null || b.getToolTipText().trim().isEmpty()) {
                            b.setToolTipText(text);
                        }
                        b.setText("");
                    }
                } else if (original instanceof String) {
                    b.setText((String) original);
                    b.putClientProperty(ORIGINAL_TEXT, null);
                }
            }
            if (child instanceof Container) {
                applyTo((Container) child, iconOnly);
            }
        }
    }

    private static void styleButton(AbstractButton button) {
        if (button.getClientProperty(HOVER_EFFECT) == null) {
            button.putClientProperty("JButton.buttonType", "toolBarButton");
            button.putClientProperty(RESTING_COLOR, button.getBackground());
            button.putClientProperty(RESTING_OPAQUE, button.isOpaque());
            ButtonHoverEffect effect = new ButtonHoverEffect(button);
            button.putClientProperty(HOVER_EFFECT, effect);
            button.addMouseListener(effect);
        }
        applyModernIcon(button);
        button.setMargin(new Insets(4, 8, 4, 8));
    }

    private static void applyModernIcon(AbstractButton button) {
        Object savedText = button.getClientProperty(ORIGINAL_TEXT);
        String label = savedText instanceof String ? (String) savedText : button.getText();
        if ((label == null || label.trim().isEmpty()) && button.getAction() != null) {
            Object actionName = button.getAction().getValue(javax.swing.Action.NAME);
            label = actionName instanceof String ? (String) actionName : null;
        }
        if ((label == null || label.trim().isEmpty())) {
            label = button.getToolTipText();
        }

        if (!ModernUISettings.getInstance().isModernToolbarIcons()) {
            if (Boolean.TRUE.equals(button.getClientProperty(ORIGINAL_ICON_SAVED))) {
                Object original = button.getClientProperty(ORIGINAL_ICON);
                button.setIcon(original instanceof javax.swing.Icon ? (javax.swing.Icon) original : null);
            }
            button.putClientProperty(MODERN_ICON, null);
            return;
        }

        int configuredSize = ModernUISettings.getInstance().getToolbarIconSize();
        int iconSize = configuredSize == 0 ? 18 : configuredSize;
        javax.swing.Icon modern = ModernIcons.forToolbarLabel(label, iconSize);
        if (modern == null) {
            return;
        }
        String normalizedLabel = label == null ? "" : label.toLowerCase(java.util.Locale.ROOT);
        if (normalizedLabel.contains("data source") || normalizedLabel.contains("add data")) {
            java.net.URL mascot = ToolbarCustomizer.class.getResource(
                    "/com/modernui/autopsy/options/mascot24.png");
            if (mascot != null) {
                Image image = new ImageIcon(mascot).getImage();
                modern = new ImageIcon(image.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
            } else {
                LOGGER.warning("The Add Data Source mascot icon is missing");
            }
        }
        if (!Boolean.TRUE.equals(button.getClientProperty(ORIGINAL_ICON_SAVED))) {
            button.putClientProperty(ORIGINAL_ICON, button.getIcon());
            button.putClientProperty(ORIGINAL_ICON_SAVED, Boolean.TRUE);
        }
        button.putClientProperty(MODERN_ICON, modern);
        button.setIcon(modern);
    }

    private static final class ButtonHoverEffect extends MouseAdapter {

        private final AbstractButton button;
        private final Timer timer;
        private Color from;
        private Color to;
        private Color resting;
        private int frame;

        ButtonHoverEffect(AbstractButton button) {
            this.button = button;
            timer = new Timer(15, e -> advance());
            timer.setCoalesce(true);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (button.isEnabled() && !button.isSelected()) {
                animate(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (!button.isSelected()) {
                animate(false);
            }
        }

        private void animate(boolean entering) {
            if (timer.isRunning()) {
                timer.stop();
            }
            Object savedColor = button.getClientProperty(RESTING_COLOR);
            resting = savedColor instanceof Color ? (Color) savedColor : button.getBackground();
            from = button.getBackground();
            Color hover = UIManager.getColor("Button.hoverBackground");
            to = entering ? (hover != null ? hover : blend(resting, UIManager.getColor("Button.foreground"), 0.09f))
                    : resting;
            frame = 0;
            if (to == null || to.equals(from)) {
                finish();
            } else {
                button.setOpaque(true);
                timer.start();
            }
        }

        private void advance() {
            frame++;
            float progress = Math.min(1f, frame / 8f);
            button.setBackground(blend(from, to, progress));
            if (progress >= 1f) {
                finish();
            }
        }

        private void finish() {
            timer.stop();
            if (to != null) {
                button.setBackground(to);
            }
            if (to == resting) {
                Object opaque = button.getClientProperty(RESTING_OPAQUE);
                if (opaque instanceof Boolean) {
                    button.setOpaque((Boolean) opaque);
                }
            }
        }
    }

    private static Color blend(Color start, Color end, float amount) {
        if (start == null || end == null) {
            return start;
        }
        float inverse = 1f - amount;
        return new Color(
                Math.round(start.getRed() * inverse + end.getRed() * amount),
                Math.round(start.getGreen() * inverse + end.getGreen() * amount),
                Math.round(start.getBlue() * inverse + end.getBlue() * amount),
                Math.round(start.getAlpha() * inverse + end.getAlpha() * amount));
    }
}

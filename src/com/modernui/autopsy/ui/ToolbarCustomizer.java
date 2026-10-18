package com.modernui.autopsy.ui;

import com.modernui.autopsy.ModernUISettings;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
                applyTo(toolbar, s.isToolbarIconOnly());
                setDragHandleVisible(toolbar, !s.isLockToolbars());
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Modern UI: could not apply toolbar settings", ex);
        }
    }

    private static void watch(JComponent c) {
        if (c.getClientProperty(WATCHED) == null) {
            c.putClientProperty(WATCHED, Boolean.TRUE);
            c.addContainerListener(REAPPLY);
        }
    }

    /**
     * NetBeans adds a drag handle (the ⋮ grip) to each toolbar, tagged with
     * the client property "_toolbar_dragger_". Hiding it locks the toolbar in
     * place and removes the visual clutter.
     */
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
}

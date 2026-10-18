package com.modernui.autopsy.studio;

import com.modernui.autopsy.navigator.NavigatorTopComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/** Buttons for the plugin's own windows, shown in the Workspace toolbar. */
@ActionID(category = "Window", id = "com.modernui.autopsy.studio.CaseDashboardToolbar")
@ActionRegistration(displayName = "Case dashboard", lazy = false)
@ActionReference(path = "Toolbars/Workspace", position = 50)
public final class CaseDashboardToolbarAction extends AbstractAction implements Presenter.Toolbar {

    @Override
    public void actionPerformed(ActionEvent e) {
        CaseWorkspaceTopComponent.openWorkspace();
    }

    @Override
    public Component getToolbarPresenter() {
        JPanel deck = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        deck.setOpaque(false);
        com.modernui.autopsy.sigil.SigilWidgets.Wordmark mark = new com.modernui.autopsy.sigil.SigilWidgets.Wordmark();
        mark.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                CaseWorkspaceTopComponent.openWorkspace();
            }
        });
        deck.add(mark);
        deck.add(command("Case", "Case Workspace", Glyph.DASHBOARD,
                CaseWorkspaceTopComponent::openWorkspace));
        deck.add(command("Navigator", "Studio Navigator", Glyph.NAVIGATOR,
                NavigatorTopComponent::openNavigator));
        deck.add(command("Studio", "Modern UI Studio (Ctrl+Shift+U)", Glyph.STUDIO,
                () -> StudioDialog.show(com.modernui.autopsy.sigil.Sigil.isActive()
                        ? StudioDialog.Tab.SIGIL : StudioDialog.Tab.THEME)));
        deck.add(command("Theme", "Switch between light and dark (Ctrl+Shift+L)", Glyph.THEME,
                () -> new ToggleThemeAction().actionPerformed(new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED, "toolbar"))));
        return deck;
    }

    private static JButton command(String title, String tip, Glyph glyph, Runnable action) {
        JButton button = new JButton(title, new DashboardIcon(glyph));
        button.setToolTipText(tip);
        button.getAccessibleContext().setAccessibleDescription(tip);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("JButton.hoverForeground", UIManager.getColor("Label.foreground"));
        button.setFocusable(false);
        button.addActionListener(e -> action.run());
        return button;
    }

    private static Color accent() {
        Color accent = UIManager.getColor("Component.focusColor");
        return accent != null ? accent : new Color(190, 110, 220);
    }

    private enum Glyph {
        DASHBOARD,
        NAVIGATOR,
        STUDIO,
        THEME
    }

    private static final class DashboardIcon implements javax.swing.Icon {

        private final Glyph glyph;

        DashboardIcon(Glyph glyph) {
            this.glyph = glyph;
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
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color accent = accent();
                Color foreground = UIManager.getColor("Label.foreground");
                if (foreground == null) {
                    foreground = Color.WHITE;
                }
                g.setColor(accent);
                switch (glyph) {
                    case DASHBOARD:
                        g.fillRoundRect(1, 1, 6, 6, 2, 2);
                        g.fillRoundRect(9, 1, 6, 6, 2, 2);
                        g.fillRoundRect(1, 9, 6, 6, 2, 2);
                        g.fillRoundRect(9, 9, 6, 6, 2, 2);
                        break;
                    case NAVIGATOR:
                        int[] xs = {8, 14, 9, 2};
                        int[] ys = {1, 7, 15, 10};
                        g.drawPolygon(xs, ys, 4);
                        g.drawLine(8, 1, 9, 15);
                        g.drawLine(2, 10, 14, 7);
                        g.fillOval(6, 6, 4, 4);
                        break;
                    case STUDIO:
                        g.setStroke(new java.awt.BasicStroke(1.5f));
                        g.drawLine(2, 4, 14, 4);
                        g.drawLine(2, 8, 14, 8);
                        g.drawLine(2, 12, 14, 12);
                        g.setColor(foreground);
                        g.fillOval(4, 2, 4, 4);
                        g.fillOval(9, 6, 4, 4);
                        g.fillOval(5, 10, 4, 4);
                        break;
                    case THEME:
                        g.setColor(accent);
                        g.fillOval(1, 1, 14, 14);
                        g.setColor(UIManager.getColor("ToolBar.background") != null
                                ? UIManager.getColor("ToolBar.background") : component.getBackground());
                        g.fillOval(6, 0, 10, 12);
                        break;
                    default:
                        break;
                }
            } finally {
                g.dispose();
            }
        }
    }
}

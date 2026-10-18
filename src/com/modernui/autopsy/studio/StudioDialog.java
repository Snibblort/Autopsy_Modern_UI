package com.modernui.autopsy.studio;

import com.modernui.autopsy.navigator.NavigatorEditorPanel;
import com.modernui.autopsy.theme.ThemeStudioPanel;
import com.modernui.autopsy.ui.MenuToolbarEditorPanel;
import com.modernui.autopsy.workspace.WorkspacesPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/** The Studio window. Non-modal so you can watch changes apply. */
public final class StudioDialog {

    /** Studio tabs, in display order. */
    public enum Tab {
        THEME("Theme"),
        SIGIL("Cyber-Sigilism"),
        TOOLBARS("Toolbars & menus"),
        WORKSPACES("Workspaces"),
        NAVIGATOR("Navigator");

        final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private static JDialog dialog;
    private static JTabbedPane tabs;

    private StudioDialog() {
    }

    /** Opens the Studio (or brings it forward) on the given tab. */
    public static void show(Tab tab) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(tab));
            return;
        }
        if (dialog == null || !dialog.isDisplayable()) {
            build();
        }
        tabs.setSelectedIndex(tab.ordinal());
        dialog.setVisible(true);
        dialog.toFront();
    }

    private static void build() {
        Frame owner = WindowManager.getDefault().getMainWindow();
        dialog = new JDialog(owner, "Modern UI Studio", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        tabs = new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabType", "card");
        tabs.addTab(Tab.THEME.title, new ThemeStudioPanel());
        tabs.addTab(Tab.SIGIL.title, new com.modernui.autopsy.sigil.SigilStudioPanel());
        tabs.addTab(Tab.TOOLBARS.title, new MenuToolbarEditorPanel());
        tabs.addTab(Tab.WORKSPACES.title, new WorkspacesPanel());
        tabs.addTab(Tab.NAVIGATOR.title, new NavigatorEditorPanel());
        dialog.getContentPane().add(tabs, BorderLayout.CENTER);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setMinimumSize(new Dimension(720, 560));
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), 820), Math.max(dialog.getHeight(), 640));
        dialog.setLocationRelativeTo(owner);
    }

    /** Tools > Modern UI Studio… (also under View). */
    @ActionID(category = "Tools", id = "com.modernui.autopsy.studio.OpenStudio")
    @ActionRegistration(displayName = "Modern UI Studio…", iconBase = "com/modernui/autopsy/options/modernui_studio.png")
    @ActionReferences({
        @ActionReference(path = "Menu/Tools", position = 1450),
        @ActionReference(path = "Menu/View", position = 900, separatorBefore = 890),
        @ActionReference(path = "Shortcuts", name = "DS-U")
    })
    public static final class OpenStudioAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            show(Tab.THEME);
        }
    }
}

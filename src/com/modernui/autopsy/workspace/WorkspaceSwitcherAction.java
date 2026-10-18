package com.modernui.autopsy.workspace;

import com.modernui.autopsy.studio.StudioDialog;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.actions.Presenter;

/**
 * "Workspace: [name ▾]" switcher on its own toolbar, and Window ▸ Workspaces.
 */
@ActionID(category = "Window", id = "com.modernui.autopsy.workspace.WorkspaceSwitcherAction")
@ActionRegistration(displayName = "Workspaces", lazy = false)
@ActionReferences({
    @ActionReference(path = "Toolbars/Workspace", position = 100),
    @ActionReference(path = "Menu/Window", position = 50, separatorAfter = 60)
})
public final class WorkspaceSwitcherAction extends AbstractAction implements Presenter.Toolbar, Presenter.Menu {

    private static final String MANAGE = "Manage workspaces…";

    public WorkspaceSwitcherAction() {
        super("Workspaces");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StudioDialog.show(StudioDialog.Tab.WORKSPACES);
    }

    @Override
    public Component getToolbarPresenter() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);
        panel.add(new JLabel("Workspace:"));
        JComboBox<String> combo = new JComboBox<>();
        combo.setToolTipText("Switch window layout");
        final boolean[] updating = {false};
        Runnable fill = () -> {
            updating[0] = true;
            try {
                combo.removeAllItems();
                WorkspaceManager.list().forEach(combo::addItem);
                combo.addItem(MANAGE);
                combo.setSelectedItem(WorkspaceManager.current());
            } finally {
                updating[0] = false;
            }
        };
        fill.run();
        combo.addActionListener(e -> {
            if (updating[0]) {
                return;
            }
            Object picked = combo.getSelectedItem();
            if (MANAGE.equals(picked)) {
                SwingUtilities.invokeLater(fill);
                StudioDialog.show(StudioDialog.Tab.WORKSPACES);
            } else if (picked != null && !picked.equals(WorkspaceManager.current())) {
                WorkspaceManager.switchTo(picked.toString());
            }
        });
        WorkspaceManager.addChangeListener(e -> SwingUtilities.invokeLater(fill));
        Dimension size = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(Math.max(140, size.width), size.height));
        combo.setMaximumSize(combo.getPreferredSize());
        panel.add(combo);
        return panel;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenu menu = new JMenu("Workspaces");
        menu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                menu.removeAll();
                ButtonGroup group = new ButtonGroup();
                String current = WorkspaceManager.current();
                for (String name : WorkspaceManager.list()) {
                    JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, name.equals(current));
                    item.addActionListener(ev -> WorkspaceManager.switchTo(name));
                    group.add(item);
                    menu.add(item);
                }
                menu.addSeparator();
                JMenuItem manage = new JMenuItem(MANAGE);
                manage.addActionListener(ev -> StudioDialog.show(StudioDialog.Tab.WORKSPACES));
                menu.add(manage);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
        return menu;
    }
}

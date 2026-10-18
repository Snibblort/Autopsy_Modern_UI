package com.modernui.autopsy.workspace;

import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;

/**
 * Studio tab: create, switch and delete workspaces (saved window layouts).
 */
public final class WorkspacesPanel extends JPanel {

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> list = new JList<>(listModel);
    private final ChangeListener refresher = e -> reload();

    public WorkspacesPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = StudioUi.form();
        StudioUi.wide(top, StudioUi.section("Workspaces"));
        StudioUi.hint(top, "Each workspace remembers its own window arrangement. Switch to one, then drag, resize,");
        StudioUi.hint(top, "open or close panels: the layout is saved automatically. Example: Triage, Deep Dive, Reporting.");

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new CurrentRenderer());
        list.setFixedCellHeight(26);
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    switchSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(420, 240));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.add(StudioUi.button("New workspace…", this::createNew));
        buttons.add(StudioUi.button("Switch to", this::switchSelected));
        buttons.add(StudioUi.button("Delete", this::deleteSelected));
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(buttons, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.add(scroll, BorderLayout.CENTER);
        center.add(holder, BorderLayout.EAST);

        JLabel tip = new JLabel("Tip: the Workspace switcher on the toolbar and Window ▸ Workspaces switch layouts in one click.");
        tip.setForeground(StudioUi.muted());

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(tip, BorderLayout.SOUTH);

        reload();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        WorkspaceManager.addChangeListener(refresher);
        reload();
    }

    @Override
    public void removeNotify() {
        WorkspaceManager.removeChangeListener(refresher);
        super.removeNotify();
    }

    private void reload() {
        String selected = list.getSelectedValue();
        listModel.clear();
        WorkspaceManager.list().forEach(listModel::addElement);
        list.setSelectedValue(selected != null && listModel.contains(selected) ? selected : WorkspaceManager.current(), true);
    }

    private void createNew() {
        String name = JOptionPane.showInputDialog(this, "Name for the new workspace:", "New workspace",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        String error = WorkspaceManager.validate(name);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "New workspace", JOptionPane.WARNING_MESSAGE);
            return;
        }
        WorkspaceManager.create(name);
        reload();
    }

    private void switchSelected() {
        String name = list.getSelectedValue();
        if (name != null && !name.equals(WorkspaceManager.current())) {
            WorkspaceManager.switchTo(name);
        }
    }

    private void deleteSelected() {
        String name = list.getSelectedValue();
        if (name == null || WorkspaceManager.DEFAULT.equals(name)) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete the workspace \"" + name + "\" and its saved layout?",
                "Delete workspace", JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.OK_OPTION) {
            WorkspaceManager.delete(name);
        }
    }

    /** Marks the active workspace. */
    private static final class CurrentRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
            boolean current = value != null && value.equals(WorkspaceManager.current());
            setText(current ? value + "   ● active" : String.valueOf(value));
            setFont(l.getFont().deriveFont(current ? Font.BOLD : Font.PLAIN));
            return this;
        }
    }
}

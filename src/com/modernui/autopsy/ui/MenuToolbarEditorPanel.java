package com.modernui.autopsy.ui;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.Actions;

/**
 * Studio tab: show, hide and reorder every toolbar button and menu item, and
 * choose how toolbar buttons look.
 */
public final class MenuToolbarEditorPanel extends JPanel {

    private static final String[] ICON_SIZES = {"Autopsy default", "Small (16 px)", "Large (24 px)"};

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JCheckBox iconOnly = new JCheckBox("Icon-only toolbar buttons (names move to tooltips)");
    private final JCheckBox lockToolbars = new JCheckBox("Lock toolbars (hide the ⋮ drag handles)");
    private final JComboBox<String> iconSize = new JComboBox<>(ICON_SIZES);
    private final JButton toggle = new JButton("Hide");
    private final JLabel status = new JLabel(" ");

    public MenuToolbarEditorPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel look = StudioUi.form();
        StudioUi.wide(look, StudioUi.section("Toolbar look"));
        StudioUi.wide(look, iconOnly);
        StudioUi.wide(look, lockToolbars);
        StudioUi.row(look, "Icon size:", iconSize);
        JPanel shortcuts = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        shortcuts.add(StudioUi.button("Keyboard shortcuts…", () -> OptionsDisplayer.getDefault().open(OptionsDisplayer.KEYMAPS)));
        shortcuts.add(StudioUi.button("Reset window layout", MenuToolbarEditorPanel::resetWindows));
        StudioUi.wide(look, shortcuts);
        StudioUi.wide(look, StudioUi.section("Toolbar buttons and menu items"));
        StudioUi.hint(look, "Select an item, then hide/show or move it. Changes apply immediately and can be reset.");

        ModernUISettings s = ModernUISettings.getInstance();
        iconOnly.setSelected(s.isToolbarIconOnly());
        iconSize.setSelectedIndex(s.getToolbarIconSize() == 16 ? 1 : s.getToolbarIconSize() == 24 ? 2 : 0);
        lockToolbars.setSelected(s.isLockToolbars());
        lockToolbars.addActionListener(e -> {
            s.setLockToolbars(lockToolbars.isSelected());
            ToolbarCustomizer.apply();
        });
        iconOnly.addActionListener(e -> {
            s.setToolbarIconOnly(iconOnly.isSelected());
            ToolbarCustomizer.apply();
        });
        iconSize.addActionListener(e -> {
            int i = iconSize.getSelectedIndex();
            s.setToolbarIconSize(i == 1 ? 16 : i == 2 ? 24 : 0);
            ToolbarCustomizer.apply();
            if (i == 0) {
                status.setText("Autopsy's default size returns after a restart.");
            }
        });

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(Math.max(tree.getRowHeight(), 22));
        tree.setCellRenderer(new ItemRenderer());
        tree.addTreeSelectionListener(e -> updateButtons());
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(560, 300));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        toggle.addActionListener(e -> toggleSelected());
        buttons.add(toggle);
        buttons.add(StudioUi.button("Move up", () -> moveSelected(-1)));
        buttons.add(StudioUi.button("Move down", () -> moveSelected(1)));
        buttons.add(StudioUi.button("Reset this group", this::resetSelected));
        buttons.add(StudioUi.button("Refresh", this::reload));
        JPanel buttonHolder = new JPanel(new BorderLayout());
        buttonHolder.add(buttons, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.add(scroll, BorderLayout.CENTER);
        center.add(buttonHolder, BorderLayout.EAST);

        add(look, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        reload();
    }

    private static void resetWindows() {
        Action reset = Actions.forID("Window", "org.netbeans.core.windows.actions.ResetWindowsAction");
        if (reset != null) {
            reset.actionPerformed(new ActionEvent(MenuToolbarEditorPanel.class, ActionEvent.ACTION_PERFORMED, "reset"));
        }
    }

    public void reload() {
        String keep = selectedItem() != null ? selectedItem().path : null;
        root.removeAllChildren();
        root.add(build(UiConfig.load(UiConfig.TOOLBARS)));
        root.add(build(UiConfig.load(UiConfig.MENU)));
        treeModel.reload();
        tree.expandRow(1);
        tree.expandRow(0);
        if (keep != null) {
            select(keep);
        }
        updateButtons();
    }

    private static DefaultMutableTreeNode build(UiConfig.Item item) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(item);
        for (UiConfig.Item child : item.children) {
            node.add(build(child));
        }
        return node;
    }

    private UiConfig.Item selectedItem() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object user = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return user instanceof UiConfig.Item ? (UiConfig.Item) user : null;
    }

    private void select(String itemPath) {
        java.util.Enumeration<?> all = root.depthFirstEnumeration();
        while (all.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) all.nextElement();
            Object user = node.getUserObject();
            if (user instanceof UiConfig.Item && itemPath.equals(((UiConfig.Item) user).path)) {
                TreePath tp = new TreePath(node.getPath());
                tree.setSelectionPath(tp);
                tree.scrollPathToVisible(tp);
                return;
            }
        }
    }

    private void updateButtons() {
        UiConfig.Item item = selectedItem();
        boolean topLevel = item != null && (UiConfig.TOOLBARS.equals(item.path) || UiConfig.MENU.equals(item.path));
        toggle.setEnabled(item != null && !topLevel);
        toggle.setText(item != null && item.hidden ? "Show" : "Hide");
    }

    private void toggleSelected() {
        UiConfig.Item item = selectedItem();
        if (item == null) {
            return;
        }
        boolean ok = item.hidden ? UiConfig.show(item) : UiConfig.hide(item);
        status.setText(ok ? (item.hidden ? "Shown: " : "Hidden: ") + item.name
                : "Couldn't change " + item.name + ". Try \"Reset this group\".");
        reload();
        select(item.path);
    }

    private void moveSelected(int delta) {
        UiConfig.Item item = selectedItem();
        if (item == null || item.hidden) {
            return;
        }
        if (UiConfig.move(item, delta)) {
            reload();
            select(item.path);
        }
    }

    private void resetSelected() {
        UiConfig.Item item = selectedItem();
        if (item == null) {
            return;
        }
        UiConfig.Item group = item.folder ? item : null;
        if (group == null) {
            TreePath parent = tree.getSelectionPath().getParentPath();
            Object user = parent == null ? null : ((DefaultMutableTreeNode) parent.getLastPathComponent()).getUserObject();
            group = user instanceof UiConfig.Item ? (UiConfig.Item) user : null;
        }
        if (group != null && UiConfig.reset(group)) {
            status.setText("Restored Autopsy's original " + group.name + ".");
        }
        reload();
    }

    /** Folders bold, hidden items greyed with a label, separators faint. */
    private static final class ItemRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object user = ((DefaultMutableTreeNode) value).getUserObject();
            if (user instanceof UiConfig.Item) {
                UiConfig.Item item = (UiConfig.Item) user;
                setFont(tree.getFont().deriveFont(item.folder ? Font.BOLD : item.hidden ? Font.ITALIC : Font.PLAIN));
                setText(item.hidden ? item.name + "   (hidden)" : item.name);
                if ((item.hidden || item.isSeparator()) && !sel) {
                    setForeground(StudioUi.muted());
                }
                setIcon(null);
            }
            return this;
        }
    }
}

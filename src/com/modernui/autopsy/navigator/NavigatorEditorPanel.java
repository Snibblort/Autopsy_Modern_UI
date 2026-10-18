package com.modernui.autopsy.navigator;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.navigator.NavigatorConfig.Group;
import com.modernui.autopsy.navigator.NavigatorConfig.Kind;
import com.modernui.autopsy.navigator.NavigatorConfig.View;
import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 * Studio tab: design the navigator. Groups hold views; each view is a saved
 * query. Every change is saved and shows up in the navigator immediately.
 */
public final class NavigatorEditorPanel extends JPanel {

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JSpinner maxRows = new JSpinner(new SpinnerNumberModel(10000, 100, 1000000, 1000));
    private final JLabel status = new JLabel(" ");
    private NavigatorConfig config = NavigatorConfig.load();

    public NavigatorEditorPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = StudioUi.form();
        StudioUi.wide(top, StudioUi.section("Navigator views"));
        StudioUi.hint(top, "Organize the case your way. Groups hold views; each view is a saved search that opens in");
        StudioUi.hint(top, "Autopsy's result viewer. Ctrl+click a view in the navigator to open it in a new tab.");
        maxRows.setValue(ModernUISettings.getInstance().getNavigatorMaxResults());
        maxRows.addChangeListener(e -> ModernUISettings.getInstance().setNavigatorMaxResults((Integer) maxRows.getValue()));
        StudioUi.row(top, "Max rows per view:", maxRows);
        javax.swing.JCheckBox merge = new javax.swing.JCheckBox(
                "One column: show Autopsy's case tree inside the Navigator and hide its own panel");
        merge.setSelected(ModernUISettings.getInstance().isMergeAutopsyTree());
        merge.addActionListener(e -> {
            ModernUISettings.getInstance().setMergeAutopsyTree(merge.isSelected());
            NavigatorTopComponent.refreshMerge();
        });
        StudioUi.wide(top, merge);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(Math.max(tree.getRowHeight(), 22));
        tree.setCellRenderer(new EditorRenderer());
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(520, 300));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.add(StudioUi.button("Add group…", this::addGroup));
        buttons.add(StudioUi.button("Add view…", this::addView));
        buttons.add(StudioUi.button("Edit…", this::editSelected));
        buttons.add(StudioUi.button("Delete", this::deleteSelected));
        buttons.add(StudioUi.button("Move up", () -> moveSelected(-1)));
        buttons.add(StudioUi.button("Move down", () -> moveSelected(1)));
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(buttons, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.add(scroll, BorderLayout.CENTER);
        center.add(holder, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel fileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fileButtons.add(StudioUi.button("Restore default views", this::restoreDefaults));
        fileButtons.add(StudioUi.button("Import…", this::importConfig));
        fileButtons.add(StudioUi.button("Export…", this::exportConfig));
        bottom.add(fileButtons, BorderLayout.NORTH);
        status.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        rebuild(null);
    }

    private void rebuild(Object select) {
        root.removeAllChildren();
        DefaultMutableTreeNode toSelect = null;
        for (Group g : config.groups) {
            DefaultMutableTreeNode gn = new DefaultMutableTreeNode(g);
            if (g == select) {
                toSelect = gn;
            }
            for (View v : g.views) {
                DefaultMutableTreeNode vn = new DefaultMutableTreeNode(v);
                if (v == select) {
                    toSelect = vn;
                }
                gn.add(vn);
            }
            root.add(gn);
        }
        treeModel.reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        if (toSelect != null) {
            TreePath p = new TreePath(toSelect.getPath());
            tree.setSelectionPath(p);
            tree.scrollPathToVisible(p);
        }
    }

    private void saveAndRebuild(Object select, String message) {
        config.save();
        rebuild(select);
        status.setText(message);
    }

    private Object selected() {
        TreePath p = tree.getSelectionPath();
        return p == null ? null : ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
    }

    private Group groupOf(Object item) {
        if (item instanceof Group) {
            return (Group) item;
        }
        for (Group g : config.groups) {
            if (g.views.contains(item)) {
                return g;
            }
        }
        return null;
    }

    private void addGroup() {
        String name = JOptionPane.showInputDialog(this, "Group name:", "Add group", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Group g = new Group(name.trim());
            config.groups.add(g);
            saveAndRebuild(g, "Added group " + g.name + ".");
        }
    }

    private void addView() {
        Group g = groupOf(selected());
        if (g == null) {
            if (config.groups.isEmpty()) {
                config.groups.add(new Group("My views"));
            }
            g = config.groups.get(config.groups.size() - 1);
        }
        View v = new View("New view", Kind.EXTENSIONS, "");
        if (editView(v, "Add view to " + g.name)) {
            g.views.add(v);
            saveAndRebuild(v, "Added " + v.name + ".");
        }
    }

    private void editSelected() {
        Object item = selected();
        if (item instanceof Group) {
            Group g = (Group) item;
            String name = JOptionPane.showInputDialog(this, "Group name:", g.name);
            if (name != null && !name.trim().isEmpty()) {
                g.name = name.trim();
                saveAndRebuild(g, "Renamed group.");
            }
        } else if (item instanceof View) {
            View v = (View) item;
            if (editView(v, "Edit view")) {
                saveAndRebuild(v, "Saved " + v.name + ".");
            }
        }
    }

    /** Edits a view in a small form. Returns true if OK was pressed with valid input. */
    private boolean editView(View v, String title) {
        JTextField name = new JTextField(v.name, 28);
        JComboBox<Kind> kind = new JComboBox<>(Kind.values());
        kind.setSelectedItem(v.kind);
        JTextField param = new JTextField(v.param, 28);
        JLabel hint = new JLabel(v.kind.getHint());
        hint.setForeground(StudioUi.muted());
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC));
        JButton pickTypes = StudioUi.button("Choose artifact types…", () -> {
            String chosen = chooseArtifactTypes(param.getText());
            if (chosen != null) {
                param.setText(chosen);
            }
        });
        JButton test = StudioUi.button("Test on open case", () -> hint.setText(test(new View(name.getText(),
                (Kind) kind.getSelectedItem(), param.getText()))));
        Runnable sync = () -> {
            Kind k = (Kind) kind.getSelectedItem();
            hint.setText(k.getHint());
            pickTypes.setVisible(k == Kind.ARTIFACTS);
            param.setEnabled(k != Kind.DELETED);
        };
        kind.addActionListener(e -> sync.run());
        sync.run();

        JPanel form = StudioUi.form();
        StudioUi.row(form, "Name:", name);
        StudioUi.row(form, "Shows:", kind);
        StudioUi.row(form, "Parameter:", param);
        StudioUi.wide(form, hint);
        JPanel extra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        extra.add(pickTypes);
        extra.add(test);
        StudioUi.wide(form, extra);

        int answer = JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION || name.getText().trim().isEmpty()) {
            return false;
        }
        v.name = name.getText().trim();
        v.kind = (Kind) kind.getSelectedItem();
        v.param = param.getText().trim();
        return true;
    }

    /** Counts the view on the open case so the user can check a filter before saving. */
    private static String test(View v) {
        if (!Case.isCaseOpen()) {
            return "Open a case to test this view.";
        }
        try {
            long n = NavigatorQuery.count(Case.getCurrentCaseThrows(), v);
            return "Matches " + java.text.NumberFormat.getIntegerInstance().format(n) + " item(s) in this case.";
        } catch (Exception ex) {
            return "Error: " + ex.getMessage();
        }
    }

    private String chooseArtifactTypes(String current) {
        List<String> selected = new ArrayList<>();
        for (String s : current.split("[,;\\s]+")) {
            if (!s.trim().isEmpty()) {
                selected.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
        BlackboardArtifact.ARTIFACT_TYPE[] types = BlackboardArtifact.ARTIFACT_TYPE.values();
        JList<BlackboardArtifact.ARTIFACT_TYPE> list = new JList<>(types);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                BlackboardArtifact.ARTIFACT_TYPE t = (BlackboardArtifact.ARTIFACT_TYPE) value;
                setText(t.getDisplayName() + "   (" + t.getLabel() + ")");
                return this;
            }
        });
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            if (selected.contains(types[i].getLabel())) {
                indices.add(i);
            }
        }
        list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(460, 360));
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel("Ctrl+click to pick several types:"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        int answer = JOptionPane.showConfirmDialog(this, panel, "Artifact types", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (BlackboardArtifact.ARTIFACT_TYPE t : list.getSelectedValuesList()) {
            names.add(t.getLabel());
        }
        return String.join(", ", names);
    }

    private void deleteSelected() {
        Object item = selected();
        if (item instanceof Group) {
            int answer = JOptionPane.showConfirmDialog(this, "Delete the group \"" + item + "\" and its views?",
                    "Delete", JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION) {
                config.groups.remove(item);
                saveAndRebuild(null, "Deleted group.");
            }
        } else if (item instanceof View) {
            Group g = groupOf(item);
            if (g != null) {
                g.views.remove(item);
                saveAndRebuild(g, "Deleted view.");
            }
        }
    }

    private void moveSelected(int delta) {
        Object item = selected();
        if (item instanceof Group) {
            move(config.groups, (Group) item, delta);
        } else if (item instanceof View) {
            Group g = groupOf(item);
            if (g != null) {
                move(g.views, (View) item, delta);
            }
        }
        saveAndRebuild(item, " ");
    }

    private static <T> void move(List<T> list, T item, int delta) {
        int i = list.indexOf(item);
        int j = i + delta;
        if (i >= 0 && j >= 0 && j < list.size()) {
            list.remove(i);
            list.add(j, item);
        }
    }

    private void restoreDefaults() {
        int answer = JOptionPane.showConfirmDialog(this, "Replace your navigator views with the default set?",
                "Restore default views", JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.OK_OPTION) {
            config = NavigatorConfig.defaults();
            saveAndRebuild(null, "Restored the default views.");
        }
    }

    private void importConfig() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String text = new String(Files.readAllBytes(chooser.getSelectedFile().toPath()), StandardCharsets.UTF_8);
            NavigatorConfig imported = NavigatorConfig.fromText(text);
            if (imported.groups.isEmpty()) {
                status.setText("That file has no navigator views.");
                return;
            }
            config = imported;
            saveAndRebuild(null, "Imported " + imported.groups.size() + " group(s).");
        } catch (IOException ex) {
            status.setText("Could not read the file: " + ex.getMessage());
        }
    }

    private void exportConfig() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("autopsy-navigator.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.write(chooser.getSelectedFile().toPath(), config.toText().getBytes(StandardCharsets.UTF_8));
            status.setText("Exported to " + chooser.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            status.setText("Could not write the file: " + ex.getMessage());
        }
    }

    private static final class EditorRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object user = ((DefaultMutableTreeNode) value).getUserObject();
            setIcon(null);
            if (user instanceof Group) {
                setFont(tree.getFont().deriveFont(Font.BOLD));
            } else if (user instanceof View) {
                View v = (View) user;
                setFont(tree.getFont());
                String p = v.param.length() > 48 ? v.param.substring(0, 45) + "…" : v.param;
                setText(v.name + "   — " + v.kind + (p.isEmpty() ? "" : ": " + p));
            }
            return this;
        }
    }
}

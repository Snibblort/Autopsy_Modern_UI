package com.modernui.autopsy.navigator;

import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.TreeView;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.sleuthkit.autopsy.directorytree.DirectoryTreeTopComponent;

/**
 * Autopsy's own case tree (Data Sources, File Views, Data Artifacts, Analysis
 * Results, OS Accounts, Tags, Score, Reports), shown inside the Navigator.
 *
 * <p>This is a second view of the same tree, not a copy. This panel hands its
 * BeanTreeView Autopsy's own ExplorerManager. Autopsy listens to that
 * manager for selections, so clicking a node here updates the result viewer
 * exactly as clicking it in Autopsy's tree does, and Autopsy's "go to" actions
 * select and expand nodes here too.
 */
final class AutopsyTreeSection extends JPanel implements ExplorerManager.Provider {

    private static final Logger LOGGER = Logger.getLogger(AutopsyTreeSection.class.getName());

    private final ExplorerManager manager;
    private final BeanTreeView view = new BeanTreeView();

    AutopsyTreeSection() {
        this(DirectoryTreeTopComponent.findInstance().getExplorerManager(), DirectoryTreeTopComponent.findInstance());
    }

    /** {@code autopsyTree} may be null (no Back/Forward/⚙ buttons); used by tests. */
    AutopsyTreeSection(ExplorerManager manager, DirectoryTreeTopComponent autopsyTree) {
        super(new BorderLayout());
        this.manager = manager;

        view.setRootVisible(false);
        view.setBorder(BorderFactory.createEmptyBorder());

        JLabel label = new JLabel("Case");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttons.setOpaque(false);
        JButton back = autopsyTree == null ? null : proxy(autopsyTree, "backButton", "←", "Back");
        JButton forward = autopsyTree == null ? null : proxy(autopsyTree, "forwardButton", "→", "Forward");
        if (back != null) {
            buttons.add(back);
        }
        if (forward != null) {
            buttons.add(forward);
        }
        JPopupMenu prefs = autopsyTree == null ? null : field(autopsyTree, "viewPreferencesPopupMenu", JPopupMenu.class);
        if (prefs != null) {
            JButton gear = toolButton("⚙", "View preferences (hide known files, group by data source…)");
            gear.addActionListener(e -> prefs.show(gear, 0, gear.getHeight()));
            buttons.add(gear);
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        header.add(label, BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        installSelectionSync();
    }

    /**
     * NetBeans normally keeps a tree view and its ExplorerManager in sync
     * once the view is on screen. This adds the same sync explicitly in both
     * directions, so the Navigator's copy of the tree drives Autopsy (and
     * follows Autopsy's own "go to" selections) regardless of timing. Each
     * side only acts when the other differs, so the two never loop.
     */
    private void installSelectionSync() {
        JTree tree = innerTree(view);
        if (tree == null) {
            return;
        }
        tree.getSelectionModel().addTreeSelectionListener(e -> {
            if (syncing) {
                return;
            }
            Node[] fromView = nodesFor(tree.getSelectionPaths());
            if (fromView.length > 0 && !Arrays.equals(fromView, manager.getSelectedNodes())) {
                syncing = true;
                try {
                    manager.setSelectedNodes(fromView);
                } catch (PropertyVetoException ex) {
                    LOGGER.log(Level.FINE, "Selection vetoed", ex);
                } finally {
                    syncing = false;
                }
            }
        });
        manager.addPropertyChangeListener(WeakListeners.propertyChange(managerListener = evt -> {
            if (!ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName()) || syncing) {
                return;
            }
            Runnable apply = () -> {
                Node[] wanted = manager.getSelectedNodes();
                if (Arrays.equals(wanted, nodesFor(tree.getSelectionPaths()))) {
                    return;
                }
                List<TreePath> paths = new ArrayList<>();
                for (Node n : wanted) {
                    TreePath path = pathFor(n);
                    if (path != null) {
                        paths.add(path);
                    }
                }
                if (!paths.isEmpty()) {
                    syncing = true;
                    try {
                        for (TreePath path : paths) {
                            tree.expandPath(path.getParentPath());
                        }
                        tree.setSelectionPaths(paths.toArray(new TreePath[0]));
                        tree.scrollPathToVisible(paths.get(0));
                    } finally {
                        syncing = false;
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                apply.run();
            } else {
                SwingUtilities.invokeLater(apply);
            }
        }, manager));
    }

    private boolean syncing;
    private java.beans.PropertyChangeListener managerListener;

    private Node[] nodesFor(TreePath[] paths) {
        if (paths == null) {
            return new Node[0];
        }
        List<Node> nodes = new ArrayList<>();
        for (TreePath path : paths) {
            Node n = Visualizer.findNode(path.getLastPathComponent());
            if (n != null) {
                nodes.add(n);
            }
        }
        return nodes.toArray(new Node[0]);
    }

    /** The tree path (of view objects) leading to a node, or null if it isn't under the root. */
    private TreePath pathFor(Node node) {
        List<Object> chain = new ArrayList<>();
        Node root = manager.getRootContext();
        for (Node n = node; n != null; n = n.getParentNode()) {
            chain.add(0, Visualizer.findVisualizer(n));
            if (n == root) {
                return new TreePath(chain.toArray());
            }
        }
        return null;
    }

    /** TreeView keeps its JTree in a protected field. */
    private static JTree innerTree(TreeView view) {
        try {
            Field f = TreeView.class.getDeclaredField("tree");
            f.setAccessible(true);
            Object t = f.get(view);
            return t instanceof JTree ? (JTree) t : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.log(Level.FINE, "TreeView tree not accessible", ex);
            return null;
        }
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    /**
     * A button that clicks one of Autopsy's own (now hidden) tree buttons, so
     * Back/Forward keep Autopsy's history logic. It mirrors the original's
     * enabled state.
     */
    private static JButton proxy(DirectoryTreeTopComponent tree, String fieldName, String text, String tip) {
        JButton original = field(tree, fieldName, JButton.class);
        if (original == null) {
            return null;
        }
        JButton b = toolButton(text, tip);
        b.setEnabled(original.isEnabled());
        original.addPropertyChangeListener("enabled", e -> b.setEnabled(original.isEnabled()));
        b.addActionListener(e -> original.doClick(0));
        return b;
    }

    private static JButton toolButton(String text, String tip) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setForeground(StudioUi.muted());
        return b;
    }

    private static <T> T field(Object owner, String name, Class<T> type) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object value = f.get(owner);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.log(Level.FINE, "Autopsy tree field not available: " + name, ex);
            return null;
        }
    }
}

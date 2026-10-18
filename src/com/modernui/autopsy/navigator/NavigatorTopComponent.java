package com.modernui.autopsy.navigator;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.navigator.NavigatorConfig.Group;
import com.modernui.autopsy.navigator.NavigatorConfig.View;
import com.modernui.autopsy.studio.StudioDialog;
import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.RequestProcessor;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.ingest.IngestManager;

/**
 * The Studio Navigator: a left-side tree of the views you define in the
 * Studio, each with a live count. Clicking a view opens its results in
 * Autopsy's result viewer. It sits alongside Autopsy's own tree.
 */
@TopComponent.Description(preferredID = NavigatorTopComponent.ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "modernuiNavigator", openAtStartup = true)
@ActionID(category = "Window", id = "com.modernui.autopsy.navigator.OpenNavigator")
@ActionReference(path = "Menu/Window", position = 40)
@TopComponent.OpenActionRegistration(displayName = "Studio Navigator", preferredID = NavigatorTopComponent.ID)
public final class NavigatorTopComponent extends TopComponent {

    public static final String ID = "ModernUINavigatorTopComponent";

    private static final String DOCK_MODE = "modernuiNavigator";
    private static final Logger LOGGER = Logger.getLogger(NavigatorTopComponent.class.getName());
    private static final RequestProcessor RP = new RequestProcessor("Modern UI navigator", 1, true);
    private static final String CARD_TREE = "tree";
    private static final String CARD_EMPTY = "empty";

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JTextField filter = new JTextField();
    private final JLabel caseLabel = new JLabel(" ");
    private final JLabel emptyLabel = new JLabel("Open a case to use the navigator.", SwingConstants.CENTER);
    private final JPanel viewsPanel = new JPanel(new BorderLayout());
    private final JPanel caseTreeTab = new JPanel(new BorderLayout());
    private final JPanel savedViewsTab = new JPanel(new BorderLayout());
    private final JTabbedPane sections = new JTabbedPane();
    private AutopsyTreeSection caseTree;
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final Map<View, String> counts = new IdentityHashMap<>();
    private final Timer refreshTimer = new Timer(1500, e -> refreshCounts());

    private NavigatorConfig config = NavigatorConfig.load();
    private RequestProcessor.Task countTask;

    private final PropertyChangeListener caseListener = evt -> SwingUtilities.invokeLater(() -> {
        refreshTimer.restart();
        TreeMerger.enforce(this);
    });
    private final PropertyChangeListener ingestListener = evt -> SwingUtilities.invokeLater(refreshTimer::restart);
    private final ChangeListener configListener = e -> SwingUtilities.invokeLater(this::reloadConfig);

    private static final EnumSet<Case.Events> CASE_EVENTS = EnumSet.of(Case.Events.CURRENT_CASE,
            Case.Events.DATA_SOURCE_ADDED, Case.Events.CONTENT_TAG_ADDED, Case.Events.CONTENT_TAG_DELETED,
            Case.Events.BLACKBOARD_ARTIFACT_TAG_ADDED, Case.Events.BLACKBOARD_ARTIFACT_TAG_DELETED);
    private static final EnumSet<IngestManager.IngestJobEvent> INGEST_EVENTS = EnumSet.of(
            IngestManager.IngestJobEvent.COMPLETED, IngestManager.IngestJobEvent.CANCELLED,
            IngestManager.IngestJobEvent.DATA_SOURCE_ANALYSIS_COMPLETED);

    public NavigatorTopComponent() {
        setName("Navigator");
        setToolTipText("Navigator: Autopsy's case tree plus your own views");
        setLayout(new BorderLayout());
        refreshTimer.setRepeats(false);

        JLabel title = new JLabel("Navigator");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1f));
        caseLabel.setForeground(StudioUi.muted());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        actions.add(toolButton("⟳", "Refresh counts", this::refreshCounts));
        actions.add(toolButton("✎", "Edit navigator views", () -> StudioDialog.show(StudioDialog.Tab.NAVIGATOR)));
        JButton dock = toolButton("⋮", "Move this panel", () -> { });
        dock.addActionListener(e -> dockMenu().show(dock, 0, dock.getHeight()));
        actions.add(dock);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(actions, BorderLayout.EAST);

        filter.putClientProperty("JTextField.placeholderText", "Filter views…");
        filter.putClientProperty("JTextField.showClearButton", true);
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuildTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuildTree();
            }
        });

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(caseLabel, BorderLayout.CENTER);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(Math.max(tree.getRowHeight(), 24));
        tree.setCellRenderer(new ViewRenderer());
        tree.setToggleClickCount(1);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                Object user = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (user instanceof View && SwingUtilities.isLeftMouseButton(e)) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();
                    openView(String.valueOf(parent.getUserObject()), (View) user, e.isControlDown());
                }
            }
        });
        tree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    TreePath path = tree.getSelectionPath();
                    Object user = path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (user instanceof View) {
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) path.getLastPathComponent()).getParent();
                        openView(String.valueOf(parent.getUserObject()), (View) user, e.isControlDown());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        emptyLabel.setForeground(StudioUi.muted());
        body.add(scroll, CARD_TREE);
        body.add(emptyLabel, CARD_EMPTY);

        JLabel viewsLabel = new JLabel("My views");
        viewsLabel.setFont(viewsLabel.getFont().deriveFont(Font.BOLD));
        JPanel viewsHeader = new JPanel(new BorderLayout(0, 4));
        viewsHeader.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        viewsHeader.add(viewsLabel, BorderLayout.NORTH);
        viewsHeader.add(filter, BorderLayout.SOUTH);
        viewsPanel.add(viewsHeader, BorderLayout.NORTH);
        viewsPanel.add(body, BorderLayout.CENTER);

        caseTreeTab.setOpaque(false);
        savedViewsTab.setOpaque(false);
        savedViewsTab.add(viewsPanel, BorderLayout.CENTER);
        sections.putClientProperty("JTabbedPane.tabType", "underlined");
        sections.setBorder(BorderFactory.createEmptyBorder());
        sections.addTab("Case tree", caseTreeTab);
        sections.addTab("Saved views", savedViewsTab);

        add(header, BorderLayout.NORTH);
        add(sections, BorderLayout.CENTER);
        rebuildTree();
    }

    /** Finds the open Navigator (if any) and re-applies the one-column setting. */
    public static void refreshMerge() {
        TopComponent tc = WindowManager.getDefault().findTopComponent(ID);
        if (tc instanceof NavigatorTopComponent) {
            ((NavigatorTopComponent) tc).applyMerge();
        }
    }

    /** Opens one of the saved Navigator searches from another Modern UI surface. */
    public static boolean openSavedView(String groupName, String viewName) {
        NavigatorTopComponent navigator = findNavigator();
        if (navigator == null) {
            return false;
        }
        for (Group group : NavigatorConfig.load().groups) {
            if (!group.name.equals(groupName)) {
                continue;
            }
            for (View view : group.views) {
                if (view.name.equals(viewName)) {
                    navigator.dockToNavigatorMode();
                    navigator.open();
                    navigator.requestActive();
                    if (ModernUISettings.getInstance().isMergeAutopsyTree()) {
                        navigator.sections.setSelectedComponent(navigator.savedViewsTab);
                    }
                    navigator.openView(group.name, view, false);
                    return true;
                }
            }
        }
        return false;
    }

    /** Opens the Navigator in its joined side column rather than a sliding overlay. */
    public static boolean openNavigator() {
        NavigatorTopComponent navigator = findNavigator();
        if (navigator == null) {
            return false;
        }
        navigator.dockToNavigatorMode();
        navigator.open();
        navigator.requestActive();
        return true;
    }

    private static NavigatorTopComponent findNavigator() {
        TopComponent tc = WindowManager.getDefault().findTopComponent(ID);
        return tc instanceof NavigatorTopComponent ? (NavigatorTopComponent) tc : null;
    }

    private void dockToNavigatorMode() {
        Mode target = WindowManager.getDefault().findMode(DOCK_MODE);
        if (target == null) {
            LOGGER.warning("Modern UI: the joined Navigator docking mode is unavailable");
            return;
        }
        if (WindowManager.getDefault().findMode(this) != target) {
            target.dockInto(this);
        }
    }

    /**
     * One column: switch between Autopsy's case tree and your saved views.
     * Otherwise show only saved views and leave Autopsy's tree in its own panel.
     */
    void applyMerge() {
        boolean merge = ModernUISettings.getInstance().isMergeAutopsyTree();
        if (merge) {
            if (caseTree == null) {
                try {
                    caseTree = new AutopsyTreeSection();
                } catch (RuntimeException | LinkageError ex) {
                    LOGGER.log(Level.WARNING, "Modern UI: could not show Autopsy's tree in the Navigator", ex);
                }
            }
        }
        boolean showCaseTree = merge && caseTree != null;
        int caseTreeIndex = sections.indexOfComponent(caseTreeTab);
        if (showCaseTree) {
            caseTreeTab.removeAll();
            caseTreeTab.add(caseTree, BorderLayout.CENTER);
            if (caseTreeIndex < 0) {
                sections.insertTab("Case tree", null, caseTreeTab, null, 0);
            }
            sections.setSelectedComponent(caseTreeTab);
        } else {
            if (caseTreeIndex >= 0) {
                sections.removeTabAt(caseTreeIndex);
            }
            sections.setSelectedComponent(savedViewsTab);
        }
        revalidate();
        repaint();
        TreeMerger.update(this);
    }

    private static JButton toolButton(String text, String tip, Runnable action) {
        JButton b = StudioUi.button(text, action);
        b.setToolTipText(tip);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        return b;
    }

    private JPopupMenu dockMenu() {
        JPopupMenu menu = new JPopupMenu();
        addDock(menu, "Left side (own area)", DOCK_MODE);
        addDock(menu, "Right side", "properties", "commonpalette");
        addDock(menu, "Bottom", "output");
        addDock(menu, "Floating window", "floatingLeftBottom");
        return menu;
    }

    private void addDock(JPopupMenu menu, String label, String... modeNames) {
        JMenuItem item = new JMenuItem(label);
        Mode target = null;
        for (String name : modeNames) {
            target = WindowManager.getDefault().findMode(name);
            if (target != null) {
                break;
            }
        }
        final Mode mode = target;
        item.setEnabled(mode != null);
        item.addActionListener(e -> {
            mode.dockInto(this);
            open();
            requestActive();
        });
        menu.add(item);
    }

    @Override
    protected void componentOpened() {
        dockToNavigatorMode();
        Case.addEventTypeSubscriber(CASE_EVENTS, caseListener);
        IngestManager.getInstance().addIngestJobEventListener(INGEST_EVENTS, ingestListener);
        NavigatorConfig.addChangeListener(configListener);
        reloadConfig();
        SwingUtilities.invokeLater(this::applyMerge);
    }

    @Override
    protected void componentClosed() {
        Case.removeEventTypeSubscriber(CASE_EVENTS, caseListener);
        IngestManager.getInstance().removeIngestJobEventListener(INGEST_EVENTS, ingestListener);
        NavigatorConfig.removeChangeListener(configListener);
        refreshTimer.stop();
    }

    private void reloadConfig() {
        config = NavigatorConfig.load();
        counts.clear();
        rebuildTree();
        refreshCounts();
    }

    /** Rebuilds the tree from the config, applying the filter text. */
    private void rebuildTree() {
        String q = filter.getText().trim().toLowerCase(Locale.ROOT);
        root.removeAllChildren();
        for (Group g : config.groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(g);
            boolean groupMatches = !q.isEmpty() && g.name.toLowerCase(Locale.ROOT).contains(q);
            for (View v : g.views) {
                if (q.isEmpty() || groupMatches || v.name.toLowerCase(Locale.ROOT).contains(q)) {
                    groupNode.add(new DefaultMutableTreeNode(v));
                }
            }
            if (groupNode.getChildCount() > 0) {
                root.add(groupNode);
            }
        }
        treeModel.reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /** Recounts every view in the background. */
    private void refreshCounts() {
        if (!Case.isCaseOpen()) {
            caseLabel.setText("No case open");
            cards.show(body, CARD_EMPTY);
            return;
        }
        cards.show(body, CARD_TREE);
        final Case current;
        try {
            current = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException ex) {
            cards.show(body, CARD_EMPTY);
            return;
        }
        caseLabel.setText(current.getDisplayName());
        if (countTask != null) {
            countTask.cancel();
        }
        final NavigatorConfig snapshot = config;
        for (Group g : snapshot.groups) {
            for (View v : g.views) {
                counts.put(v, "…");
            }
        }
        relayoutRows();
        countTask = RP.post(() -> {
            NumberFormat fmt = NumberFormat.getIntegerInstance();
            for (Group g : snapshot.groups) {
                for (View v : g.views) {
                    if (Thread.interrupted()) {
                        return;
                    }
                    String text;
                    try {
                        text = fmt.format(NavigatorQuery.count(current, v));
                    } catch (Exception ex) {
                        LOGGER.log(Level.FINE, "Count failed for view " + v.name, ex);
                        text = "error";
                    }
                    final String shown = text;
                    SwingUtilities.invokeLater(() -> {
                        counts.put(v, shown);
                        relayoutRows();
                    });
                }
            }
        });
    }

    private void openView(String groupName, View view, boolean newTab) {
        final Case current;
        try {
            current = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException ex) {
            return;
        }
        final int max = ModernUISettings.getInstance().getNavigatorMaxResults();
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        RP.post(() -> {
            try {
                List<Object> items = NavigatorQuery.fetch(current, view, max);
                boolean truncated = items.size() > max;
                List<Object> shown = truncated ? items.subList(0, max) : items;
                SwingUtilities.invokeLater(() -> ResultsOpener.open(groupName, view.name, shown, truncated, newTab));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Modern UI navigator: could not load " + view.name, ex);
                SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(this,
                        "Couldn't load \"" + view.name + "\":\n" + ex.getMessage(), "Navigator",
                        javax.swing.JOptionPane.WARNING_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(null));
            }
        });
    }

    /**
     * Tells the tree every row's size may have changed. JTree caches row
     * widths, so without this a label that grows (when its count arrives)
     * gets clipped to the old width.
     */
    private void relayoutRows() {
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode group = (DefaultMutableTreeNode) root.getChildAt(i);
            treeModel.nodeChanged(group);
            for (int j = 0; j < group.getChildCount(); j++) {
                treeModel.nodeChanged(group.getChildAt(j));
            }
        }
    }

    /** Group names in bold; each view's count drawn after its name in a muted color. */
    private final class ViewRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, hasFocus);
            Object user = ((DefaultMutableTreeNode) value).getUserObject();
            setIcon(null);
            setHorizontalTextPosition(SwingConstants.LEADING);
            setIconTextGap(10);
            if (user instanceof Group) {
                setFont(t.getFont().deriveFont(Font.BOLD));
                setText(((Group) user).name);
            } else if (user instanceof View) {
                View v = (View) user;
                setFont(t.getFont());
                setText(v.name);
                String count = counts.get(v);
                if (count != null) {
                    setIcon(new CountIcon(count, getFont(), sel ? getForeground() : StudioUi.muted()));
                }
            }
            return this;
        }
    }

    /** Draws the count text; as an icon it is included in the row's measured width. */
    private static final class CountIcon implements javax.swing.Icon {

        private final String text;
        private final Font font;
        private final Color color;
        private final java.awt.FontMetrics metrics;

        CountIcon(String text, Font font, Color color) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.metrics = new java.awt.Canvas().getFontMetrics(font);
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(font);
                g2.setColor(color);
                g2.drawString(text, x, y + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return metrics.stringWidth(text);
        }

        @Override
        public int getIconHeight() {
            return metrics.getHeight();
        }
    }

}

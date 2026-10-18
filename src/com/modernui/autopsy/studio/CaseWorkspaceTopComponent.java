package com.modernui.autopsy.studio;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.navigator.NavigatorTopComponent;
import com.modernui.autopsy.sigil.Sigil;
import com.modernui.autopsy.sigil.SigilArt;
import com.modernui.autopsy.sigil.SigilClock;
import com.modernui.autopsy.sigil.SigilPanel;
import com.modernui.autopsy.sigil.SigilWidgets;
import com.modernui.autopsy.ui.ModernIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.RequestProcessor;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.ingest.IngestJobProgressSnapshot;
import org.sleuthkit.autopsy.ingest.IngestManager;

/** Start page in the editor area: the open case plus shortcuts to a few saved Navigator views. */
@TopComponent.Description(preferredID = CaseWorkspaceTopComponent.ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "com.modernui.autopsy.studio.CaseWorkspace")
@ActionReference(path = "Menu/Window", position = 35)
@TopComponent.OpenActionRegistration(displayName = "Case Workspace", preferredID = CaseWorkspaceTopComponent.ID)
public final class CaseWorkspaceTopComponent extends TopComponent {

    static final String ID = "ModernUICaseWorkspaceTopComponent";
    private static final Logger LOGGER = Logger.getLogger(CaseWorkspaceTopComponent.class.getName());
    private static final RequestProcessor STATUS_WORKER = new RequestProcessor("Modern UI case summary", 1, true);
    private static final AtomicLong STATUS_SEQUENCE = new AtomicLong();
    private static final String EDITOR_MODE = "editor";
    private static final int TWO_COLUMN_MIN_WIDTH = 820;
    private static final int TWO_ACTION_COLUMN_MIN_WIDTH = 340;

    private static final List<Shortcut> SHORTCUTS = createShortcuts();
    private static final String NO_CASE = "No case open";

    private final JLabel caseName = new SigilWidgets.Title(NO_CASE, 40f, 8f);
    private final JLabel caseStatus = new JLabel(" ");
    private final JLabel caseDetails = new JLabel(" ");
    private final JPanel cards = new JPanel(new GridLayout(0, 1, 12, 12));
    private JPanel page = new JPanel(new BorderLayout(0, 18));
    private boolean sigilMode;
    private final PropertyChangeListener lafListener = evt -> {
        if ("lookAndFeel".equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(this::rebuild);
        }
    };
    private final List<JPanel> cardActions = new ArrayList<>();
    private int cardColumns = 1;
    private int actionColumns = 1;
    private final PropertyChangeListener caseListener = evt -> refreshCaseState();
    private final PropertyChangeListener ingestListener = evt -> refreshCaseState();
    private static final EnumSet<IngestManager.IngestJobEvent> STATUS_INGEST_EVENTS = EnumSet.of(
            IngestManager.IngestJobEvent.STARTED, IngestManager.IngestJobEvent.COMPLETED,
            IngestManager.IngestJobEvent.CANCELLED, IngestManager.IngestJobEvent.DATA_SOURCE_ANALYSIS_STARTED,
            IngestManager.IngestJobEvent.DATA_SOURCE_ANALYSIS_COMPLETED);

    public CaseWorkspaceTopComponent() {
        setName("Case Workspace");
        setLayout(new BorderLayout());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout(getWidth());
            }
        });
        rebuild();
        // Cyber-Sigilism and the plain look use different layouts, so the page
        // is rebuilt whenever the theme changes.
        UIManager.addPropertyChangeListener(lafListener);
        refreshCaseState();
    }

    private void rebuild() {
        removeAll();
        cards.removeAll();
        cardActions.clear();
        cardColumns = -1;
        sigilMode = Sigil.isActive();
        page = sigilMode ? new SigilWidgets.Backdrop(new BorderLayout(0, 18)) : new JPanel(new BorderLayout(0, 18));
        page.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        page.add(sigilMode ? buildSigilHeader() : buildHeader(), BorderLayout.NORTH);

        cards.setOpaque(false);
        int index = 0;
        for (Shortcut shortcut : SHORTCUTS) {
            cards.add(sigilMode ? createSigilCard(shortcut, index++) : createCard(shortcut));
        }

        JPanel cardHost = new ViewportPanel();
        cardHost.setOpaque(false);
        cardHost.setLayout(new BorderLayout());
        cardHost.add(cards, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(cardHost,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout(scroll.getViewport().getWidth());
            }
        });
        page.add(scroll, BorderLayout.CENTER);
        page.add(buildFooter(), BorderLayout.SOUTH);
        add(page, BorderLayout.CENTER);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> updateResponsiveLayout(getWidth()));
    }

    /** Cyber-Sigilism header: spaced caps, the case name in the display face, an emblem and a divider. */
    private JPanel buildSigilHeader() {
        caseDetails.setForeground(Sigil.TEXT_SECONDARY);
        caseStatus.setForeground(Sigil.secondary());

        JPanel text = new JPanel();
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel caps = SigilWidgets.caps("CASE FILE");
        caps.setAlignmentX(LEFT_ALIGNMENT);
        caseName.setAlignmentX(LEFT_ALIGNMENT);
        caseDetails.setAlignmentX(LEFT_ALIGNMENT);
        caseStatus.setAlignmentX(LEFT_ALIGNMENT);
        text.add(caps);
        text.add(caseName);
        text.add(caseDetails);
        text.add(caseStatus);

        JPanel header = new JPanel(new BorderLayout(16, 6));
        header.setOpaque(false);
        header.add(text, BorderLayout.CENTER);
        if (Sigil.atLeast(ModernUISettings.SigilDetail.MEDIUM)) {
            header.add(new HeaderEmblem(), BorderLayout.EAST);
        }
        header.add(new SigilWidgets.Divider(), BorderLayout.SOUTH);
        return header;
    }

    /** One of the four shortcut groups as an arch-topped window with its own sigil. */
    private JPanel createSigilCard(Shortcut shortcut, int index) {
        SigilPanel card = new SigilPanel(new BorderLayout(0, 12), SigilPanel.Shape2.ARCH,
                0x5EED0L + index * 7919L).withEmblem();
        JLabel title = new SigilWidgets.Title(shortcut.title, 26f, 2f);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        for (ViewShortcut view : shortcut.views) {
            javax.swing.Icon icon = ModernIcons.forToolbarLabel(view.label, 16);
            JButton button = icon == null ? new JButton(view.label) : new JButton(view.label, icon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> openView(view));
            actions.add(button);
        }
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(actions, BorderLayout.SOUTH);
        card.add(title, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    /** Large sigil inside a turning rose window, top right of the header. */
    private static final class HeaderEmblem extends javax.swing.JComponent {

        HeaderEmblem() {
            SigilClock.attach(this);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(104, 104);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.isActive()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                double c = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                if (Sigil.atLeast(ModernUISettings.SigilDetail.HIGH)) {
                    SigilArt.roseWindow(g2, c, cy, 52, SigilClock.phase(90), Sigil.alpha(Sigil.primary(), 0.35f), 12);
                }
                SigilArt.sigil(g2, c, cy, 70, 0xCA5EL, Sigil.primary(), Sigil.glow());
            } finally {
                g2.dispose();
            }
        }
    }

    public static boolean openWorkspace() {
        WindowManager windowManager = WindowManager.getDefault();
        TopComponent workspace = windowManager.findTopComponent(ID);
        if (workspace == null) {
            return false;
        }
        dockIntoEditor(windowManager, workspace);
        workspace.open();
        workspace.requestActive();
        return true;
    }

    @Override
    protected void componentOpened() {
        Case.addEventTypeSubscriber(java.util.EnumSet.of(Case.Events.CURRENT_CASE), caseListener);
        IngestManager.getInstance().addIngestJobEventListener(STATUS_INGEST_EVENTS, ingestListener);
        refreshCaseState();
        SwingUtilities.invokeLater(() -> {
            if (isOpened()) {
                dockIntoEditor(WindowManager.getDefault(), this);
                requestActive();
            }
        });
    }

    @Override
    protected void componentClosed() {
        Case.removeEventTypeSubscriber(java.util.EnumSet.of(Case.Events.CURRENT_CASE), caseListener);
        IngestManager.getInstance().removeIngestJobEventListener(STATUS_INGEST_EVENTS, ingestListener);
    }

    private JPanel buildHeader() {
        caseDetails.setForeground(muted());
        caseStatus.setForeground(muted());

        JPanel header = new JPanel();
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(caseName);
        header.add(javax.swing.Box.createVerticalStrut(2));
        header.add(caseDetails);
        header.add(caseStatus);
        return header;
    }

    private static void dockIntoEditor(WindowManager windowManager, TopComponent workspace) {
        Mode editor = windowManager.findMode(EDITOR_MODE);
        if (editor == null) {
            LOGGER.warning("No editor mode to dock the Case Workspace into");
        } else if (windowManager.findMode(workspace) != editor) {
            editor.dockInto(workspace);
        }
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton navigator = new JButton("Open Navigator");
        navigator.addActionListener(e -> showNavigator());
        JButton customize = new JButton("Edit views…");
        customize.addActionListener(e -> StudioDialog.show(StudioDialog.Tab.NAVIGATOR));
        footer.add(navigator);
        footer.add(customize);
        return footer;
    }

    private JPanel createCard(Shortcut shortcut) {
        JPanel card = new WorkflowCard();
        card.setOpaque(true);
        card.setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel(shortcut.title);
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        JPanel actions = new JPanel(new GridLayout(0, actionColumns, 8, 8));
        actions.setOpaque(false);
        for (ViewShortcut view : shortcut.views) {
            javax.swing.Icon icon = ModernIcons.forToolbarLabel(view.label, 16);
            JButton button = icon == null ? new JButton(view.label) : new JButton(view.label, icon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> openView(view));
            actions.add(button);
        }
        cardActions.add(actions);

        card.add(title, BorderLayout.NORTH);
        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private static final class WorkflowCard extends JPanel {

        WorkflowCard() {
            super(new BorderLayout(0, 8));
            setBorder(BorderFactory.createCompoundBorder(
                    new WorkflowBorder(),
                    BorderFactory.createEmptyBorder(10, 12, 12, 12)));
        }
    }

    private static final class WorkflowBorder extends AbstractBorder {

        private static final int CUT = 10;

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(5, 7, 5, 7);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean gothic = com.modernui.autopsy.ModernUISettings.getInstance().getPalette()
                        == com.modernui.autopsy.theme.PalettePreset.GOTHIC_SIGILISM;
                Color accent = accent();
                Color line = borderColor();
                if (!gothic) {
                    g.setColor(line);
                    g.drawRect(x, y, width - 1, height - 1);
                    return;
                }

                int right = x + width - 1;
                int bottom = y + height - 1;
                java.awt.geom.Path2D frame = new java.awt.geom.Path2D.Double();
                frame.moveTo(x + CUT, y);
                frame.lineTo(right - CUT, y);
                frame.lineTo(right, y + CUT);
                frame.lineTo(right, bottom - CUT);
                frame.lineTo(right - CUT, bottom);
                frame.lineTo(x + CUT, bottom);
                frame.lineTo(x, bottom - CUT);
                frame.lineTo(x, y + CUT);
                frame.closePath();
                g.setColor(line);
                g.draw(frame);
                g.setColor(accent);
                g.drawLine(x + 1, y + CUT, x + CUT, y + 1);
                g.drawLine(right - CUT, bottom - 1, right - 1, bottom - CUT);
                g.drawLine(right - CUT - 4, y + 1, right - CUT, y + 5);
                g.drawLine(right - CUT, y + 5, right - CUT + 4, y + 1);
            } finally {
                g.dispose();
            }
        }
    }

    private void updateResponsiveLayout(int availableWidth) {
        int pageInset = availableWidth < 680 ? 14 : (sigilMode ? 32 : 24);
        page.setBorder(BorderFactory.createEmptyBorder(sigilMode ? 22 : 16, pageInset, 16, pageInset));
        int nextCardColumns;
        int nextActionColumns;
        if (sigilMode) {
            // A row of four reads as a row of cathedral windows when there's room.
            nextCardColumns = availableWidth >= 1100 ? 4 : availableWidth >= 620 ? 2 : 1;
            nextActionColumns = 1;
        } else {
            nextCardColumns = availableWidth >= TWO_COLUMN_MIN_WIDTH ? 2 : 1;
            nextActionColumns = availableWidth / nextCardColumns >= TWO_ACTION_COLUMN_MIN_WIDTH ? 2 : 1;
        }
        if (nextCardColumns == cardColumns && nextActionColumns == actionColumns) {
            return;
        }
        cardColumns = nextCardColumns;
        actionColumns = nextActionColumns;
        cards.setLayout(new GridLayout(0, cardColumns, sigilMode ? 18 : 12, sigilMode ? 18 : 12));
        for (JPanel actions : cardActions) {
            actions.setLayout(new GridLayout(0, actionColumns, 8, 8));
        }
        cards.revalidate();
    }

    private static final class ViewportPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(18, visibleRect.height - 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private void openView(ViewShortcut view) {
        if (!Case.isCaseOpen()) {
            caseStatus.setText("Open a case first.");
            return;
        }
        if (!NavigatorTopComponent.openSavedView(view.group, view.name)) {
            caseStatus.setText("\"" + view.name + "\" isn't in your Navigator views any more. Use Edit views… to add it back.");
        }
    }

    private void showNavigator() {
        NavigatorTopComponent.openNavigator();
    }

    private void refreshCaseState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshCaseState);
            return;
        }
        if (!Case.isCaseOpen()) {
            STATUS_SEQUENCE.incrementAndGet();
            caseName.setText(NO_CASE);
            caseStatus.setText(" ");
            caseDetails.setText(" ");
            return;
        }
        final Case current;
        try {
            current = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException ex) {
            caseName.setText(NO_CASE);
            caseStatus.setText(" ");
            caseDetails.setText(" ");
            return;
        }
        caseName.setText(current.getDisplayName());
        caseStatus.setText(" ");
        caseDetails.setText("Loading…");
        long sequence = STATUS_SEQUENCE.incrementAndGet();
        STATUS_WORKER.post(() -> {
            String summary;
            try {
                int sourceCount = current.getDataSources().size();
                IngestManager ingest = IngestManager.getInstance();
                List<IngestJobProgressSnapshot> jobs = ingest.getIngestJobSnapshots();
                boolean running = ingest.isIngestRunning();
                String ingestState = running
                        ? "ingest running (" + jobs.size() + (jobs.size() == 1 ? " job)" : " jobs)")
                        : "ingest idle";
                summary = sourceCount + (sourceCount == 1 ? " data source, " : " data sources, ") + ingestState;
            } catch (org.sleuthkit.datamodel.TskCoreException ex) {
                LOGGER.log(java.util.logging.Level.WARNING, "Could not load the case summary", ex);
                summary = "Couldn't read the data sources (see the log)";
            }
            String result = summary;
            SwingUtilities.invokeLater(() -> {
                if (sequence == STATUS_SEQUENCE.get() && Case.isCaseOpen()) {
                    try {
                        if (Case.getCurrentCaseThrows() == current) {
                            caseDetails.setText(result);
                        }
                    } catch (NoCurrentCaseException ex) {
                        caseDetails.setText(" ");
                    }
                }
            });
        });
    }

    private static Color accent() {
        Color color = UIManager.getColor("Component.focusColor");
        return color != null ? color : new Color(55, 125, 210);
    }

    private static Color muted() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? color : Color.GRAY;
    }

    private static Color borderColor() {
        Color color = UIManager.getColor("Component.borderColor");
        return color != null ? color : new Color(128, 128, 128, 100);
    }

    private static List<Shortcut> createShortcuts() {
        List<Shortcut> result = new ArrayList<>();
        result.add(new Shortcut("Triage",
                new ViewShortcut("Suspicious", "Deleted files", "Deleted files"),
                new ViewShortcut("Review", "Keyword hits", "Keyword hits")));
        result.add(new Shortcut("Files",
                new ViewShortcut("Media", "Images", "Images"),
                new ViewShortcut("Documents", "PDFs", "PDFs")));
        result.add(new Shortcut("User activity",
                new ViewShortcut("User activity", "Web history", "Web history"),
                new ViewShortcut("Communication", "Messages", "Messages")));
        result.add(new Shortcut("Review",
                new ViewShortcut("Review", "Tagged items", "Tagged items"),
                new ViewShortcut("Suspicious", "Malware and YARA hits", "Malware and YARA hits")));
        return result;
    }

    private static final class Shortcut {

        final String title;
        final ViewShortcut[] views;

        Shortcut(String title, ViewShortcut... views) {
            this.title = title;
            this.views = views;
        }
    }

    private static final class ViewShortcut {

        final String group;
        final String name;
        final String label;

        ViewShortcut(String group, String name, String label) {
            this.group = group;
            this.name = name;
            this.label = label;
        }
    }
}

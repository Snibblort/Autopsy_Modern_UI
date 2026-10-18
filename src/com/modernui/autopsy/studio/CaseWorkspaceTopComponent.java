package com.modernui.autopsy.studio;

import com.modernui.autopsy.navigator.NavigatorTopComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.sleuthkit.autopsy.casemodule.Case;

/**
 * A task-oriented start page that organizes common case searches without
 * replacing Autopsy's native result viewers.
 */
@TopComponent.Description(preferredID = CaseWorkspaceTopComponent.ID,
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "com.modernui.autopsy.studio.CaseWorkspace")
@ActionReference(path = "Menu/Window", position = 35)
@TopComponent.OpenActionRegistration(displayName = "Case Workspace", preferredID = CaseWorkspaceTopComponent.ID)
public final class CaseWorkspaceTopComponent extends TopComponent {

    static final String ID = "ModernUICaseWorkspaceTopComponent";

    private static final List<Shortcut> SHORTCUTS = createShortcuts();

    private final JLabel caseName = new JLabel("No case open");
    private final JLabel caseStatus = new JLabel("Open or create a case to begin investigating.");
    private final JPanel cards = new JPanel(new GridLayout(0, 2, 16, 16));
    private final PropertyChangeListener caseListener = evt -> refreshCaseState();

    public CaseWorkspaceTopComponent() {
        setName("Case Workspace");
        setToolTipText("A focused home for common case workflows");
        setLayout(new BorderLayout());

        JPanel page = new JPanel(new BorderLayout(0, 24));
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        page.add(buildHeader(), BorderLayout.NORTH);

        cards.setOpaque(false);
        for (Shortcut shortcut : SHORTCUTS) {
            cards.add(createCard(shortcut));
        }

        JPanel cardHost = new JPanel(new BorderLayout());
        cardHost.setOpaque(false);
        cardHost.add(cards, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(cardHost);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER);
        page.add(buildFooter(), BorderLayout.SOUTH);

        add(page, BorderLayout.CENTER);
        refreshCaseState();
    }

    @Override
    protected void componentOpened() {
        Case.addEventTypeSubscriber(java.util.EnumSet.of(Case.Events.CURRENT_CASE), caseListener);
        refreshCaseState();
        requestActive();
    }

    @Override
    protected void componentClosed() {
        Case.removeEventTypeSubscriber(java.util.EnumSet.of(Case.Events.CURRENT_CASE), caseListener);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        JLabel eyebrow = new JLabel("AUTOPSY  /  CASE WORKSPACE");
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 11f));
        eyebrow.setForeground(accent());

        JLabel title = new JLabel("Start with the evidence.");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));

        caseStatus.setForeground(muted());
        JPanel titles = new JPanel(new BorderLayout(0, 5));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(caseStatus, BorderLayout.CENTER);

        JPanel identity = new JPanel(new BorderLayout(0, 6));
        identity.setOpaque(false);
        identity.add(eyebrow, BorderLayout.NORTH);
        identity.add(titles, BorderLayout.CENTER);

        caseName.setFont(caseName.getFont().deriveFont(Font.BOLD, 14f));
        caseName.setForeground(accent());
        caseName.setHorizontalAlignment(SwingConstants.RIGHT);
        caseName.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        header.add(identity, BorderLayout.CENTER);
        header.add(caseName, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JLabel note = new JLabel("Shortcuts open searches in Autopsy's native result viewers.");
        note.setForeground(muted());

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        JButton navigator = new JButton("Open Navigator");
        navigator.addActionListener(e -> showNavigator());
        JButton customize = new JButton("Customize");
        customize.addActionListener(e -> StudioDialog.show(StudioDialog.Tab.NAVIGATOR));
        actions.add(navigator);
        actions.add(customize);

        footer.add(note, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private JPanel createCard(Shortcut shortcut) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(UIManager.getColor("Panel.background"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        JLabel title = new JLabel(shortcut.title);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        JLabel description = new JLabel(shortcut.description);
        description.setForeground(muted());

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(description, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 2, 8, 8));
        actions.setOpaque(false);
        for (ViewShortcut view : shortcut.views) {
            JButton button = new JButton(view.label + "  →");
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.addActionListener(e -> openView(view));
            actions.add(button);
        }

        card.add(heading, BorderLayout.NORTH);
        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private void openView(ViewShortcut view) {
        if (!Case.isCaseOpen()) {
            caseStatus.setText("Open or create a case before running a saved search.");
            return;
        }
        if (!NavigatorTopComponent.openSavedView(view.group, view.name)) {
            caseStatus.setText("That saved search was removed. Use Customize to restore or replace it.");
        }
    }

    private void showNavigator() {
        NavigatorTopComponent.openNavigator();
    }

    private void refreshCaseState() {
        Runnable update = () -> {
            if (!Case.isCaseOpen()) {
                caseName.setText("No active case");
                caseStatus.setText("Open or create a case to begin investigating.");
                return;
            }
            try {
                Case current = Case.getCurrentCaseThrows();
                caseName.setText(current.getDisplayName());
                caseStatus.setText("Choose a workflow to narrow the case to the evidence you need.");
            } catch (org.sleuthkit.autopsy.casemodule.NoCurrentCaseException ex) {
                caseName.setText("No active case");
                caseStatus.setText("Open or create a case to begin investigating.");
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
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
        result.add(new Shortcut("01  TRIAGE", "Prioritize notable files and high-value findings.",
                new ViewShortcut("Suspicious", "Deleted files", "Deleted files"),
                new ViewShortcut("Review", "Keyword hits", "Keyword hits")));
        result.add(new Shortcut("02  FIND FILES", "Jump straight to common file categories.",
                new ViewShortcut("Media", "Images", "Images"),
                new ViewShortcut("Documents", "PDFs", "PDFs")));
        result.add(new Shortcut("03  USER ACTIVITY", "Review browser, device, and communication artifacts.",
                new ViewShortcut("User activity", "Web history", "Web history"),
                new ViewShortcut("Communication", "Messages", "Messages")));
        result.add(new Shortcut("04  REVIEW", "Return to tagged evidence and items flagged for follow-up.",
                new ViewShortcut("Review", "Tagged items", "Tagged items"),
                new ViewShortcut("Suspicious", "Malware and YARA hits", "Malware and YARA hits")));
        return result;
    }

    private static final class Shortcut {

        final String title;
        final String description;
        final ViewShortcut[] views;

        Shortcut(String title, String description, ViewShortcut... views) {
            this.title = title;
            this.description = description;
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

package com.modernui.autopsy.navigator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataResultViewer;
import org.sleuthkit.autopsy.corecomponents.AbstractDataResultViewer;

/**
 * A redesigned summary tab for views opened from the Studio Navigator.
 */
@ServiceProvider(service = DataResultViewer.class)
public final class NavigatorOverviewViewer extends AbstractDataResultViewer {

    private static final long serialVersionUID = 1L;

    private final JLabel title = new JLabel("Navigator view");
    private final JLabel subtitle = new JLabel(" ");
    private final JLabel items = new JLabel("0", SwingConstants.CENTER);
    private final JLabel group = new JLabel("-", SwingConstants.CENTER);
    private final JLabel resultSet = new JLabel("-", SwingConstants.CENTER);

    public NavigatorOverviewViewer() {
        super(null);
        buildUi();
    }

    @Override
    public DataResultViewer createInstance() {
        return new NavigatorOverviewViewer();
    }

    @Override
    public boolean isSupported(Node node) {
        return node != null && node.getLookup().lookup(NavigatorResultInfo.class) != null;
    }

    @Override
    public void setNode(Node node) {
        NavigatorResultInfo info = node == null ? null : node.getLookup().lookup(NavigatorResultInfo.class);
        if (info == null) {
            resetComponent();
            return;
        }
        title.setText(info.viewName);
        subtitle.setText("Navigator view in " + info.groupName);
        items.setText(Integer.toString(info.itemCount));
        group.setText(info.groupName);
        resultSet.setText(info.truncated ? "Limited" : "Complete");
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public void resetComponent() {
        title.setText("Navigator view");
        subtitle.setText(" ");
        items.setText("0");
        group.setText("-");
        resultSet.setText("-");
    }

    @Override
    public void clearComponent() {
        resetComponent();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setOpaque(false);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 8f));
        subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Items loaded", items, "Rows in this result set"));
        metrics.add(metric("View group", group, "Navigator organization"));
        metrics.add(metric("Result set", resultSet, "Whether the row limit was reached"));

        JLabel hint = new JLabel("The Table and Thumbnail tabs remain available for the same results.");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));

        add(heading, BorderLayout.NORTH);
        add(metrics, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);
    }

    private static JPanel metric(String label, JLabel value, String detail) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        Color line = UIManager.getColor("Component.borderColor");
        if (line == null) {
            line = UIManager.getColor("Label.disabledForeground");
        }
        if (line == null) {
            line = Color.GRAY;
        }
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel heading = new JLabel(label);
        heading.setForeground(UIManager.getColor("Label.disabledForeground"));
        value.setFont(value.getFont().deriveFont(Font.BOLD, value.getFont().getSize2D() + 5f));
        JLabel note = new JLabel(detail);
        note.setForeground(UIManager.getColor("Label.disabledForeground"));

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);
        return card;
    }
}

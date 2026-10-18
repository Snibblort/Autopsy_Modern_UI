package com.modernui.autopsy.navigator;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import com.modernui.autopsy.sigil.Sigil;
import com.modernui.autopsy.sigil.SigilPanel;
import com.modernui.autopsy.sigil.SigilWidgets;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataResultViewer;
import org.sleuthkit.autopsy.corecomponents.AbstractDataResultViewer;

/**
 * "Overview" result tab for views opened from the Navigator: what the view
 * searched for and how many rows came back. Hidden for every other result.
 */
@ServiceProvider(service = DataResultViewer.class)
public final class NavigatorOverviewViewer extends AbstractDataResultViewer {

    private static final long serialVersionUID = 1L;

    private final JLabel title = new SigilWidgets.Title(" ", 30f, 4f);
    private final JLabel group = new JLabel();
    private final JLabel kind = new JLabel();
    private final JLabel criteria = new JLabel();
    private final JLabel rows = new JLabel();

    public NavigatorOverviewViewer() {
        super(null);
        buildUi();
        resetComponent();
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
        group.setText(info.groupName);
        kind.setText(info.queryKind);
        criteria.setText(info.queryParameter.isEmpty() ? "-" : shorten(info.queryParameter, 80));
        criteria.setToolTipText(info.queryParameter.isEmpty() ? null : info.queryParameter);
        rows.setForeground(Sigil.isActive() ? Sigil.secondary() : UIManager.getColor("Label.foreground"));
        String count = info.itemCount + (info.itemCount == 1 ? " row" : " rows")
                + " (" + info.fileCount + " files, " + info.artifactCount + " artifacts)";
        rows.setText(info.truncated ? count + ", stopped at the row limit" : count);
    }

    @Override
    public String getTitle() {
        return "Overview";
    }

    @Override
    public void resetComponent() {
        title.setText(" ");
        group.setText("-");
        kind.setText("-");
        criteria.setText("-");
        criteria.setToolTipText(null);
        rows.setText("-");
    }

    @Override
    public void clearComponent() {
        resetComponent();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "Group", group);
        addField(fields, 1, "Type", kind);
        addField(fields, 2, "Criteria", criteria);
        addField(fields, 3, "Results", rows);

        // With Cyber-Sigilism the fields sit in an engraved frame; otherwise the frame paints nothing.
        SigilPanel frame = new SigilPanel(new BorderLayout(), SigilPanel.Shape2.NOTCHED, 0x0EE1L);
        frame.add(fields, BorderLayout.CENTER);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.CENTER);
        head.add(new SigilWidgets.Divider(), BorderLayout.SOUTH);

        add(head, BorderLayout.NORTH);
        add(frame, BorderLayout.CENTER);
    }

    private static void addField(JPanel panel, int row, String name, JLabel value) {
        JLabel label = new JLabel(name);
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 6, 16);
        panel.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 6, 0);
        panel.add(value, c);
        if (row == 3) {
            GridBagConstraints filler = new GridBagConstraints();
            filler.gridy = row + 1;
            filler.weighty = 1;
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            panel.add(spacer, filler);
        }
    }

    private static String shorten(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit - 1) + "…";
    }
}

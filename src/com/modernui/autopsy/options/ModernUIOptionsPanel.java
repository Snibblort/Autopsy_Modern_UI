package com.modernui.autopsy.options;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.ModernUISettings.Accent;
import com.modernui.autopsy.ModernUISettings.Corners;
import com.modernui.autopsy.ModernUISettings.Density;
import com.modernui.autopsy.ModernUISettings.GridLines;
import com.modernui.autopsy.ModernUISettings.TabStyle;
import com.modernui.autopsy.ModernUISettings.Theme;
import com.modernui.autopsy.theme.PalettePreset;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.openide.LifecycleManager;

/**
 * Tools > Options > Modern UI.
 */
final class ModernUIOptionsPanel extends JPanel {

    private static final String THEME_DEFAULT_FONT = "Theme default";

    /** Modern UI fonts to offer, if installed. */
    private static final List<String> FONT_CANDIDATES = Arrays.asList(
            "Segoe UI Variable Text", "Segoe UI", "Inter", "Roboto", "Open Sans", "Noto Sans",
            "Source Sans 3", "Source Sans Pro", "Lato", "IBM Plex Sans", "Calibri", "Arial", "Verdana");

    private final ModernUIOptionsPanelController controller;

    private final JCheckBox enabledChk = new JCheckBox("Use the modern look (FlatLaf)");
    private final JComboBox<Theme> themeCombo = new JComboBox<>(Theme.values());
    private final JComboBox<PalettePreset> paletteCombo = new JComboBox<>(PalettePreset.values());
    private final JComboBox<Accent> accentCombo = new JComboBox<>(Accent.values());
    private final JComboBox<Corners> cornersCombo = new JComboBox<>(Corners.values());
    private final JComboBox<String> fontCombo = new JComboBox<>();
    private final JComboBox<Integer> fontSizeCombo = new JComboBox<>();
    private final JComboBox<Density> densityCombo = new JComboBox<>(Density.values());
    private final JComboBox<GridLines> gridCombo = new JComboBox<>(GridLines.values());
    private final JComboBox<TabStyle> tabCombo = new JComboBox<>(TabStyle.values());
    private final JCheckBox zebraChk = new JCheckBox("Striped table rows");
    private final JCheckBox titleBarChk = new JCheckBox("Modern title bar (menu inside the title bar)");
    private final JCheckBox fixDarkChk = new JCheckBox("Fix panels with hard-coded white backgrounds (experimental)");
    private final JButton restartBtn = new JButton("Save and restart Autopsy");
    private final JButton resetBtn = new JButton("Reset to defaults");

    private String customAccent = "#2675BF";
    private Accent previousAccent = Accent.DEFAULT;
    private boolean loading;

    ModernUIOptionsPanel(ModernUIOptionsPanelController controller) {
        this.controller = controller;

        fillFontCombos();
        accentCombo.setRenderer(new AccentRenderer());
        accentCombo.setMaximumRowCount(Accent.values().length);
        paletteCombo.setMaximumRowCount(PalettePreset.values().length);
        fontSizeCombo.setRenderer(new FontSizeRenderer());

        buildLayout();

        ActionListener changed = e -> {
            if (!loading) {
                updateEnabledState();
                controller.changed();
            }
        };
        enabledChk.addActionListener(changed);
        themeCombo.addActionListener(changed);
        paletteCombo.addActionListener(changed);
        cornersCombo.addActionListener(changed);
        fontCombo.addActionListener(changed);
        fontSizeCombo.addActionListener(changed);
        densityCombo.addActionListener(changed);
        gridCombo.addActionListener(changed);
        tabCombo.addActionListener(changed);
        zebraChk.addActionListener(changed);
        titleBarChk.addActionListener(changed);
        fixDarkChk.addActionListener(changed);
        accentCombo.addActionListener(e -> onAccentChosen());

        restartBtn.addActionListener(e -> saveAndRestart());
        resetBtn.addActionListener(e -> resetToDefaults());
    }

    private void fillFontCombos() {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        fontCombo.addItem(THEME_DEFAULT_FONT);
        for (String family : FONT_CANDIDATES) {
            if (installed.contains(family)) {
                fontCombo.addItem(family);
            }
        }
        for (int delta = ModernUISettings.MIN_FONT_DELTA; delta <= ModernUISettings.MAX_FONT_DELTA; delta++) {
            fontSizeCombo.addItem(delta);
        }
    }

    private void buildLayout() {
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.insets = new Insets(0, 4, 2, 12);

        JLabel title = new JLabel("Modern UI");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 4f));
        form.add(title, gc);

        gc.gridy++;
        form.add(new JLabel("Choose a shared application theme or a Modern UI palette profile."), gc);

        gc.gridy++;
        gc.insets = new Insets(10, 4, 4, 12);
        form.add(enabledChk, gc);

        addSection(form, gc, "Theme");
        addRow(form, gc, "Design profile:", paletteCombo);
        addRow(form, gc, "Theme:", themeCombo);
        addRow(form, gc, "Accent color:", accentCombo);
        addRow(form, gc, "Corners:", cornersCombo);
        addWide(form, gc, fixDarkChk);

        addSection(form, gc, "Text");
        addRow(form, gc, "Font:", fontCombo);
        addRow(form, gc, "Font size:", fontSizeCombo);

        addSection(form, gc, "Tables and tabs");
        addRow(form, gc, "Row density:", densityCombo);
        addRow(form, gc, "Grid lines:", gridCombo);
        addRow(form, gc, "Tab style:", tabCombo);
        addWide(form, gc, zebraChk);

        addSection(form, gc, "Window");
        addWide(form, gc, titleBarChk);

        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 2;
        gc.insets = new Insets(16, 0, 4, 12);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(restartBtn);
        buttons.add(resetBtn);
        JButton studioBtn = new JButton("Open Modern UI Studio…");
        studioBtn.setToolTipText("Palettes, any UI property, toolbars & menus, workspaces and the navigator");
        studioBtn.addActionListener(e -> com.modernui.autopsy.studio.StudioDialog.show(
                com.modernui.autopsy.studio.StudioDialog.Tab.THEME));
        buttons.add(studioBtn);
        form.add(buttons, gc);

        JPanel leftAligned = new JPanel(new BorderLayout());
        leftAligned.add(form, BorderLayout.WEST);
        JPanel north = new JPanel(new BorderLayout());
        north.add(leftAligned, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(north);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private static void addSection(JPanel form, GridBagConstraints gc, String name) {
        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(14, 4, 2, 12);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel(name);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        header.add(label, BorderLayout.WEST);
        JSeparator line = new JSeparator();
        JPanel lineHolder = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.fill = GridBagConstraints.HORIZONTAL;
        lc.weightx = 1;
        lineHolder.add(line, lc);
        header.add(lineHolder, BorderLayout.CENTER);
        form.add(header, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gc.insets = new Insets(4, 4, 4, 12);
    }

    private static void addRow(JPanel form, GridBagConstraints gc, String label, Component field) {
        gc.gridy++;
        gc.gridx = 0;
        gc.gridwidth = 1;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    private static void addWide(JPanel form, GridBagConstraints gc, Component field) {
        gc.gridy++;
        gc.gridx = 1;
        gc.gridwidth = 1;
        form.add(field, gc);
    }

    private void onAccentChosen() {
        if (loading) {
            return;
        }
        Accent chosen = (Accent) accentCombo.getSelectedItem();
        if (chosen == Accent.CUSTOM) {
            Color picked = JColorChooser.showDialog(this, "Choose an accent color", Color.decode(customAccent));
            if (picked == null) {
                loading = true;
                try {
                    accentCombo.setSelectedItem(previousAccent);
                } finally {
                    loading = false;
                }
                return;
            }
            customAccent = ModernUISettings.toHex(picked);
            accentCombo.repaint();
        }
        previousAccent = (Accent) accentCombo.getSelectedItem();
        controller.changed();
    }

    private void updateEnabledState() {
        boolean on = enabledChk.isSelected();
        Theme theme = (Theme) themeCombo.getSelectedItem();
        PalettePreset palette = (PalettePreset) paletteCombo.getSelectedItem();
        themeCombo.setEnabled(on);
        if (palette != null && palette != PalettePreset.NONE) {
            themeCombo.setEnabled(false);
            theme = palette.getBase();
        }
        paletteCombo.setEnabled(on);
        accentCombo.setEnabled(on);
        cornersCombo.setEnabled(on);
        fontCombo.setEnabled(on);
        fontSizeCombo.setEnabled(on);
        densityCombo.setEnabled(on);
        gridCombo.setEnabled(on);
        tabCombo.setEnabled(on);
        zebraChk.setEnabled(on);
        titleBarChk.setEnabled(on);
        fixDarkChk.setEnabled(on && theme != null && theme.isDark());
    }

    void load() {
        loading = true;
        try {
            ModernUISettings s = ModernUISettings.getInstance();
            enabledChk.setSelected(s.isEnabled());
            themeCombo.setSelectedItem(s.getTheme());
            paletteCombo.setSelectedItem(s.getPalette());
            customAccent = s.getCustomAccent();
            accentCombo.setSelectedItem(s.getAccent());
            previousAccent = s.getAccent();
            cornersCombo.setSelectedItem(s.getCorners());
            selectFont(s.getFontFamily());
            fontSizeCombo.setSelectedItem(s.getFontSizeDelta());
            densityCombo.setSelectedItem(s.getDensity());
            gridCombo.setSelectedItem(s.getGridLines());
            tabCombo.setSelectedItem(s.getTabStyle());
            zebraChk.setSelected(s.isZebraRows());
            titleBarChk.setSelected(s.isModernTitleBar());
            fixDarkChk.setSelected(s.isFixDarkColors());
            updateEnabledState();
        } finally {
            loading = false;
        }
    }

    private void selectFont(String family) {
        if (family == null || family.isEmpty()) {
            fontCombo.setSelectedItem(THEME_DEFAULT_FONT);
            return;
        }
        boolean listed = false;
        for (int i = 0; i < fontCombo.getItemCount(); i++) {
            if (family.equals(fontCombo.getItemAt(i))) {
                listed = true;
                break;
            }
        }
        if (!listed) {
            fontCombo.addItem(family);
        }
        fontCombo.setSelectedItem(family);
    }

    private String selectedFontFamily() {
        Object item = fontCombo.getSelectedItem();
        return item == null || THEME_DEFAULT_FONT.equals(item) ? "" : item.toString();
    }

    private int selectedFontDelta() {
        Object item = fontSizeCombo.getSelectedItem();
        return item instanceof Integer ? (Integer) item : 0;
    }

    void store() {
        ModernUISettings s = ModernUISettings.getInstance();
        s.setEnabled(enabledChk.isSelected());
        s.setTheme((Theme) themeCombo.getSelectedItem());
        s.setPalette((PalettePreset) paletteCombo.getSelectedItem());
        s.setAccent((Accent) accentCombo.getSelectedItem());
        s.setCustomAccent(customAccent);
        s.setCorners((Corners) cornersCombo.getSelectedItem());
        s.setFontFamily(selectedFontFamily());
        s.setFontSizeDelta(selectedFontDelta());
        s.setDensity((Density) densityCombo.getSelectedItem());
        s.setGridLines((GridLines) gridCombo.getSelectedItem());
        s.setTabStyle((TabStyle) tabCombo.getSelectedItem());
        s.setZebraRows(zebraChk.isSelected());
        s.setModernTitleBar(titleBarChk.isSelected());
        s.setFixDarkColors(fixDarkChk.isSelected());
    }

    boolean isChanged() {
        ModernUISettings s = ModernUISettings.getInstance();
        return enabledChk.isSelected() != s.isEnabled()
                || themeCombo.getSelectedItem() != s.getTheme()
                || paletteCombo.getSelectedItem() != s.getPalette()
                || accentCombo.getSelectedItem() != s.getAccent()
                || (accentCombo.getSelectedItem() == Accent.CUSTOM && !customAccent.equalsIgnoreCase(s.getCustomAccent()))
                || cornersCombo.getSelectedItem() != s.getCorners()
                || !selectedFontFamily().equals(s.getFontFamily())
                || selectedFontDelta() != s.getFontSizeDelta()
                || densityCombo.getSelectedItem() != s.getDensity()
                || gridCombo.getSelectedItem() != s.getGridLines()
                || tabCombo.getSelectedItem() != s.getTabStyle()
                || zebraChk.isSelected() != s.isZebraRows()
                || titleBarChk.isSelected() != s.isModernTitleBar()
                || fixDarkChk.isSelected() != s.isFixDarkColors();
    }

    private void resetToDefaults() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Reset all Modern UI settings to their defaults?",
                "Modern UI", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        ModernUISettings.getInstance().resetToDefaults();
        load();
        controller.changed();
    }

    private void saveAndRestart() {
        store();
        controller.changed();
        try {
            LifecycleManager.getDefault().markForRestart();
        } catch (UnsupportedOperationException ex) {
            JOptionPane.showMessageDialog(this,
                    "Settings saved. Close and reopen Autopsy to apply them.",
                    "Modern UI", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> LifecycleManager.getDefault().exit());
    }

    /** Shows a small color swatch next to each accent name. */
    private final class AccentRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Accent) {
                Accent accent = (Accent) value;
                String hex = accent == Accent.CUSTOM ? customAccent : accent.getHex();
                setIcon(hex == null ? null : new Swatch(Color.decode(hex)));
                if (accent == Accent.CUSTOM && index < 0) {
                    setText("Custom (" + customAccent + ")");
                }
            }
            return this;
        }
    }

    /** Shows font size changes as "Default", "+1 pt", "-1 pt". */
    private static final class FontSizeRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Integer) {
                int delta = (Integer) value;
                setText(delta == 0 ? "Default" : (delta > 0 ? "+" : "") + delta + " pt");
            }
            return this;
        }
    }

    private static final class Swatch implements Icon {

        private final Color color;

        Swatch(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, getIconWidth(), getIconHeight());
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}

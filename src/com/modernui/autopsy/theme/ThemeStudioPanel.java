package com.modernui.autopsy.theme;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.ModernUISettings.Theme;
import com.modernui.autopsy.ThemeApplier;
import com.modernui.autopsy.studio.StudioUi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Theme Studio: pick a base theme or community palette, then override any
 * FlatLaf property (colors, sizes, fonts, arcs) and apply it.
 */
public final class ThemeStudioPanel extends JPanel {

    /** Shown first in the property browser: the keys people change most. */
    static final String[] SUGGESTED_KEYS = {
        "@background", "@foreground", "@accentColor", "@componentBackground", "@menuBackground",
        "@buttonBackground", "@selectionBackground", "@selectionForeground",
        "Tree.background", "Tree.selectionBackground", "Tree.rowHeight",
        "Table.background", "Table.alternateRowColor", "Table.selectionBackground", "Table.gridColor", "Table.rowHeight",
        "TableHeader.background", "TableHeader.foreground", "TableHeader.separatorColor", "TableHeader.height",
        "TabbedPane.underlineColor", "TabbedPane.selectedBackground", "TabbedPane.tabHeight", "TabbedPane.hoverColor",
        "ScrollBar.thumb", "ScrollBar.track", "ScrollBar.width", "ScrollBar.thumbArc",
        "Component.focusColor", "Component.borderColor", "Component.arc", "Button.arc",
        "TitlePane.background", "MenuBar.background", "ToolBar.background", "Panel.background",
        "TextArea.background", "TextArea.foreground", "Label.foreground", "defaultFont"
    };

    /** Where to read the current value of a theme variable (variables aren't in UIManager). */
    static Object currentValue(String key) {
        switch (key) {
            case "@background":
                return UIManager.get("Panel.background");
            case "@foreground":
                return UIManager.get("Label.foreground");
            case "@accentColor":
                return UIManager.get("Component.focusColor");
            case "@componentBackground":
                return UIManager.get("TextField.background");
            case "@menuBackground":
                return UIManager.get("MenuBar.background");
            case "@buttonBackground":
                return UIManager.get("Button.background");
            case "@selectionBackground":
                return UIManager.get("List.selectionBackground");
            case "@selectionForeground":
                return UIManager.get("List.selectionForeground");
            default:
                return UIManager.get(key);
        }
    }

    private final JComboBox<Theme> baseCombo = new JComboBox<>(Theme.values());
    private final JComboBox<PalettePreset> paletteCombo = new JComboBox<>(PalettePreset.values());
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Property", "Value"}, 0);
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");

    public ThemeStudioPanel() {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        paletteCombo.setRenderer(new PaletteRenderer());
        paletteCombo.setMaximumRowCount(PalettePreset.values().length);
        paletteCombo.addActionListener(e -> baseCombo.setEnabled(paletteCombo.getSelectedItem() == PalettePreset.NONE));

        JPanel top = StudioUi.form();
        StudioUi.row(top, "Base theme:", baseCombo);
        StudioUi.row(top, "Palette:", paletteCombo);
        StudioUi.hint(top, "A palette sets its own light or dark base. Your overrides below are applied on top.");

        table.setRowHeight(Math.max(table.getRowHeight(), 24));
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setCellRenderer(new ValueRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JPanel overrides = new JPanel(new BorderLayout(0, 6));
        overrides.add(StudioUi.section("Overrides"), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 260));
        overrides.add(scroll, BorderLayout.CENTER);

        JPanel rowButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rowButtons.add(StudioUi.button("Add property…", this::addProperty));
        rowButtons.add(StudioUi.button("Pick color…", this::pickColor));
        rowButtons.add(StudioUi.button("Remove", this::removeRow));
        rowButtons.add(StudioUi.button("Clear all", () -> model.setRowCount(0)));
        rowButtons.add(StudioUi.button("Import theme…", this::importTheme));
        rowButtons.add(StudioUi.button("Export theme…", this::exportTheme));
        overrides.add(rowButtons, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel apply = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        apply.add(StudioUi.button("Save", () -> save(true)));
        apply.add(StudioUi.button("Apply now", this::applyNow));
        apply.add(StudioUi.button("Save and restart Autopsy", () -> {
            save(false);
            StudioUi.restartAutopsy(this);
        }));
        bottom.add(apply, BorderLayout.NORTH);
        status.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 0));
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(overrides, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        load();
    }

    public void load() {
        ModernUISettings s = ModernUISettings.getInstance();
        baseCombo.setSelectedItem(s.getTheme());
        paletteCombo.setSelectedItem(s.getPalette());
        baseCombo.setEnabled(s.getPalette() == PalettePreset.NONE);
        model.setRowCount(0);
        for (Map.Entry<String, String> e : ThemeOverrides.load().entrySet()) {
            model.addRow(new Object[]{e.getKey(), e.getValue()});
        }
    }

    Map<String, String> tableValues() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String key = String.valueOf(model.getValueAt(i, 0)).trim();
            String value = String.valueOf(model.getValueAt(i, 1)).trim();
            if (!key.isEmpty() && !value.isEmpty() && !"null".equals(value)) {
                map.put(key, value);
            }
        }
        return map;
    }

    private void save(boolean announce) {
        ModernUISettings s = ModernUISettings.getInstance();
        s.setTheme((Theme) baseCombo.getSelectedItem());
        s.setPalette((PalettePreset) paletteCombo.getSelectedItem());
        boolean ok = ThemeOverrides.save(tableValues());
        if (announce) {
            status.setText(ok ? "Saved. Restart Autopsy (or Apply now) to see it." : "Could not save the overrides; see the log.");
        }
    }

    private void applyNow() {
        save(false);
        try {
            ThemeApplier.applyLive();
            SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this) != null
                    ? SwingUtilities.getWindowAncestor(this) : this);
            status.setText("Applied. If anything looks off, use Save and restart.");
        } catch (IllegalStateException ex) {
            status.setText(ex.getMessage());
        }
    }

    private void addProperty() {
        String[] picked = PropertyBrowser.choose(this);
        if (picked == null) {
            return;
        }
        for (int i = 0; i < model.getRowCount(); i++) {
            if (picked[0].equals(model.getValueAt(i, 0))) {
                table.setRowSelectionInterval(i, i);
                table.editCellAt(i, 1);
                return;
            }
        }
        model.addRow(new Object[]{picked[0], picked[1]});
        int row = model.getRowCount() - 1;
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    private void pickColor() {
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a row first.");
            return;
        }
        Color current = parseColor(String.valueOf(model.getValueAt(row, 1)));
        Color picked = JColorChooser.showDialog(this, "Pick a color for " + model.getValueAt(row, 0),
                current != null ? current : Color.GRAY);
        if (picked != null) {
            model.setValueAt(ModernUISettings.toHex(picked), row, 1);
        }
    }

    private void removeRow() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            model.removeRow(row);
        }
    }

    private void importTheme() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Theme files (*.properties)", "properties"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String text = new String(Files.readAllBytes(chooser.getSelectedFile().toPath()), StandardCharsets.UTF_8);
            Map<String, String> values = ThemeOverrides.parse(text);
            String base = ThemeOverrides.header(text, "base");
            String palette = ThemeOverrides.header(text, "palette");
            for (Theme t : Theme.values()) {
                if (t.name().equalsIgnoreCase(base) || t.toString().equalsIgnoreCase(base)) {
                    baseCombo.setSelectedItem(t);
                }
            }
            paletteCombo.setSelectedItem(PalettePreset.NONE);
            for (PalettePreset p : PalettePreset.values()) {
                if (p.name().equalsIgnoreCase(palette)) {
                    paletteCombo.setSelectedItem(p);
                }
            }
            model.setRowCount(0);
            for (Map.Entry<String, String> e : values.entrySet()) {
                model.addRow(new Object[]{e.getKey(), e.getValue()});
            }
            status.setText("Imported " + values.size() + " properties. Save or Apply now to use them.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read the file: " + ex.getMessage(), "Import theme",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportTheme() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("my-autopsy-theme.properties"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".properties")) {
            file = new File(file.getParentFile(), file.getName() + ".properties");
        }
        PalettePreset palette = (PalettePreset) paletteCombo.getSelectedItem();
        String text = ThemeOverrides.format(tableValues(), ((Theme) baseCombo.getSelectedItem()).name(),
                palette == PalettePreset.NONE ? null : palette.name());
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            status.setText("Exported to " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not write the file: " + ex.getMessage(), "Export theme",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static Color parseColor(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.matches("#[0-9A-Fa-f]{6}")) {
                return Color.decode(v);
            }
            if (v.matches("#[0-9A-Fa-f]{8}")) {
                long rgba = Long.parseLong(v.substring(1), 16);
                return new Color((int) (rgba >> 24) & 0xFF, (int) (rgba >> 16) & 0xFF,
                        (int) (rgba >> 8) & 0xFF, (int) rgba & 0xFF);
            }
            if (v.matches("#[0-9A-Fa-f]{3}")) {
                return Color.decode("#" + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2) + v.charAt(3) + v.charAt(3));
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    /** Formats a UIManager value in FlatLaf properties syntax. */
    static String formatValue(Object value) {
        if (value instanceof Color) {
            Color c = (Color) value;
            return c.getAlpha() == 255 ? ModernUISettings.toHex(c)
                    : String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }
        if (value instanceof Insets) {
            Insets i = (Insets) value;
            return i.top + "," + i.left + "," + i.bottom + "," + i.right;
        }
        if (value instanceof Dimension) {
            Dimension d = (Dimension) value;
            return d.width + "," + d.height;
        }
        if (value instanceof Font) {
            Font f = (Font) value;
            String style = f.isBold() && f.isItalic() ? "bold italic " : f.isBold() ? "bold " : f.isItalic() ? "italic " : "";
            return style + f.getSize() + " \"" + f.getFamily() + "\"";
        }
        return value == null ? "" : String.valueOf(value);
    }

    /** Shows a color swatch for color values. */
    private static final class ValueRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color c = parseColor(value == null ? null : value.toString());
            setIcon(c == null ? null : StudioUi.swatch(c));
            return this;
        }
    }

    private static final class PaletteRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PalettePreset && value != PalettePreset.NONE) {
                PalettePreset p = (PalettePreset) value;
                setIcon(StudioUi.twoToneSwatch(Color.decode(p.getBackgroundHex()), Color.decode(p.getAccentHex())));
            } else {
                setIcon(null);
            }
            return this;
        }
    }

    /** Searchable list of every property the current theme defines. */
    static final class PropertyBrowser {

        private PropertyBrowser() {
        }

        /** Returns {key, formattedCurrentValue} or null if cancelled. */
        static String[] choose(JComponent parent) {
            List<String> keys = new ArrayList<>();
            for (String k : SUGGESTED_KEYS) {
                keys.add(k);
            }
            TreeSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (Object k : UIManager.getLookAndFeelDefaults().keySet()) {
                if (k instanceof String) {
                    Object v = UIManager.get(k);
                    if (v instanceof Color || v instanceof Integer || v instanceof Insets
                            || v instanceof Dimension || v instanceof Font || v instanceof Boolean) {
                        all.add((String) k);
                    }
                }
            }
            all.removeAll(keys);
            keys.addAll(all);

            DefaultListModel<String> listModel = new DefaultListModel<>();
            keys.forEach(listModel::addElement);
            JList<String> list = new JList<>(listModel);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                    Object current = value == null ? null : currentValue(value.toString());
                    String shown = current == null ? "" : "    " + formatValue(current);
                    setText(value + shown);
                    setIcon(current instanceof Color ? StudioUi.swatch((Color) current) : null);
                    return this;
                }
            });
            JTextField search = new JTextField();
            search.putClientProperty("JTextField.placeholderText", "Search properties (e.g. table, tab, scroll)…");
            search.getDocument().addDocumentListener(new DocumentListener() {
                private void filter() {
                    String q = search.getText().trim().toLowerCase(Locale.ROOT);
                    listModel.clear();
                    for (String k : keys) {
                        if (q.isEmpty() || k.toLowerCase(Locale.ROOT).contains(q)) {
                            listModel.addElement(k);
                        }
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filter();
                }
            });
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.add(search, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(520, 380));
            panel.add(scroll, BorderLayout.CENTER);
            JLabel tip = new JLabel("@ names are theme variables: changing one recolors everything derived from it.");
            tip.setFont(tip.getFont().deriveFont(Font.ITALIC));
            panel.add(tip, BorderLayout.SOUTH);

            int answer = JOptionPane.showConfirmDialog(parent, panel, "Add a property",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            String key = list.getSelectedValue();
            if (answer != JOptionPane.OK_OPTION || key == null) {
                return null;
            }
            Object current = currentValue(key);
            String value = current != null ? formatValue(current) : "";
            return new String[]{key, value};
        }
    }
}

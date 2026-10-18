package com.modernui.autopsy.sigil;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.ModernUISettings.Density;
import com.modernui.autopsy.ModernUISettings.SigilDetail;
import com.modernui.autopsy.ModernUISettings.SigilFont;
import com.modernui.autopsy.ModernUISettings.SigilPresentation;
import com.modernui.autopsy.ThemeApplier;
import com.modernui.autopsy.studio.StudioUi;
import com.modernui.autopsy.theme.PalettePreset;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

/**
 * Studio tab for Cyber-Sigilism. Changes are saved as they're made; the
 * custom-painted parts (glow, ornament, motion, type) follow immediately,
 * colors follow on "Apply now".
 */
public final class SigilStudioPanel extends JPanel {

    private final ModernUISettings s = ModernUISettings.getInstance();
    private final JCheckBox enabled = new JCheckBox("Use Cyber-Sigilism");
    private final JComboBox<SigilPresentation> presentation = new JComboBox<>(SigilPresentation.values());
    private final JComboBox<SigilDetail> detail = new JComboBox<>(SigilDetail.values());
    private final JSlider glow = new JSlider(0, 100);
    private final JLabel glowValue = new JLabel();
    private final JCheckBox motion = new JCheckBox("Animate (turn off for reduced motion)");
    private final JComboBox<SigilFont> font = new JComboBox<>(SigilFont.values());
    private final JComboBox<Density> density = new JComboBox<>(Density.values());
    private final JButton primary = new JButton();
    private final JButton secondary = new JButton();
    private final JLabel status = new JLabel(" ");
    private final Preview preview = new Preview();
    private boolean loading;

    public SigilStudioPanel() {
        super(new BorderLayout(16, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JPanel form = StudioUi.form();
        StudioUi.wide(form, StudioUi.section("Cyber-Sigilism"));
        StudioUi.hint(form, "Black cathedral surfaces, ultraviolet light, engraved geometry.");
        StudioUi.wide(form, enabled);
        StudioUi.row(form, "Presentation:", presentation);
        StudioUi.row(form, "Ornament:", detail);
        JPanel glowRow = new JPanel(new BorderLayout(8, 0));
        glowRow.setOpaque(false);
        glowRow.add(glow, BorderLayout.CENTER);
        glowRow.add(glowValue, BorderLayout.EAST);
        StudioUi.row(form, "Glow:", glowRow);
        StudioUi.wide(form, motion);
        StudioUi.row(form, "Display type:", font);
        StudioUi.row(form, "Density:", density);
        StudioUi.row(form, "Primary light:", colorRow(primary, true));
        StudioUi.row(form, "Secondary light:", colorRow(secondary, false));
        StudioUi.hint(form, "Display type is used for headings only, never for file names or data.");

        StudioUi.wide(form, StudioUi.section("Apply"));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(StudioUi.button("Apply now", this::applyNow));
        buttons.add(javax.swing.Box.createHorizontalStrut(8));
        buttons.add(StudioUi.button("Save and restart", () -> StudioUi.restartAutopsy(this)));
        buttons.add(javax.swing.Box.createHorizontalStrut(8));
        buttons.add(StudioUi.button("Defaults", this::defaults));
        StudioUi.wide(form, buttons);
        StudioUi.wide(form, status);

        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(left);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(450, 10));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.WEST);
        add(preview, BorderLayout.CENTER);

        load();
        enabled.addActionListener(e -> save());
        presentation.addActionListener(e -> save());
        detail.addActionListener(e -> save());
        glow.addChangeListener(e -> save());
        motion.addActionListener(e -> save());
        font.addActionListener(e -> save());
        density.addActionListener(e -> save());
        primary.addActionListener(e -> pick(true));
        secondary.addActionListener(e -> pick(false));
    }

    private JPanel colorRow(JButton button, boolean isPrimary) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(button);
        row.add(javax.swing.Box.createHorizontalStrut(6));
        row.add(StudioUi.button("Reset", () -> {
            if (isPrimary) {
                s.setSigilPrimary(ModernUISettings.DEFAULT_SIGIL_PRIMARY);
            } else {
                s.setSigilSecondary(ModernUISettings.DEFAULT_SIGIL_SECONDARY);
            }
            load();
            status.setText("Saved. Apply now to recolor everything.");
        }));
        return row;
    }

    private void load() {
        loading = true;
        try {
            enabled.setSelected(s.getPalette() == PalettePreset.GOTHIC_SIGILISM);
            presentation.setSelectedItem(s.getSigilPresentation());
            detail.setSelectedItem(s.getSigilDetail());
            glow.setValue(s.getSigilGlow());
            glowValue.setText(s.getSigilGlow() + "%");
            motion.setSelected(s.isSigilAnimation());
            font.setSelectedItem(s.getSigilFont());
            density.setSelectedItem(s.getDensity());
            primary.setText(s.getSigilPrimary());
            primary.setIcon(StudioUi.swatch(Color.decode(s.getSigilPrimary())));
            secondary.setText(s.getSigilSecondary());
            secondary.setIcon(StudioUi.swatch(Color.decode(s.getSigilSecondary())));
            boolean on = enabled.isSelected();
            for (JComponent c : new JComponent[]{presentation, detail, glow, motion, font, primary, secondary}) {
                c.setEnabled(on);
            }
        } finally {
            loading = false;
        }
        preview.repaint();
    }

    private void save() {
        if (loading) {
            return;
        }
        boolean wasOn = s.getPalette() == PalettePreset.GOTHIC_SIGILISM;
        if (enabled.isSelected() != wasOn) {
            s.setPalette(enabled.isSelected() ? PalettePreset.GOTHIC_SIGILISM : PalettePreset.NONE);
            if (enabled.isSelected()) {
                s.setEnabled(true);
            }
        }
        s.setSigilPresentation((SigilPresentation) presentation.getSelectedItem());
        s.setSigilDetail((SigilDetail) detail.getSelectedItem());
        s.setSigilGlow(glow.getValue());
        s.setSigilAnimation(motion.isSelected());
        s.setSigilFont((SigilFont) font.getSelectedItem());
        s.setDensity((Density) density.getSelectedItem());
        glowValue.setText(glow.getValue() + "%");
        SigilClock.update();
        load();
        repaintAllWindows();
        status.setText(enabled.isSelected() != wasOn
                ? "Saved. Apply now (or restart) to switch the whole theme."
                : "Saved. Light, ornament, motion and type update now; Apply now for colors.");
    }

    private void pick(boolean isPrimary) {
        Color current = Color.decode(isPrimary ? s.getSigilPrimary() : s.getSigilSecondary());
        Color chosen = JColorChooser.showDialog(this, isPrimary ? "Primary light" : "Secondary light", current);
        if (chosen == null) {
            return;
        }
        if (isPrimary) {
            s.setSigilPrimary(ModernUISettings.toHex(chosen));
        } else {
            s.setSigilSecondary(ModernUISettings.toHex(chosen));
        }
        load();
        repaintAllWindows();
        status.setText("Saved. Apply now to recolor everything.");
    }

    private void defaults() {
        s.setPalette(PalettePreset.GOTHIC_SIGILISM);
        s.setEnabled(true);
        s.setSigilPresentation(SigilPresentation.FULL);
        s.setSigilDetail(SigilDetail.HIGH);
        s.setSigilGlow(65);
        s.setSigilAnimation(true);
        s.setSigilFont(SigilFont.PIRATA);
        s.setSigilPrimary(ModernUISettings.DEFAULT_SIGIL_PRIMARY);
        s.setSigilSecondary(ModernUISettings.DEFAULT_SIGIL_SECONDARY);
        load();
        applyNow();
    }

    private void applyNow() {
        try {
            ThemeApplier.applyLive();
            status.setText("Applied. If anything looks off, use Save and restart.");
        } catch (IllegalStateException ex) {
            status.setText(ex.getMessage());
        }
        load();
    }

    private static void repaintAllWindows() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            w.repaint();
        }
    }

    /** Live preview of the ornament, type and light settings. */
    private static final class Preview extends JComponent {

        Preview() {
            SigilClock.attach(this);
            setPreferredSize(new Dimension(420, 460));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (!Sigil.isActive()) {
                    g2.setColor(getBackground());
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(StudioUi.muted());
                    g2.drawString("Cyber-Sigilism is off.", 20, 30);
                    return;
                }
                Shape frame = SigilArt.clipped(0.5, 0.5, w - 1, h - 1, 14);
                g2.setPaint(new GradientPaint(0, 0, Sigil.VOID, 0, h, Sigil.BACKGROUND));
                g2.fill(frame);
                float glowV = Sigil.glow();
                if (Sigil.atLeast(SigilDetail.HIGH)) {
                    SigilArt.roseWindow(g2, w - 70, 80, 120, SigilClock.phase(120), Sigil.alpha(Sigil.primary(), 0.16f), 12);
                }
                // wordmark
                java.awt.Font f = Sigil.display(34);
                TextLayout tl = new TextLayout("Autopsy", f, g2.getFontRenderContext());
                SigilArt.glowFill(g2, tl.getOutline(AffineTransform.getTranslateInstance(28, 64)),
                        Sigil.isFull() ? Sigil.lavender() : Sigil.TEXT, glowV);
                if (Sigil.atLeast(SigilDetail.MEDIUM)) {
                    SigilArt.divider(g2, 24, 84, w - 48, Sigil.alpha(Sigil.primary(), 0.85f), glowV);
                }
                // arch window card
                int cw = Math.min(200, w - 60);
                int cx = 28;
                int cy = 108;
                int ch = h - cy - 74;
                Shape arch = SigilArt.archTop(cx + 0.5, cy + 0.5, cw, ch, Sigil.isFull() ? 34 : 14);
                g2.setPaint(new GradientPaint(0, cy, Sigil.ELEVATED, 0, cy + ch, Sigil.BACKGROUND));
                g2.fill(arch);
                if (Sigil.atLeast(SigilDetail.HIGH)) {
                    SigilArt.hatch(g2, arch, Sigil.alpha(Sigil.primary(), 0.05f), 9);
                }
                SigilArt.glowStroke(g2, arch, Sigil.primary(), 1f, glowV);
                if (Sigil.isFull()) {
                    if (Sigil.atLeast(SigilDetail.HIGH)) {
                        SigilArt.roseWindow(g2, cx + cw / 2.0, cy + 70, 34, SigilClock.phase(240),
                                Sigil.alpha(Sigil.primary(), 0.25f), 8);
                    }
                    SigilArt.sigil(g2, cx + cw / 2.0, cy + 70, 54, 0x5EED0L, Sigil.lavender(), glowV);
                }
                if (Sigil.atLeast(SigilDetail.MEDIUM)) {
                    SigilArt.corners(g2, new Rectangle2D.Double(cx, cy + 34, cw, ch - 34), 4, 14, Sigil.lavender(), glowV * 0.5f);
                }
                if (Sigil.animate()) {
                    SigilArt.trace(g2, arch, SigilClock.phase(3.2), 0.12, Sigil.lavender(), glowV);
                }
                java.awt.Font tf = Sigil.display(24);
                TextLayout t2 = new TextLayout("Triage", tf, g2.getFontRenderContext());
                double tw = t2.getBounds().getWidth();
                SigilArt.glowFill(g2, t2.getOutline(AffineTransform.getTranslateInstance(cx + (cw - tw) / 2, cy + 140)),
                        Sigil.isFull() ? Sigil.lavender() : Sigil.TEXT, glowV);
                // sample sigils on the right
                int sx = cx + cw + 30;
                if (sx + 60 < w) {
                    for (int i = 0; i < 3; i++) {
                        SigilArt.sigil(g2, sx + 36, cy + 50 + i * 92, 64, 0xB00L + i * 31L,
                                i == 1 ? Sigil.secondary() : Sigil.primary(), glowV);
                    }
                }
                // gauge
                int gy = h - 46;
                int gw = w - 56;
                Shape track = SigilArt.clipped(28, gy, gw, 10, 4);
                g2.setColor(Sigil.ELEVATED);
                g2.fill(track);
                double amount = Sigil.animate() ? 0.25 + 0.6 * (0.5 + 0.5 * Math.sin(SigilClock.seconds() * 0.7)) : 0.62;
                Graphics2D clip = (Graphics2D) g2.create();
                clip.clip(track);
                clip.setPaint(new GradientPaint(28, 0, Sigil.DEEP_VIOLET, (float) (28 + gw * amount), 0, Sigil.primary()));
                clip.fillRect(28, gy, (int) (gw * amount), 10);
                clip.dispose();
                g2.setColor(Sigil.BORDER);
                g2.draw(track);
                SigilArt.glowStroke(g2, new java.awt.geom.Line2D.Double(28 + gw * amount, gy, 28 + gw * amount, gy + 9),
                        Sigil.lavender(), 1f, glowV);
                g2.setColor(Sigil.TEXT_SECONDARY);
                g2.drawString("INGEST", 28, gy - 8);
                g2.setColor(Sigil.secondary());
                String pct = Math.round(amount * 100) + "%";
                g2.drawString(pct, 28 + gw - g2.getFontMetrics().stringWidth(pct), gy - 8);
                SigilArt.glowStroke(g2, frame, Sigil.BORDER, 1f, 0);
            } finally {
                g2.dispose();
            }
        }
    }
}

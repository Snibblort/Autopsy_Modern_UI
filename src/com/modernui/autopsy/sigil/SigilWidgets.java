package com.modernui.autopsy.sigil;

import com.modernui.autopsy.ModernUISettings.SigilDetail;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** Small reusable Cyber-Sigilism components. Each one falls back to a plain look when the theme is off. */
public final class SigilWidgets {

    private SigilWidgets() {
    }

    /**
     * Page background: void-to-panel gradient, a slowly turning rose window
     * and a vignette. Plain panel background when the theme is off.
     */
    public static class Backdrop extends JPanel implements SigilClock.Animated {

        private int ticks;

        public Backdrop(LayoutManager layout) {
            super(layout);
            SigilClock.attach(this);
        }

        @Override
        public void tick() {
            // The window turns slowly; 4 repaints a second is enough and keeps CPU low.
            if (Sigil.atLeast(SigilDetail.HIGH) && (++ticks % 5) == 0) {
                int r = roseRadius();
                repaint(getWidth() - r * 2 - 40, -r, r * 3, r * 3);
            }
        }

        private int roseRadius() {
            return Math.max(120, Math.min(260, getHeight() / 3));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.isActive()) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, Sigil.VOID, 0, h, Sigil.BACKGROUND));
                g2.fillRect(0, 0, w, h);
                if (Sigil.atLeast(SigilDetail.HIGH)) {
                    int r = roseRadius();
                    double cx = w - r * 0.75;
                    double cy = r * 0.55;
                    g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, r * 1.4f, new float[]{0f, 1f},
                            new Color[]{Sigil.alpha(Sigil.primary(), 0.10f * (0.4f + Sigil.glow())), Sigil.alpha(Sigil.VOID, 0f)}));
                    g2.fillRect(0, 0, w, h);
                    SigilArt.roseWindow(g2, cx, cy, r, SigilClock.phase(360), Sigil.alpha(Sigil.primary(), 0.13f), 12);
                    SigilArt.roseWindow(g2, cx, cy, r * 0.55, -SigilClock.phase(240), Sigil.alpha(Sigil.lavender(), 0.10f), 8);
                }
                if (Sigil.atLeast(SigilDetail.MEDIUM)) {
                    // engraved vertical ribs, like vaulting
                    g2.setColor(Sigil.alpha(Sigil.primary(), 0.045f));
                    for (int x = 24; x < w; x += 96) {
                        g2.drawLine(x, 0, x, h);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Heading in the display typeface, lit with a soft halo. Falls back to a
     * bold UI-font label. Re-reads the font on every theme change.
     */
    public static class Title extends JLabel {

        private final float size;
        private final float plainDelta;

        public Title(String text, float size, float plainDelta) {
            super(text);
            this.size = size;
            this.plainDelta = plainDelta;
            refresh();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            refresh();
        }

        private void refresh() {
            if (size == 0) {
                return;
            }
            if (Sigil.isFull()) {
                setFont(Sigil.display(size));
                setForeground(Sigil.lavender());
            } else {
                Font base = UIManager.getFont("Label.font");
                if (base != null) {
                    setFont(base.deriveFont(Font.BOLD, base.getSize2D() + plainDelta));
                }
                setForeground(UIManager.getColor("Label.foreground"));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.isFull() || Sigil.glow() <= 0 || getText() == null || getText().isEmpty()) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout layout = new TextLayout(getText(), getFont(), frc);
                FontMetrics fm = getFontMetrics(getFont());
                int tw = (int) Math.ceil(layout.getAdvance());
                int x = getInsets().left;
                int align = getHorizontalAlignment();
                if (align == CENTER) {
                    x = (getWidth() - tw) / 2;
                } else if (align == RIGHT || align == TRAILING) {
                    x = getWidth() - getInsets().right - tw;
                }
                int y = getInsets().top + (getHeight() - getInsets().top - getInsets().bottom - fm.getHeight()) / 2 + fm.getAscent();
                Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, y));
                SigilArt.glowFill(g2, outline, getForeground(), Sigil.glow());
            } finally {
                g2.dispose();
            }
        }
    }

    /** Small spaced capitals ("CASE FILE") for labels above titles. */
    public static JLabel caps(String text) {
        JLabel l = new JLabel(text) {
            @Override
            public void updateUI() {
                super.updateUI();
                Font base = UIManager.getFont("Label.font");
                if (base != null) {
                    Font f = base.deriveFont(Font.BOLD, base.getSize2D() - 1f);
                    setFont(f.deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.22f)));
                }
                setForeground(Sigil.isActive() ? Sigil.TEXT_SECONDARY : UIManager.getColor("Label.disabledForeground"));
            }
        };
        l.updateUI();
        return l;
    }

    /** Thorn divider; collapses to nothing when the theme is off or ornament is low. */
    public static class Divider extends JComponent {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(40, Sigil.atLeast(SigilDetail.MEDIUM) ? 14 : 0);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.atLeast(SigilDetail.MEDIUM)) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                SigilArt.divider(g2, 0, getHeight() / 2.0, getWidth(), Sigil.alpha(Sigil.primary(), 0.85f), Sigil.glow());
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Application mark for the toolbar: emblem plus "AUTOPSY" in the display
     * face. Hidden unless the full presentation is on.
     */
    public static class Wordmark extends JComponent {

        public Wordmark() {
            setToolTipText("Autopsy");
            refresh();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            refresh();
        }

        private void refresh() {
            setVisible(Sigil.isFull());
            revalidate();
        }

        @Override
        public Dimension getPreferredSize() {
            if (!Sigil.isFull()) {
                return new Dimension(0, 0);
            }
            FontMetrics fm = getFontMetrics(Sigil.display(19));
            return new Dimension(30 + fm.stringWidth("Autopsy") + 14, 30);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.isFull()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                float glow = Sigil.glow();
                SigilArt.emblem(g2, 4, 3, 24, Sigil.primary(), glow);
                Font f = Sigil.display(19);
                TextLayout tl = new TextLayout("Autopsy", f, g2.getFontRenderContext());
                FontMetrics fm = getFontMetrics(f);
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                SigilArt.glowFill(g2, tl.getOutline(AffineTransform.getTranslateInstance(32, y)), Sigil.lavender(), glow);
                g2.setColor(Sigil.alpha(Sigil.primary(), 0.7f));
                g2.fill(SigilArt.thorn(32, getHeight() - 4, getWidth() - 6, getHeight() - 4, 1.6));
            } finally {
                g2.dispose();
            }
        }
    }

    /** A plain transparent panel, for layouts inside sigil surfaces. */
    public static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout == null ? new BorderLayout() : layout);
        p.setOpaque(false);
        return p;
    }
}

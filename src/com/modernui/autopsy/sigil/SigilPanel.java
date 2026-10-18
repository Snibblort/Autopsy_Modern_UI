package com.modernui.autopsy.sigil;

import com.modernui.autopsy.ModernUISettings.SigilDetail;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * An architectural panel: clipped or arch-topped frame, layered fill, corner
 * pieces, engraving and, on hover, a light tracing the edge.
 *
 * When Cyber-Sigilism is off it paints nothing and behaves like a plain
 * transparent JPanel, so callers can use it unconditionally.
 */
public class SigilPanel extends JPanel implements SigilClock.Animated {

    public enum Shape2 {
        CLIPPED, ARCH, NOTCHED
    }

    private final Shape2 kind;
    private final long seed;
    private boolean hover;
    private boolean emblem;
    private final AWTEventListener hoverWatcher;

    public SigilPanel(LayoutManager layout, Shape2 kind, long seed) {
        super(layout);
        this.kind = kind;
        this.seed = seed;
        setOpaque(false);
        // Track hover across children too (buttons inside the card).
        hoverWatcher = e -> {
            if (!(e instanceof MouseEvent) || !isShowing()) {
                return;
            }
            MouseEvent me = (MouseEvent) e;
            Point p = me.getLocationOnScreen();
            Point o = getLocationOnScreen();
            setHover(p.x >= o.x && p.y >= o.y && p.x < o.x + getWidth() && p.y < o.y + getHeight());
        };
        SigilClock.attach(this);
        updateInsets();
    }

    /** Draws the panel's sigil in the arch (arch panels only). */
    public SigilPanel withEmblem() {
        emblem = true;
        updateInsets();
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Toolkit.getDefaultToolkit().addAWTEventListener(hoverWatcher,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        updateInsets();
    }

    @Override
    public void removeNotify() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(hoverWatcher);
        super.removeNotify();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setOpaque(false);
        updateInsets();
    }

    private void updateInsets() {
        if (kind == null) {
            return;
        }
        if (!Sigil.isActive()) {
            setBorder(new EmptyBorder(8, 10, 8, 10));
        } else if (kind == Shape2.ARCH) {
            int top = archRise() + (emblem && Sigil.atLeast(SigilDetail.MEDIUM) ? 92 : 18);
            setBorder(new EmptyBorder(top, 22, 20, 22));
        } else {
            setBorder(new EmptyBorder(16, 20, 16, 20));
        }
    }

    private int archRise() {
        return Sigil.isFull() ? 64 : 14;
    }

    private void setHover(boolean h) {
        if (h != hover) {
            hover = h;
            repaint();
        }
    }

    @Override
    public void tick() {
        if (hover && Sigil.isActive()) {
            repaint();
        }
    }

    private Shape outline() {
        double w = getWidth() - 1;
        double h = getHeight() - 1;
        switch (kind) {
            case ARCH:
                return SigilArt.archTop(0.5, 0.5, w, h, archRise());
            case NOTCHED:
                return SigilArt.notched(0.5, 0.5, w, h, 14);
            case CLIPPED:
            default:
                return SigilArt.clipped(0.5, 0.5, w, h, 10);
        }
    }

    /** Arch windows want height; they stand at least 320px tall in the full presentation. */
    @Override
    public java.awt.Dimension getPreferredSize() {
        java.awt.Dimension d = super.getPreferredSize();
        if (kind == Shape2.ARCH && emblem && Sigil.isFull()) {
            d.height = Math.max(d.height, 340);
        }
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!Sigil.isActive() || getWidth() < 20 || getHeight() < 20) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            Shape s = outline();
            float glow = Sigil.glow();
            boolean full = Sigil.isFull();
            g2.setPaint(new GradientPaint(0, 0, hover ? Sigil.ELEVATED : Sigil.PANEL, 0, getHeight(), Sigil.BACKGROUND));
            g2.fill(s);
            if (Sigil.atLeast(SigilDetail.HIGH)) {
                SigilArt.hatch(g2, s, Sigil.alpha(Sigil.primary(), hover ? 0.05f : 0.03f), 9);
            }
            if (full && kind == Shape2.ARCH) {
                // inner arch, like a recessed window
                Shape inner = SigilArt.archTop(7, 7, getWidth() - 15, getHeight() - 15, archRise() - 4);
                g2.setColor(Sigil.alpha(Sigil.primary(), 0.16f));
                g2.draw(inner);
            }
            if (full && emblem && kind == Shape2.ARCH && Sigil.atLeast(SigilDetail.MEDIUM)) {
                double cx = getWidth() / 2.0;
                double cy = archRise() + 38;
                if (Sigil.atLeast(SigilDetail.HIGH)) {
                    SigilArt.roseWindow(g2, cx, cy, 40, SigilClock.phase(240), Sigil.alpha(Sigil.primary(), 0.22f), 8);
                }
                SigilArt.sigil(g2, cx, cy, 62, seed, hover ? Sigil.lavender() : Sigil.primary(), glow * (hover ? 1f : 0.6f));
            }
            Color edge = hover ? Sigil.primary() : Sigil.BORDER;
            SigilArt.glowStroke(g2, s, edge, 1f, hover ? glow : 0f);
            if (Sigil.atLeast(SigilDetail.MEDIUM)) {
                Rectangle2D r = new Rectangle2D.Double(0, kind == Shape2.ARCH ? archRise() : 0,
                        getWidth(), getHeight() - (kind == Shape2.ARCH ? archRise() : 0));
                SigilArt.corners(g2, r, 4, 14, hover ? Sigil.lavender() : Sigil.alpha(Sigil.primary(), 0.8f),
                        hover ? glow : glow * 0.4f);
            }
            if (hover && Sigil.animate()) {
                SigilArt.trace(g2, s, SigilClock.phase(3.2), 0.12, Sigil.lavender(), glow);
            }
        } finally {
            g2.dispose();
        }
    }
}

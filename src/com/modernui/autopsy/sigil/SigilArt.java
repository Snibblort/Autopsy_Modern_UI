package com.modernui.autopsy.sigil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vector ornaments for Cyber-Sigilism, all built from geometry at paint time
 * (no images), so they stay sharp at any size and take the current colors.
 *
 * Painters take a Graphics2D they may change freely; callers pass a copy.
 */
public final class SigilArt {

    private SigilArt() {
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ---- shapes -------------------------------------------------------

    /** Rectangle with all four corners cut at 45°. */
    public static Path2D clipped(double x, double y, double w, double h, double cut) {
        cut = Math.max(0, Math.min(cut, Math.min(w, h) / 2));
        Path2D p = new Path2D.Double();
        p.moveTo(x + cut, y);
        p.lineTo(x + w - cut, y);
        p.lineTo(x + w, y + cut);
        p.lineTo(x + w, y + h - cut);
        p.lineTo(x + w - cut, y + h);
        p.lineTo(x + cut, y + h);
        p.lineTo(x, y + h - cut);
        p.lineTo(x, y + cut);
        p.closePath();
        return p;
    }

    /** Rectangle with the top-left and bottom-right corners cut, the rest square. */
    public static Path2D notched(double x, double y, double w, double h, double cut) {
        cut = Math.max(0, Math.min(cut, Math.min(w, h) / 2));
        Path2D p = new Path2D.Double();
        p.moveTo(x + cut, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w, y + h - cut);
        p.lineTo(x + w - cut, y + h);
        p.lineTo(x, y + h);
        p.lineTo(x, y + cut);
        p.closePath();
        return p;
    }

    /**
     * Panel whose top edge rises to a pointed (Gothic) arch. {@code rise} is
     * how far the apex sits above the springing line.
     */
    public static Path2D archTop(double x, double y, double w, double h, double rise) {
        rise = Math.max(0, Math.min(rise, h / 2));
        double spring = y + rise;
        double mid = x + w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x, spring);
        // Each half bulges outward like a segment of a circle, meeting at a point.
        p.curveTo(x, spring - rise * 0.5, mid - w * 0.16, y + rise * 0.3, mid, y);
        p.curveTo(mid + w * 0.16, y + rise * 0.3, x + w, spring - rise * 0.5, x + w, spring);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }

    /** An equilateral lancet (pointed window) standing on (x, y+h), apex at the top. */
    public static Path2D lancet(double x, double y, double w, double h) {
        double r = w;
        double rise = r * Math.sin(Math.toRadians(60));
        double spring = y + Math.min(rise, h);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x, spring);
        p.append(new Arc2D.Double(x + w - r, spring - r, 2 * r, 2 * r, 180, -60, Arc2D.OPEN), true);
        p.append(new Arc2D.Double(x - r, spring - r, 2 * r, 2 * r, 60, -60, Arc2D.OPEN), true);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }

    /** A long, sharp triangle from a base (centered on x1,y1, width w) to a point. */
    public static Path2D thorn(double x1, double y1, double x2, double y2, double width) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len == 0) {
            return new Path2D.Double();
        }
        double nx = -dy / len * width / 2;
        double ny = dx / len * width / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(x1 + nx, y1 + ny);
        p.lineTo(x2, y2);
        p.lineTo(x1 - nx, y1 - ny);
        p.closePath();
        return p;
    }

    public static Path2D diamond(double cx, double cy, double rx, double ry) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - ry);
        p.lineTo(cx + rx, cy);
        p.lineTo(cx, cy + ry);
        p.lineTo(cx - rx, cy);
        p.closePath();
        return p;
    }

    // ---- light ---------------------------------------------------------

    /**
     * Strokes a shape with a soft ultraviolet halo under a crisp line.
     * {@code glow} 0..1 scales the halo; 0 draws just the line.
     */
    public static void glowStroke(Graphics2D g, Shape shape, Color color, float width, float glow) {
        Stroke old = g.getStroke();
        if (glow > 0.01f) {
            for (int i = 3; i >= 1; i--) {
                g.setStroke(new BasicStroke(width + i * 2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
                g.setColor(Sigil.alpha(color, glow * 0.07f * (4 - i)));
                g.draw(shape);
            }
        }
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(color);
        g.draw(shape);
        g.setStroke(old);
    }

    /** Fills a shape and haloes it. */
    public static void glowFill(Graphics2D g, Shape shape, Color color, float glow) {
        if (glow > 0.01f) {
            Stroke old = g.getStroke();
            for (int i = 3; i >= 1; i--) {
                g.setStroke(new BasicStroke(i * 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Sigil.alpha(color, glow * 0.08f * (4 - i)));
                g.draw(shape);
            }
            g.setStroke(old);
        }
        g.setColor(color);
        g.fill(shape);
    }

    /**
     * Draws a bright segment travelling around a shape's outline. {@code phase}
     * 0..1 is the segment's position; {@code length} is a fraction of the
     * perimeter.
     */
    public static void trace(Graphics2D g, Shape shape, double phase, double length, Color color, float glow) {
        List<double[]> pts = flatten(shape);
        if (pts.size() < 2) {
            return;
        }
        double[] cum = new double[pts.size()];
        for (int i = 1; i < pts.size(); i++) {
            double[] a = pts.get(i - 1);
            double[] b = pts.get(i);
            cum[i] = cum[i - 1] + Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
        double total = cum[cum.length - 1];
        if (total <= 0) {
            return;
        }
        double start = (phase % 1.0) * total;
        double span = length * total;
        Path2D seg = new Path2D.Double();
        boolean started = false;
        for (double d = 0; d <= span; d += 2) {
            double[] pt = at(pts, cum, (start + d) % total);
            if (!started) {
                seg.moveTo(pt[0], pt[1]);
                started = true;
            } else {
                seg.lineTo(pt[0], pt[1]);
            }
        }
        glowStroke(g, seg, color, 1.6f, Math.max(glow, 0.35f));
        double[] head = at(pts, cum, (start + span) % total);
        glowFill(g, diamond(head[0], head[1], 2.4, 2.4), Color.WHITE, glow);
    }

    private static double[] at(List<double[]> pts, double[] cum, double d) {
        for (int i = 1; i < pts.size(); i++) {
            if (cum[i] >= d) {
                double seg = cum[i] - cum[i - 1];
                double t = seg == 0 ? 0 : (d - cum[i - 1]) / seg;
                double[] a = pts.get(i - 1);
                double[] b = pts.get(i);
                return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
            }
        }
        return pts.get(pts.size() - 1);
    }

    private static List<double[]> flatten(Shape shape) {
        List<double[]> pts = new ArrayList<>();
        double[] c = new double[6];
        double[] first = null;
        for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                double[] p = {c[0], c[1]};
                if (first == null) {
                    first = p;
                }
                pts.add(p);
            } else if (type == PathIterator.SEG_CLOSE && first != null) {
                pts.add(first);
            }
        }
        return pts;
    }

    // ---- ornaments -----------------------------------------------------

    /**
     * Corner piece: a bracket along both edges with a thorn running inward
     * and a small diamond on the point. (dx, dy) = (+1/-1) picks the corner:
     * (1,1) top-left, (-1,1) top-right, (1,-1) bottom-left, (-1,-1) bottom-right.
     */
    public static void corner(Graphics2D g, double x, double y, double size, int dx, int dy, Color color, float glow) {
        Path2D bracket = new Path2D.Double();
        bracket.moveTo(x + dx * size, y);
        bracket.lineTo(x + dx * 3, y);
        bracket.lineTo(x, y + dy * 3);
        bracket.lineTo(x, y + dy * size);
        glowStroke(g, bracket, color, 1.4f, glow);
        g.setColor(color);
        g.fill(thorn(x + dx * 4, y + dy * 4, x + dx * size * 0.62, y + dy * size * 0.62, 3.2));
        g.fill(thorn(x + dx * size * 0.55, y + 1.5 * dy, x + dx * size * 0.9, y + dy * 4.5, 2.2));
        g.fill(thorn(x + 1.5 * dx, y + dy * size * 0.55, x + dx * 4.5, y + dy * size * 0.9, 2.2));
        glowFill(g, diamond(x + dx * 1.5, y + dy * 1.5, 2.2, 2.2), Sigil.lavender(), glow);
    }

    /** All four corners of a rectangle, inset by {@code inset}. */
    public static void corners(Graphics2D g, Rectangle2D r, double inset, double size, Color color, float glow) {
        double l = r.getX() + inset;
        double t = r.getY() + inset;
        double rt = r.getMaxX() - inset - 1;
        double b = r.getMaxY() - inset - 1;
        corner(g, l, t, size, 1, 1, color, glow);
        corner(g, rt, t, size, -1, 1, color, glow);
        corner(g, l, b, size, 1, -1, color, glow);
        corner(g, rt, b, size, -1, -1, color, glow);
    }

    /** Horizontal divider: central diamond, thorns tapering out to each end, back-barbs. */
    public static void divider(Graphics2D g, double x, double y, double w, Color color, float glow) {
        double cx = x + w / 2;
        g.setColor(color);
        g.fill(thorn(cx - 6, y, x, y, 2.4));
        g.fill(thorn(cx + 6, y, x + w, y, 2.4));
        for (int side = -1; side <= 1; side += 2) {
            double bx = cx + side * w * 0.18;
            g.fill(thorn(bx, y, bx - side * 9, y - 4, 1.6));
            g.fill(thorn(bx, y, bx - side * 9, y + 4, 1.6));
            double ex = cx + side * w * 0.36;
            g.fill(diamond(ex, y, 1.6, 1.6));
        }
        glowFill(g, diamond(cx, y, 5, 4), Sigil.lavender(), glow);
        g.setColor(Sigil.VOID);
        g.fill(diamond(cx, y, 1.8, 1.4));
    }

    /**
     * A symmetrical cyber-sigil: a spine with mirrored thorn branches, a core
     * diamond and a broken ring. The same seed always gives the same sigil.
     */
    public static void sigil(Graphics2D g, double cx, double cy, double size, long seed, Color color, float glow) {
        Random rnd = new Random(seed);
        double h = size / 2;
        Graphics2D s = (Graphics2D) g.create();
        try {
            smooth(s);
            s.setColor(color);
            // spine
            s.fill(thorn(cx, cy - h * 0.15, cx, cy - h, 3.2));
            s.fill(thorn(cx, cy + h * 0.15, cx, cy + h, 3.2));
            // mirrored branches
            int branches = 3 + rnd.nextInt(2);
            for (int i = 0; i < branches; i++) {
                double t = -0.75 + 1.5 * (i + 0.5) / branches + (rnd.nextDouble() - 0.5) * 0.12;
                double by = cy + t * h;
                double reach = h * (0.35 + rnd.nextDouble() * 0.45) * (1 - Math.abs(t) * 0.45);
                double lift = (rnd.nextDouble() - 0.5) * h * 0.7;
                double kinkX = reach * (0.45 + rnd.nextDouble() * 0.2);
                double kinkY = lift * 0.3;
                boolean barb = rnd.nextBoolean();
                for (int side = -1; side <= 1; side += 2) {
                    double kx = cx + side * kinkX;
                    double ky = by + kinkY;
                    s.fill(thorn(cx, by, kx, ky, 2.6));
                    s.fill(thorn(kx, ky, cx + side * reach, by + lift, 2.2));
                    if (barb) {
                        s.fill(thorn(kx, ky, kx + side * reach * 0.2, ky - h * 0.22, 1.5));
                    }
                }
            }
            // broken ring
            s.setStroke(new BasicStroke(1.1f));
            double r = h * 0.62;
            for (int q = 0; q < 4; q++) {
                s.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, q * 90 + 12, 66, Arc2D.OPEN));
            }
            // core
            glowFill(s, diamond(cx, cy, h * 0.16, h * 0.24), Sigil.lavender(), glow);
            s.setColor(Sigil.VOID);
            s.fill(diamond(cx, cy, h * 0.06, h * 0.1));
        } finally {
            s.dispose();
        }
    }

    /**
     * Rose window: concentric rings, a ring of lancet petals, foils between
     * them and a star at the hub. {@code rotation} is in turns (0..1).
     */
    public static void roseWindow(Graphics2D g, double cx, double cy, double r, double rotation, Color line, int petals) {
        Graphics2D s = (Graphics2D) g.create();
        try {
            smooth(s);
            s.setColor(line);
            s.setStroke(new BasicStroke(1f));
            s.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            s.draw(new Ellipse2D.Double(cx - r * 0.95, cy - r * 0.95, r * 1.9, r * 1.9));
            s.draw(new Ellipse2D.Double(cx - r * 0.36, cy - r * 0.36, r * 0.72, r * 0.72));
            s.draw(new Ellipse2D.Double(cx - r * 0.12, cy - r * 0.12, r * 0.24, r * 0.24));
            AffineTransform base = s.getTransform();
            double step = 2 * Math.PI / petals;
            for (int i = 0; i < petals; i++) {
                s.setTransform(base);
                s.rotate(rotation * 2 * Math.PI + i * step, cx, cy);
                double pw = 2 * Math.PI * r * 0.62 / petals * 0.62;
                s.draw(lancet(cx - pw / 2, cy - r * 0.9, pw, r * 0.52));
                s.draw(new Line2D.Double(cx, cy - r * 0.36, cx, cy - r * 0.38 - r * 0.0));
                s.rotate(step / 2, cx, cy);
                double fr = r * 0.055;
                s.draw(new Ellipse2D.Double(cx - fr, cy - r * 0.86 - fr, 2 * fr, 2 * fr));
                s.draw(new Line2D.Double(cx, cy - r * 0.12, cx, cy - r * 0.36));
            }
            s.setTransform(base);
            Path2D star = new Path2D.Double();
            int points = 6;
            for (int i = 0; i < points * 2; i++) {
                double a = -Math.PI / 2 + i * Math.PI / points - rotation * 2 * Math.PI;
                double rr = (i % 2 == 0) ? r * 0.32 : r * 0.16;
                double px = cx + Math.cos(a) * rr;
                double py = cy + Math.sin(a) * rr;
                if (i == 0) {
                    star.moveTo(px, py);
                } else {
                    star.lineTo(px, py);
                }
            }
            star.closePath();
            s.draw(star);
        } finally {
            s.dispose();
        }
    }

    /** Faint diagonal engraving inside a shape. */
    public static void hatch(Graphics2D g, Shape area, Color color, double spacing) {
        Graphics2D s = (Graphics2D) g.create();
        try {
            s.clip(area);
            s.setColor(color);
            s.setStroke(new BasicStroke(1f));
            Rectangle2D b = area.getBounds2D();
            for (double d = -b.getHeight(); d < b.getWidth(); d += spacing) {
                s.draw(new Line2D.Double(b.getX() + d, b.getMaxY(), b.getX() + d + b.getHeight(), b.getY()));
            }
        } finally {
            s.dispose();
        }
    }

    /** Small emblem used as the application mark: a sigil inside a pointed arch. */
    public static void emblem(Graphics2D g, double x, double y, double size, Color color, float glow) {
        Graphics2D s = (Graphics2D) g.create();
        try {
            smooth(s);
            Path2D arch = lancet(x + size * 0.12, y, size * 0.76, size);
            glowStroke(s, arch, color, 1.3f, glow);
            sigil(s, x + size / 2, y + size * 0.6, size * 0.62, 0x5161L, Sigil.lavender(), glow);
        } finally {
            s.dispose();
        }
    }
}

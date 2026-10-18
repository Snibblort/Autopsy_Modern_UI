package com.modernui.autopsy.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.UIManager;

/** Line icons, drawn in the current accent color, for the common Autopsy toolbar buttons. */
public final class ModernIcons implements Icon {

    private final Glyph glyph;
    private final int size;

    private ModernIcons(Glyph glyph, int size) {
        this.glyph = glyph;
        this.size = size;
    }

    public static Icon forToolbarLabel(String label, int preferredSize) {
        Glyph glyph = Glyph.from(label);
        return glyph == null ? null : new ModernIcons(glyph, Math.max(16, preferredSize));
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            double scale = size / 24.0;
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Color foreground = UIManager.getColor("Label.foreground");
            Color accent = UIManager.getColor("Component.focusColor");
            g.setColor(foreground == null ? Color.LIGHT_GRAY : foreground);
            paintGlyph(g, accent == null ? g.getColor() : accent);
        } finally {
            g.dispose();
        }
    }

    private void paintGlyph(Graphics2D g, Color accent) {
        switch (glyph) {
            case DATA_SOURCE:
                g.draw(new RoundRectangle2D.Float(3, 5, 18, 15, 2, 2));
                g.draw(new Line2D.Float(3, 9, 10, 9));
                g.setColor(accent);
                g.draw(new Line2D.Float(15, 10, 15, 18));
                g.draw(new Line2D.Float(11, 14, 19, 14));
                break;
            case MEDIA:
                g.draw(new RoundRectangle2D.Float(3, 4, 18, 16, 2, 2));
                g.draw(new Ellipse2D.Float(7, 7, 3, 3));
                Path2D mountain = new Path2D.Float();
                mountain.moveTo(5, 17);
                mountain.lineTo(10, 12);
                mountain.lineTo(13, 15);
                mountain.lineTo(16, 11);
                mountain.lineTo(20, 16);
                g.draw(mountain);
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(7, 7, 3, 3));
                break;
            case COMMUNICATION:
                Path2D bubble = new Path2D.Float();
                bubble.moveTo(4, 5);
                bubble.quadTo(4, 3, 7, 3);
                bubble.lineTo(18, 3);
                bubble.quadTo(21, 3, 21, 6);
                bubble.lineTo(21, 14);
                bubble.quadTo(21, 17, 18, 17);
                bubble.lineTo(10, 17);
                bubble.lineTo(5, 21);
                bubble.lineTo(6, 16);
                bubble.quadTo(4, 15, 4, 12);
                bubble.closePath();
                g.draw(bubble);
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(8, 9, 2, 2));
                g.fill(new Ellipse2D.Float(12, 9, 2, 2));
                g.fill(new Ellipse2D.Float(16, 9, 2, 2));
                break;
            case LOCATION:
                Path2D pin = new Path2D.Float();
                pin.moveTo(12, 22);
                pin.curveTo(10, 18, 4, 12, 4, 9);
                pin.curveTo(4, 4, 8, 2, 12, 2);
                pin.curveTo(17, 2, 20, 5, 20, 9);
                pin.curveTo(20, 13, 14, 19, 12, 22);
                g.draw(pin);
                g.setColor(accent);
                g.draw(new Ellipse2D.Float(9, 6, 6, 6));
                break;
            case TIMELINE:
                g.draw(new Line2D.Float(3, 12, 21, 12));
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(3, 9, 6, 6));
                g.fill(new Ellipse2D.Float(10, 5, 6, 6));
                g.fill(new Ellipse2D.Float(16, 13, 6, 6));
                break;
            case DISCOVERY:
                g.draw(new Ellipse2D.Float(3, 3, 13, 13));
                g.draw(new Line2D.Float(14, 14, 21, 21));
                g.setColor(accent);
                g.draw(new Line2D.Float(9, 6, 9, 12));
                g.draw(new Line2D.Float(6, 9, 12, 9));
                break;
            case REPORT:
                g.draw(new RoundRectangle2D.Float(5, 2, 14, 20, 2, 2));
                g.draw(new Line2D.Float(8, 8, 16, 8));
                g.draw(new Line2D.Float(8, 12, 16, 12));
                g.setColor(accent);
                g.draw(new Line2D.Float(8, 16, 14, 16));
                break;
            case CLOSE_CASE:
                g.draw(new RoundRectangle2D.Float(3, 4, 18, 16, 2, 2));
                g.setColor(accent);
                g.draw(new Line2D.Float(8, 9, 16, 17));
                g.draw(new Line2D.Float(16, 9, 8, 17));
                break;
            case KEYWORD_LIST:
                g.draw(new Line2D.Float(5, 5, 19, 5));
                g.draw(new Line2D.Float(5, 12, 19, 12));
                g.setColor(accent);
                g.draw(new Line2D.Float(5, 19, 15, 19));
                break;
            case KEYWORD_SEARCH:
                g.draw(new Ellipse2D.Float(3, 3, 13, 13));
                g.draw(new Line2D.Float(14, 14, 21, 21));
                g.setColor(accent);
                g.draw(new Line2D.Float(6, 9, 13, 9));
                break;
            case DASHBOARD:
                g.setColor(accent);
                g.draw(new RoundRectangle2D.Float(3, 3, 8, 8, 2, 2));
                g.draw(new RoundRectangle2D.Float(13, 3, 8, 8, 2, 2));
                g.draw(new RoundRectangle2D.Float(3, 13, 8, 8, 2, 2));
                g.draw(new RoundRectangle2D.Float(13, 13, 8, 8, 2, 2));
                break;
            case NAVIGATOR:
                Path2D sigil = new Path2D.Float();
                sigil.moveTo(12, 2);
                sigil.lineTo(20, 9);
                sigil.lineTo(12, 22);
                sigil.lineTo(4, 9);
                sigil.closePath();
                g.setColor(accent);
                g.draw(sigil);
                g.draw(new Line2D.Float(12, 2, 12, 22));
                g.draw(new Line2D.Float(4, 9, 20, 9));
                break;
            case STUDIO:
                g.draw(new Line2D.Float(4, 6, 20, 6));
                g.draw(new Line2D.Float(4, 12, 20, 12));
                g.draw(new Line2D.Float(4, 18, 20, 18));
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(7, 4, 4, 4));
                g.fill(new Ellipse2D.Float(14, 10, 4, 4));
                g.fill(new Ellipse2D.Float(9, 16, 4, 4));
                break;
            case THEME:
                g.setColor(accent);
                g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                Path2D halfMoon = new Path2D.Float();
                halfMoon.moveTo(12, 3);
                halfMoon.lineTo(12, 21);
                halfMoon.moveTo(12, 3);
                halfMoon.curveTo(3, 3, 3, 21, 12, 21);
                g.draw(halfMoon);
                break;
            case REFRESH:
                g.setColor(accent);
                g.drawArc(4, 4, 16, 16, 45, 275);
                g.draw(new Line2D.Float(17, 3, 20, 7));
                g.draw(new Line2D.Float(17, 3, 13, 4));
                break;
            case EDIT:
                g.setColor(accent);
                g.draw(new Line2D.Float(5, 18, 17, 6));
                g.draw(new Line2D.Float(14, 5, 18, 9));
                g.draw(new Line2D.Float(4, 20, 9, 19));
                g.draw(new Line2D.Float(4, 20, 5, 15));
                break;
            case MORE:
                g.setColor(accent);
                g.fill(new Ellipse2D.Float(10, 3, 4, 4));
                g.fill(new Ellipse2D.Float(10, 10, 4, 4));
                g.fill(new Ellipse2D.Float(10, 17, 4, 4));
                break;
            default:
                break;
        }
    }

    private enum Glyph {
        DATA_SOURCE("data source", "add data"),
        MEDIA("images/videos", "image gallery", "images", "videos", "media"),
        COMMUNICATION("communications", "communication"),
        LOCATION("geolocation", "location"),
        TIMELINE("timeline"),
        DISCOVERY("discovery"),
        REPORT("generate report", "report"),
        CLOSE_CASE("close case"),
        KEYWORD_LIST("keyword lists", "keyword list"),
        KEYWORD_SEARCH("keyword search"),
        DASHBOARD("dashboard"),
        NAVIGATOR("navigator"),
        STUDIO("studio"),
        THEME("theme", "light/dark"),
        REFRESH("refresh counts"),
        EDIT("edit navigator views", "edit"),
        MORE("move this panel", "more");

        private final String[] labels;

        Glyph(String... labels) {
            this.labels = labels;
        }

        static Glyph from(String text) {
            if (text == null) {
                return null;
            }
            String normalized = text.replace("&", "").toLowerCase(java.util.Locale.ROOT).trim();
            for (Glyph glyph : values()) {
                for (String label : glyph.labels) {
                    if (normalized.equals(label) || normalized.contains(label)) {
                        return glyph;
                    }
                }
            }
            return null;
        }
    }
}

package com.modernui.autopsy.sigil.flat;

import com.modernui.autopsy.sigil.Sigil;
import com.modernui.autopsy.sigil.SigilArt;
import com.modernui.autopsy.sigil.SigilControls;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import com.formdev.flatlaf.ui.*;

/*
 * Loaded only through FlatBridge, never by the module's own class loader:
 * NetBeans doesn't export com.formdev.flatlaf.ui to other modules.
 * Uses only public members of the sigil package.
 */
/**
 * Violet gauge with notches every 10%, a lit leading edge and, when
 * indeterminate, a sliding chevron. The text, if any, is FlatLaf's.
 */
public class SigilProgressBarUI extends FlatProgressBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new SigilProgressBarUI();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JProgressBar bar = (JProgressBar) c;
        if (!Sigil.isActive() || bar.getOrientation() != SwingConstants.HORIZONTAL) {
            super.paint(g, c);
            return;
        }
        Insets in = bar.getInsets();
        int x = in.left;
        int y = in.top;
        int w = bar.getWidth() - in.left - in.right;
        int h = bar.getHeight() - in.top - in.bottom;
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            int cut = Math.min(h / 2, 4);
            Shape track = SigilArt.clipped(x, y, w, h, cut);
            g2.setColor(bar.getBackground());
            g2.fill(track);
            Color hi = bar.isEnabled() ? Sigil.primary() : Sigil.BORDER;
            Color lo = bar.isEnabled() ? Sigil.DEEP_VIOLET : Sigil.ELEVATED;
            int amount;
            if (bar.isIndeterminate()) {
                Rectangle box = getBox(null);
                amount = 0;
                if (box != null) {
                    Path2D chevron = new Path2D.Double();
                    double skew = Math.min(h, 8);
                    chevron.moveTo(box.x, y);
                    chevron.lineTo(box.x + box.width, y);
                    chevron.lineTo(box.x + box.width + skew, y + h / 2.0);
                    chevron.lineTo(box.x + box.width, y + h);
                    chevron.lineTo(box.x, y + h);
                    chevron.lineTo(box.x + skew, y + h / 2.0);
                    chevron.closePath();
                    Graphics2D clip = (Graphics2D) g2.create();
                    clip.clip(track);
                    clip.setPaint(new GradientPaint(box.x, 0, lo, box.x + box.width, 0, hi));
                    clip.fill(chevron);
                    clip.dispose();
                }
            } else {
                amount = getAmountFull(in, w, h);
                if (amount > 0) {
                    Graphics2D clip = (Graphics2D) g2.create();
                    clip.clip(track);
                    clip.setPaint(new GradientPaint(x, 0, lo, x + Math.max(amount, 1), 0, hi));
                    clip.fillRect(x, y, amount, h);
                    clip.dispose();
                    if (amount < w && h >= 4) {
                        SigilArt.glowStroke(g2, new java.awt.geom.Line2D.Double(x + amount, y, x + amount, y + h - 1),
                                Sigil.lavender(), 1f, Sigil.glow());
                    }
                }
            }
            if (h >= 6) {
                g2.setColor(Sigil.alpha(Sigil.VOID, 0.55f));
                for (int i = 1; i < 10; i++) {
                    int tx = x + w * i / 10;
                    g2.drawLine(tx, y + h - Math.max(2, h / 3), tx, y + h - 1);
                }
            }
            g2.setColor(bar.isEnabled() ? Sigil.BORDER : Sigil.ELEVATED);
            g2.draw(SigilArt.clipped(x + 0.5, y + 0.5, w - 1, h - 1, cut));
            if (bar.isStringPainted()) {
                paintString(g, x, y, w, h, amount, in);
            }
        } finally {
            g2.dispose();
        }
    }
}

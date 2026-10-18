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
/** Matching clipped outline; focus adds a lavender halo. */
public class SigilButtonBorder extends FlatButtonBorder implements UIResource {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (!Sigil.isActive() || SigilControls.plainButton(c)) {
            super.paintBorder(c, g, x, y, width, height);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            int fw = getFocusWidth(c);
            double w = width - 2 * fw - 1;
            double h = height - 2 * fw - 1;
            Shape s = SigilArt.clipped(x + fw + 0.5, y + fw + 0.5, w, h, SigilControls.cut((int) h));
            if (isFocused(c)) {
                SigilArt.glowStroke(g2, s, Sigil.lavender(), 1f, Math.max(0.4f, Sigil.glow()));
            } else {
                Paint p = getBorderColor(c);
                g2.setPaint(p != null ? p : Sigil.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(s);
            }
            if (Sigil.isFull() && c.isEnabled() && ((AbstractButton) c).getModel().isRollover()) {
                g2.setColor(Sigil.lavender());
                double cx = x + width / 2.0;
                g2.fill(SigilArt.diamond(cx, y + fw + 0.5, 2.5, 1.8));
                g2.fill(SigilArt.diamond(cx, y + height - fw - 1, 2.5, 1.8));
            }
        } finally {
            g2.dispose();
        }
    }
}

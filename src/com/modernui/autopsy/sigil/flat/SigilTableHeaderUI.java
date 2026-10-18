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
/** Header base line in the primary color with a faint rising glow and diamond joints. */
public class SigilTableHeaderUI extends FlatTableHeaderUI {

    public static ComponentUI createUI(JComponent c) {
        return new SigilTableHeaderUI();
    }

    @Override
    protected void paintBottomSeparator(Graphics g, JComponent c, int x, int width) {
        if (!Sigil.isActive()) {
            super.paintBottomSeparator(g, c, x, width);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            int h = c.getHeight();
            float glow = Sigil.glow();
            if (glow > 0) {
                g2.setPaint(new GradientPaint(0, h - 8, Sigil.alpha(Sigil.primary(), 0f),
                        0, h, Sigil.alpha(Sigil.primary(), 0.16f * glow)));
                g2.fillRect(x, h - 8, width, 8);
            }
            g2.setColor(Sigil.primary());
            g2.fillRect(x, h - 1, width, 1);
            if (c instanceof JTableHeader && Sigil.isFull()) {
                TableColumnModel cols = ((JTableHeader) c).getColumnModel();
                int edge = 0;
                g2.setColor(Sigil.lavender());
                for (int i = 0; i < cols.getColumnCount() - 1; i++) {
                    edge += cols.getColumn(i).getWidth();
                    g2.fill(SigilArt.diamond(edge - 0.5, h - 1, 2.5, 2.5));
                }
            }
        } finally {
            g2.dispose();
        }
    }
}

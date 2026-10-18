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
/** Selected tab: a tapered underline with a lit diamond, plus a low violet wash. */
public class SigilTabbedPaneUI extends FlatTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new SigilTabbedPaneUI();
    }

    @Override
    protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) {
        super.paintTabBackground(g, placement, index, x, y, w, h, selected);
        if (!Sigil.isActive() || !selected || placement != SwingConstants.TOP) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setPaint(new GradientPaint(0, y, Sigil.alpha(Sigil.primary(), 0f),
                    0, y + h, Sigil.alpha(Sigil.primary(), 0.10f + 0.12f * Sigil.glow())));
            g2.fill(SigilArt.clipped(x, y, w, h + 6, 5));
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabSelection(Graphics g, int placement, int x, int y, int w, int h) {
        if (!Sigil.isActive() || (placement != SwingConstants.TOP && placement != SwingConstants.BOTTOM)) {
            super.paintTabSelection(g, placement, x, y, w, h);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            boolean focused = tabPane.isEnabled();
            Color c = focused ? Sigil.primary() : Sigil.DEEP_VIOLET;
            double ly = placement == SwingConstants.TOP ? y + h - 1.5 : y + 1;
            double cx = x + w / 2.0;
            g2.setColor(c);
            g2.fill(SigilArt.thorn(cx, ly, x + 2, ly, 3));
            g2.fill(SigilArt.thorn(cx, ly, x + w - 2, ly, 3));
            SigilArt.glowFill(g2, SigilArt.diamond(cx, ly, 4.5, 3), Sigil.lavender(), Sigil.glow());
        } finally {
            g2.dispose();
        }
    }
}

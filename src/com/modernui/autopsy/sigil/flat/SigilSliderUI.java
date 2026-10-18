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
/** Diamond thumb with a halo. */
public class SigilSliderUI extends FlatSliderUI {

    public static ComponentUI createUI(JComponent c) {
        return new SigilSliderUI();
    }

    @Override
    public void paintThumb(Graphics g) {
        if (!Sigil.isActive()) {
            super.paintThumb(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            Rectangle r = thumbRect;
            double cx = r.getCenterX();
            double cy = r.getCenterY();
            double rad = Math.min(r.width, r.height) / 2.0;
            Color c = slider.isEnabled() ? (thumbPressed || thumbHover ? Color.WHITE : Sigil.lavender()) : Sigil.BORDER;
            SigilArt.glowFill(g2, SigilArt.diamond(cx, cy, rad, rad + 1), c, slider.isEnabled() ? Sigil.glow() : 0);
            g2.setColor(Sigil.VOID);
            g2.fill(SigilArt.diamond(cx, cy, rad * 0.35, rad * 0.45));
        } finally {
            g2.dispose();
        }
    }
}

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
/** Buttons with clipped corners and a lit edge on hover/focus. */
public class SigilButtonUI extends FlatButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new SigilButtonUI();
    }

    public SigilButtonUI() {
        super(false);
    }

    @Override
    protected void paintBackground(Graphics g, JComponent c) {
        if (!Sigil.isActive() || SigilControls.plainButton(c)) {
            super.paintBackground(g, c);
            return;
        }
        Color bg = getBackground(c);
        if (bg == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            SigilArt.smooth(g2);
            int fw = SigilControls.focusWidth();
            int w = c.getWidth() - 2 * fw;
            int h = c.getHeight() - 2 * fw;
            Shape s = SigilArt.clipped(fw, fw, w, h, SigilControls.cut(h));
            g2.setColor(bg);
            g2.fill(s);
            ButtonModel m = ((AbstractButton) c).getModel();
            if (c.isEnabled() && (m.isRollover() || m.isPressed())) {
                g2.setPaint(new GradientPaint(0, fw + h * 0.5f, Sigil.alpha(Sigil.primary(), 0f),
                        0, fw + h, Sigil.alpha(Sigil.primary(), 0.22f + 0.2f * Sigil.glow())));
                g2.fill(s);
            }
        } finally {
            g2.dispose();
        }
    }
}

package com.modernui.autopsy.sigil;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.ingest.IngestManager;

/**
 * Status-bar element: a small sigil that turns while ingest runs, with the
 * job count. Clicking it opens the Case Workspace. Only visible with
 * Cyber-Sigilism on.
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 50)
public final class SigilStatusLine implements StatusLineElementProvider {

    private static final Logger LOGGER = Logger.getLogger(SigilStatusLine.class.getName());

    private Element element;

    @Override
    public synchronized Component getStatusLineElement() {
        if (element == null) {
            element = new Element();
        }
        return element;
    }

    private static final class Element extends JComponent implements SigilClock.Animated {

        private volatile int jobs;
        private final PropertyChangeListener ingestListener = e -> refreshJobs();

        Element() {
            setToolTipText("Ingest status. Click for the Case Workspace.");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    com.modernui.autopsy.studio.CaseWorkspaceTopComponent.openWorkspace();
                }
            });
            try {
                IngestManager.getInstance().addIngestJobEventListener(EnumSet.allOf(IngestManager.IngestJobEvent.class),
                        ingestListener);
            } catch (RuntimeException | LinkageError ex) {
                LOGGER.log(Level.FINE, "Ingest status unavailable", ex);
            }
            SigilClock.attach(this);
            refreshJobs();
            setVisible(Sigil.isActive());
        }

        private void refreshJobs() {
            int n = 0;
            try {
                if (IngestManager.getInstance().isIngestRunning()) {
                    n = Math.max(1, IngestManager.getInstance().getIngestJobSnapshots().size());
                }
            } catch (RuntimeException | LinkageError ex) {
                n = 0;
            }
            jobs = n;
            SwingUtilities.invokeLater(() -> {
                revalidate();
                repaint();
            });
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setVisible(Sigil.isActive());
            revalidate();
        }

        @Override
        public void tick() {
            if (jobs > 0) {
                repaint();
            }
        }

        private String text() {
            return jobs > 0 ? "INGEST · " + jobs : "IDLE";
        }

        private Font font() {
            Font base = UIManager.getFont("Label.font");
            return (base != null ? base : new Font(Font.SANS_SERIF, Font.PLAIN, 11)).deriveFont(Font.BOLD, 10.5f);
        }

        @Override
        public Dimension getPreferredSize() {
            if (!Sigil.isActive()) {
                return new Dimension(0, 0);
            }
            return new Dimension(26 + getFontMetrics(font()).stringWidth(text()) + 10, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!Sigil.isActive()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                SigilArt.smooth(g2);
                double cy = getHeight() / 2.0;
                boolean running = jobs > 0;
                double turn = running ? SigilClock.phase(4) : 0;
                g2.rotate(turn * 2 * Math.PI, 12, cy);
                SigilArt.sigil(g2, 12, cy, 16, 0xA11L, running ? Sigil.secondary() : Sigil.primary(),
                        running ? Sigil.glow() : 0f);
                g2.rotate(-turn * 2 * Math.PI, 12, cy);
                g2.setFont(font());
                g2.setColor(running ? Sigil.secondary() : Sigil.TEXT_SECONDARY);
                int ty = (int) (cy + g2.getFontMetrics().getAscent() / 2.0 - 1);
                g2.drawString(text(), 24, ty);
            } finally {
                g2.dispose();
            }
        }
    }
}

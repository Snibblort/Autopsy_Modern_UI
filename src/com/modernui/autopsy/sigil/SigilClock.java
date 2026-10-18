package com.modernui.autopsy.sigil;

import java.awt.event.HierarchyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * One shared timer for every sigil animation. It only runs while at least one
 * registered component is on screen and animation is enabled, and each tick
 * only repaints those components. Swing thread only.
 */
public final class SigilClock {

    /** Frame interval. 20 fps is plenty for slow light and linework. */
    private static final int INTERVAL_MS = 50;

    private static final List<WeakReference<JComponent>> CLIENTS = new ArrayList<>();
    private static final Timer TIMER = new Timer(INTERVAL_MS, e -> tick());
    private static final long START = System.nanoTime();

    static {
        TIMER.setCoalesce(true);
    }

    private SigilClock() {
    }

    /** Seconds since the clock was created; the phase source for all animation. */
    public static double seconds() {
        return (System.nanoTime() - START) / 1e9;
    }

    /** 0..1, repeating every {@code periodSeconds}. Frozen at 0 when motion is off. */
    public static double phase(double periodSeconds) {
        if (!Sigil.animate()) {
            return 0;
        }
        return (seconds() / periodSeconds) % 1.0;
    }

    /** Repaint {@code c} on every tick while it is showing. */
    public static void attach(JComponent c) {
        CLIENTS.add(new WeakReference<>(c));
        c.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                update();
            }
        });
        update();
    }

    /** Re-checks whether the timer should run (call after settings change). */
    public static void update() {
        boolean anyShowing = false;
        for (Iterator<WeakReference<JComponent>> it = CLIENTS.iterator(); it.hasNext();) {
            JComponent c = it.next().get();
            if (c == null) {
                it.remove();
            } else if (c.isShowing()) {
                anyShowing = true;
            }
        }
        boolean run = anyShowing && Sigil.animate();
        if (run && !TIMER.isRunning()) {
            TIMER.start();
        } else if (!run && TIMER.isRunning()) {
            TIMER.stop();
        }
    }

    private static void tick() {
        if (!Sigil.animate()) {
            TIMER.stop();
            return;
        }
        boolean anyShowing = false;
        for (Iterator<WeakReference<JComponent>> it = CLIENTS.iterator(); it.hasNext();) {
            JComponent c = it.next().get();
            if (c == null) {
                it.remove();
            } else if (c.isShowing()) {
                anyShowing = true;
                if (c instanceof Animated) {
                    ((Animated) c).tick();
                } else {
                    c.repaint();
                }
            }
        }
        if (!anyShowing) {
            TIMER.stop();
        }
    }

    /** Components that want to choose what to repaint on each tick. */
    public interface Animated {

        void tick();
    }
}

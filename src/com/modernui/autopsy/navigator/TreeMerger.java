package com.modernui.autopsy.navigator;

import com.modernui.autopsy.ModernUISettings;
import java.awt.event.HierarchyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.directorytree.DirectoryTreeTopComponent;

/**
 * Hides DirectoryTreeTopComponent while its tree is shown in the Navigator.
 *
 * It can't be closed (Autopsy disables that, and other code expects it open),
 * so we dock it behind the Navigator; view modes have no tabs in Autopsy, so
 * it's invisible there. Autopsy re-docks it on every case open and raises it
 * from "go to" actions, hence the timer and hierarchy listener.
 */
final class TreeMerger {

    private static final Logger LOGGER = Logger.getLogger(TreeMerger.class.getName());
    private static final String TREE_MODE = "explorer";

    private static Timer watchdog;
    private static boolean listening;

    private TreeMerger() {
    }

    /** Starts (or stops) keeping Autopsy's tree merged, per the saved setting. Swing thread. */
    static void update(TopComponent navigator) {
        boolean merge = ModernUISettings.getInstance().isMergeAutopsyTree();
        if (merge) {
            if (watchdog == null) {
                watchdog = new Timer(1500, e -> enforce(navigator));
                watchdog.start();
            }
            listenForTreeShowing(navigator);
            enforce(navigator);
        } else {
            if (watchdog != null) {
                watchdog.stop();
                watchdog = null;
            }
            restore();
        }
    }

    /** Moves Autopsy's tree panel behind the Navigator if it is anywhere else. */
    static void enforce(TopComponent navigator) {
        if (!ModernUISettings.getInstance().isMergeAutopsyTree() || !navigator.isOpened()) {
            return;
        }
        try {
            DirectoryTreeTopComponent tree = DirectoryTreeTopComponent.findInstance();
            if (!tree.isOpened()) {
                return;
            }
            Mode navMode = WindowManager.getDefault().findMode(navigator);
            if (navMode == null) {
                return;
            }
            Mode treeMode = WindowManager.getDefault().findMode(tree);
            if (treeMode != navMode) {
                navMode.dockInto(tree);
                tree.open();
            }
            if (tree.isShowing()) {
                navigator.requestVisible();
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Could not tuck away Autopsy's tree", ex);
        }
    }

    /** Puts Autopsy's tree panel back in its own column. */
    static void restore() {
        try {
            DirectoryTreeTopComponent tree = DirectoryTreeTopComponent.findInstance();
            Mode explorer = WindowManager.getDefault().findMode(TREE_MODE);
            if (explorer != null && tree.isOpened() && WindowManager.getDefault().findMode(tree) != explorer) {
                explorer.dockInto(tree);
                tree.open();
                tree.requestVisible();
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "Could not restore Autopsy's tree", ex);
        }
    }

    /**
     * Autopsy's "view in directory" style actions bring its tree panel to the
     * front. Since the Navigator shows the same tree (same selection), bring the
     * Navigator back on top right away.
     */
    private static void listenForTreeShowing(TopComponent navigator) {
        if (listening) {
            return;
        }
        try {
            DirectoryTreeTopComponent tree = DirectoryTreeTopComponent.findInstance();
            tree.addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && tree.isShowing()
                        && ModernUISettings.getInstance().isMergeAutopsyTree()) {
                    SwingUtilities.invokeLater(() -> enforce(navigator));
                }
            });
            listening = true;
        } catch (RuntimeException | LinkageError ex) {
            LOGGER.log(Level.FINE, "Autopsy's tree not available yet", ex);
        }
    }
}

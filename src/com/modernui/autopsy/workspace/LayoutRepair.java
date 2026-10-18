package com.modernui.autopsy.workspace;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbPreferences;

/**
 * 2.0 shipped a navigator mode constraint that scrambled the main window, and
 * NetBeans persisted the result in Windows2Local, which overrides our layer
 * defaults. If that exact layout is on disk, move it to
 * config/ModernUI/layout-backup so Autopsy falls back to its default layout.
 * Runs once.
 */
public final class LayoutRepair {

    private static final Logger LOGGER = Logger.getLogger(LayoutRepair.class.getName());
    private static final String DONE_KEY = "layoutRepair201Done";
    private static final String LAYOUT = "Windows2Local";
    private static final String NAV_MODE = LAYOUT + "/Modes/modernuiNavigator.wsmode";
    private static final String BACKUP = "ModernUI/layout-backup";
    private static final Pattern FIRST_PATH = Pattern.compile(
            "<path\\s+orientation=\"(\\w+)\"\\s+number=\"(\\d+)\"");

    private LayoutRepair() {
    }

    /** Call from ModuleInstall.validate(), before the window system reads the layout. */
    public static void runOnce() {
        Preferences prefs = NbPreferences.forModule(LayoutRepair.class);
        if (prefs.getBoolean(DONE_KEY, false)) {
            return;
        }
        try {
            FileObject navMode = FileUtil.getConfigFile(NAV_MODE);
            if (navMode != null && isBrokenNavigatorMode(navMode.asText("UTF-8"))) {
                FileObject layout = FileUtil.getConfigFile(LAYOUT);
                if (layout != null) {
                    backupAndClear(layout);
                    LOGGER.info("Reset the window layout saved by 2.0 (backup in config/" + BACKUP + ").");
                }
            }
            prefs.putBoolean(DONE_KEY, true);
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not repair the window layout; use Reset window layout in the Studio.", ex);
        }
    }

    /**
     * The 2.0 layout starts the Navigator's path with a vertical split
     * (saved as horizontal 0 / vertical 0). The fixed placement starts with
     * horizontal 10, the Navigator's own column beside the tree.
     */
    static boolean isBrokenNavigatorMode(String wsmode) {
        Matcher m = FIRST_PATH.matcher(wsmode);
        if (!m.find()) {
            return false;
        }
        String firstOrientation = m.group(1);
        String firstNumber = m.group(2);
        boolean secondIsVerticalZero = m.find() && "vertical".equals(m.group(1)) && "0".equals(m.group(2));
        return !"horizontal".equals(firstOrientation)
                || ("0".equals(firstNumber) && secondIsVerticalZero);
    }

    private static void backupAndClear(FileObject layout) throws IOException {
        FileObject backupRoot = FileUtil.createFolder(FileUtil.getConfigRoot(), BACKUP);
        for (FileObject old : backupRoot.getChildren()) {
            old.delete();
        }
        copyTree(layout, backupRoot);
        for (FileObject child : layout.getChildren()) {
            child.delete();
        }
    }

    private static void copyTree(FileObject from, FileObject toFolder) throws IOException {
        for (FileObject child : from.getChildren()) {
            if (child.isFolder()) {
                copyTree(child, FileUtil.createFolder(toFolder, child.getNameExt()));
            } else {
                FileUtil.copyFile(child, toFolder, child.getName(), child.getExt());
            }
        }
    }
}

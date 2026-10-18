package com.modernui.autopsy.workspace;

import com.modernui.autopsy.ModernUISettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.event.ChangeListener;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.windows.WindowManager;

/**
 * Named workspaces built on NetBeans window-system roles. Each role keeps its
 * own window arrangement (which panels are open, where, and how big), saved
 * automatically in config/Windows2Local-&lt;name&gt;. "Default" is Autopsy's
 * normal layout (no role).
 */
public final class WorkspaceManager {

    public static final String DEFAULT = "Default";

    private static final Logger LOGGER = Logger.getLogger(WorkspaceManager.class.getName());
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9 _-]{1,40}");
    private static final ChangeSupport CHANGES = new ChangeSupport(WorkspaceManager.class);

    private WorkspaceManager() {
    }

    public static void addChangeListener(ChangeListener l) {
        CHANGES.addChangeListener(l);
    }

    public static void removeChangeListener(ChangeListener l) {
        CHANGES.removeChangeListener(l);
    }

    /** "Default" first, then the user's workspaces in the order created. */
    public static List<String> list() {
        List<String> names = new ArrayList<>();
        names.add(DEFAULT);
        for (String line : ModernUISettings.getInstance().getWorkspaces().split("\\n")) {
            String name = line.trim();
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    public static String current() {
        String role = WindowManager.getDefault().getRole();
        return role == null || role.isEmpty() ? DEFAULT : role;
    }

    /** Returns an error message, or null if the name can be used. */
    public static String validate(String name) {
        if (name == null || !VALID.matcher(name.trim()).matches()) {
            return "Use 1-40 letters, numbers, spaces, - or _.";
        }
        for (String existing : list()) {
            if (existing.equalsIgnoreCase(name.trim())) {
                return "A workspace named \"" + existing + "\" already exists.";
            }
        }
        return null;
    }

    /** Switches the window layout. Call on the Swing thread. */
    public static void switchTo(String name) {
        WindowManager.getDefault().setRole(DEFAULT.equals(name) ? null : name);
        CHANGES.fireChange();
    }

    /**
     * Switches to the default workspace and asks NetBeans to restore its default
     * window arrangement. Returns false when the platform reset action is absent.
     */
    public static boolean restoreDefaultLayout() {
        Action reset = Actions.forID("Window", "org.netbeans.core.windows.actions.ResetWindowsAction");
        if (reset == null) {
            return false;
        }
        WindowManager.getDefault().setRole(null);
        reset.actionPerformed(new java.awt.event.ActionEvent(
                WorkspaceManager.class, java.awt.event.ActionEvent.ACTION_PERFORMED, "reset"));
        CHANGES.fireChange();
        return true;
    }

    /** Creates a workspace (starting from Autopsy's default layout) and switches to it. */
    public static void create(String name) {
        String clean = name.trim();
        List<String> names = list();
        names.remove(DEFAULT);
        names.add(clean);
        save(names);
        switchTo(clean);
    }

    /** Deletes a workspace and its saved layout. Switches to Default first if needed. */
    public static void delete(String name) {
        if (DEFAULT.equals(name)) {
            return;
        }
        if (name.equals(current())) {
            switchTo(DEFAULT);
        }
        List<String> names = list();
        names.remove(DEFAULT);
        names.remove(name);
        save(names);
        FileObject layout = FileUtil.getConfigFile("Windows2Local-" + name);
        if (layout != null) {
            try {
                layout.delete();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Could not delete layout for workspace " + name, ex);
            }
        }
        CHANGES.fireChange();
    }

    private static void save(List<String> names) {
        ModernUISettings.getInstance().setWorkspaces(String.join("\n", names));
    }

    static void fireChanged() {
        CHANGES.fireChange();
    }
}

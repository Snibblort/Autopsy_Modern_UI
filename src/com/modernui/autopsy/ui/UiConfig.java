package com.modernui.autopsy.ui;

import com.modernui.autopsy.ConfigStore;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MultiFileSystem;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;

/**
 * Reads and edits the configuration folders behind Autopsy's toolbars
 * ("Toolbars") and menu bar ("Menu").
 *
 * <p>NetBeans builds both from these folders and rebuilds them live when the
 * folders change. Changes are written to the user's config layer only, so
 * Autopsy's own files are never modified and "Reset" removes them again.
 * Hiding writes a "_hidden" marker, which is the same mechanism Autopsy
 * itself uses to hide NetBeans items. Reset uses NetBeans' FileObject.revert()
 * where available, plus the Studio's own record of the original order.
 */
public final class UiConfig {

    public static final String TOOLBARS = "Toolbars";
    public static final String MENU = "Menu";

    private static final Logger LOGGER = Logger.getLogger(UiConfig.class.getName());

    private UiConfig() {
    }

    /** One toolbar, menu, submenu or entry. */
    public static final class Item {

        public final String path;
        public final String name;
        public final boolean folder;
        public final boolean hidden;
        public final FileObject file;
        public final List<Item> children = new ArrayList<>();

        Item(String path, String name, boolean folder, boolean hidden, FileObject file) {
            this.path = path;
            this.name = name;
            this.folder = folder;
            this.hidden = hidden;
            this.file = file;
        }

        public boolean isSeparator() {
            return name.startsWith("———");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Loads a tree of everything under {@code root} ("Toolbars" or "Menu"), including items hidden here. */
    public static Item load(String root) {
        FileObject folder = FileUtil.getConfigFile(root);
        String title = TOOLBARS.equals(root) ? "Toolbars" : "Menu bar";
        Item item = new Item(root, title, true, false, folder);
        if (folder != null) {
            addChildren(item, folder);
        }
        return item;
    }

    private static void addChildren(Item parent, FileObject folder) {
        List<FileObject> ordered = FileUtil.getOrder(Arrays.asList(folder.getChildren()), false);
        for (FileObject child : ordered) {
            if (child.getNameExt().endsWith("_hidden")) {
                continue;
            }
            Item item = new Item(child.getPath(), displayName(child), child.isFolder(), false, child);
            if (child.isFolder()) {
                addChildren(item, child);
            }
            parent.children.add(item);
        }
        for (Map.Entry<String, String> hidden : hiddenItems().entrySet()) {
            String path = hidden.getKey();
            int slash = path.lastIndexOf('/');
            if (slash > 0 && path.substring(0, slash).equals(folder.getPath())) {
                parent.children.add(new Item(path, hidden.getValue(), false, true, null));
            }
        }
    }

    /** A readable name for a config entry (action name, menu text or folder title). */
    public static String displayName(FileObject fo) {
        String fallback = fo.getName().replace('-', ' ');
        if (fo.getName().toLowerCase().contains("separator")) {
            return "——— separator ———";
        }
        try {
            Object attr = fo.getAttribute("displayName");
            if (attr instanceof String && !((String) attr).isEmpty()) {
                return clean((String) attr);
            }
            DataObject data = DataObject.find(fo);
            if (data instanceof DataFolder) {
                return clean(data.getNodeDelegate().getDisplayName());
            }
            InstanceCookie cookie = data.getLookup().lookup(InstanceCookie.class);
            if (cookie != null) {
                Object instance = cookie.instanceCreate();
                if (instance instanceof JSeparator) {
                    return "——— separator ———";
                }
                if (instance instanceof Action) {
                    Object name = ((Action) instance).getValue(Action.NAME);
                    if (name instanceof String && !((String) name).isEmpty()) {
                        return clean((String) name);
                    }
                }
                if (instance instanceof JMenuItem) {
                    return clean(((JMenuItem) instance).getText());
                }
            }
            String nodeName = data.getNodeDelegate().getDisplayName();
            return nodeName == null || nodeName.isEmpty() ? fallback : clean(nodeName);
        } catch (IOException | ClassNotFoundException | RuntimeException | LinkageError ex) {
            LOGGER.log(Level.FINE, "No display name for " + fo.getPath(), ex);
            return fallback;
        }
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replace("&&", "\u0000").replace("&", "").replace("\u0000", "&");
        return t.replaceAll("<[^>]*>", "").trim();
    }

    /** Hides an entry. Returns false if it couldn't be hidden. */
    public static boolean hide(Item item) {
        if (item.file == null || item.hidden) {
            return false;
        }
        String path = item.file.getPath();
        String name = item.name;
        try {
            if (!hideResource(path, true)) {
                item.file.delete();
            }
            Map<String, String> hidden = hiddenItems();
            hidden.put(path, name);
            saveHidden(hidden);
            return true;
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not hide " + path, ex);
            return false;
        }
    }

    /** Shows an entry hidden earlier from the Studio. */
    public static boolean show(Item item) {
        if (!item.hidden) {
            return false;
        }
        try {
            boolean ok = hideResource(item.path, false);
            Map<String, String> hidden = hiddenItems();
            hidden.remove(item.path);
            saveHidden(hidden);
            refreshParent(item.path);
            return ok;
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not show " + item.path, ex);
            return false;
        }
    }

    /** Moves a visible entry up (-1) or down (+1) within its folder. */
    public static boolean move(Item item, int delta) {
        if (item.file == null || item.file.getParent() == null) {
            return false;
        }
        FileObject parent = item.file.getParent();
        List<FileObject> order = new ArrayList<>(FileUtil.getOrder(Arrays.asList(parent.getChildren()), false));
        int index = order.indexOf(item.file);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= order.size()) {
            return false;
        }
        order.remove(index);
        order.add(target, item.file);
        try {
            snapshotOrder(parent);
            FileUtil.setOrder(order);
            return true;
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not reorder " + item.path, ex);
            return false;
        }
    }

    /**
     * Throws away every change made to this toolbar/menu folder in the user's
     * config (hidden items, order), restoring Autopsy's original.
     */
    public static boolean reset(Item folderItem) {
        if (folderItem.file == null || !folderItem.folder) {
            return false;
        }
        boolean ok = false;
        try {
            if (folderItem.file.canRevert()) {
                folderItem.file.revert();
                ok = true;
            }
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not reset " + folderItem.path, ex);
        }
        ok |= restoreOrder(folderItem.file);
        Map<String, String> hidden = hiddenItems();
        List<String> under = new ArrayList<>();
        for (String p : hidden.keySet()) {
            if (p.startsWith(folderItem.path + "/")) {
                under.add(p);
            }
        }
        for (String p : under) {
            try {
                hideResource(p, false);
            } catch (IOException | RuntimeException ex) {
                LOGGER.log(Level.FINE, "Could not unhide " + p, ex);
            }
            hidden.remove(p);
        }
        saveHidden(hidden);
        refreshParent(folderItem.path + "/x");
        return ok || !under.isEmpty();
    }

    /**
     * Remembers a folder's original item positions the first time the Studio
     * reorders it, so "Reset" can restore them even where NetBeans' own
     * revert isn't available.
     */
    private static void snapshotOrder(FileObject folder) {
        Map<String, String> snapshots = orderSnapshots();
        String prefix = folder.getPath() + "\t";
        for (String key : snapshots.keySet()) {
            if (key.startsWith(prefix)) {
                return;
            }
        }
        for (FileObject child : folder.getChildren()) {
            Object pos = child.getAttribute("position");
            snapshots.put(prefix + child.getNameExt(), pos == null ? "-" : pos.toString());
        }
        saveSnapshots(snapshots);
    }

    private static boolean restoreOrder(FileObject folder) {
        Map<String, String> snapshots = orderSnapshots();
        String prefix = folder.getPath() + "\t";
        boolean restored = false;
        for (java.util.Iterator<Map.Entry<String, String>> it = snapshots.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> e = it.next();
            if (!e.getKey().startsWith(prefix)) {
                continue;
            }
            FileObject child = folder.getFileObject(e.getKey().substring(prefix.length()));
            if (child != null) {
                try {
                    child.setAttribute("position", "-".equals(e.getValue()) ? null : Integer.valueOf(e.getValue()));
                    restored = true;
                } catch (IOException | NumberFormatException ex) {
                    LOGGER.log(Level.FINE, "Could not restore position of " + child.getPath(), ex);
                }
            }
            it.remove();
        }
        saveSnapshots(snapshots);
        return restored;
    }

    private static Map<String, String> orderSnapshots() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : ConfigStore.read(ConfigStore.ORDER_SNAPSHOTS, "").split("\\r?\\n")) {
            String[] f = line.split("\t");
            if (f.length == 3) {
                map.put(f[0] + "\t" + f[1], f[2]);
            }
        }
        return map;
    }

    private static void saveSnapshots(Map<String, String> snapshots) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : snapshots.entrySet()) {
            out.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        ConfigStore.writeQuietly(ConfigStore.ORDER_SNAPSHOTS, out.toString());
    }

    /** Uses MultiFileSystem.hideResource, the API behind "_hidden" markers. */
    static boolean hideResource(String path, boolean hide) throws IOException {
        FileSystem fs = FileUtil.getConfigRoot().getFileSystem();
        if (!(fs instanceof MultiFileSystem)) {
            return false;
        }
        try {
            Method m = MultiFileSystem.class.getDeclaredMethod("hideResource", String.class, boolean.class);
            m.setAccessible(true);
            m.invoke(fs, path, hide);
            return true;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }
            throw new IOException(ex.getCause());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.log(Level.FINE, "hideResource unavailable", ex);
            return false;
        }
    }

    private static void refreshParent(String path) {
        int slash = path.lastIndexOf('/');
        FileObject parent = slash > 0 ? FileUtil.getConfigFile(path.substring(0, slash)) : null;
        if (parent != null) {
            parent.refresh();
        }
    }

    /** Items hidden from the Studio: path -> display name. */
    static Map<String, String> hiddenItems() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : ConfigStore.read(ConfigStore.HIDDEN_ITEMS, "").split("\\r?\\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) {
                map.put(line.substring(0, tab), line.substring(tab + 1));
            }
        }
        return map;
    }

    private static void saveHidden(Map<String, String> hidden) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : hidden.entrySet()) {
            out.append(e.getKey()).append('\t').append(e.getValue().replace('\t', ' ').replace('\n', ' ')).append('\n');
        }
        ConfigStore.writeQuietly(ConfigStore.HIDDEN_ITEMS, out.toString());
    }
}

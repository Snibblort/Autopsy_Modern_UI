package com.modernui.autopsy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Stores larger Modern UI data (theme overrides, navigator layout, hidden
 * toolbar/menu items) as text files in Autopsy's user config folder, under
 * config/ModernUI. Java preferences cap values at 8 KB, which these can exceed.
 */
public final class ConfigStore {

    public static final String THEME_OVERRIDES = "theme-overrides.properties";
    public static final String NAVIGATOR = "navigator.txt";
    public static final String HIDDEN_ITEMS = "hidden-items.txt";
    public static final String ORDER_SNAPSHOTS = "order-snapshots.txt";

    private static final String FOLDER = "ModernUI";
    private static final Logger LOGGER = Logger.getLogger(ConfigStore.class.getName());

    private ConfigStore() {
    }

    /** Returns the file's text, or {@code fallback} if it doesn't exist or can't be read. */
    public static String read(String name, String fallback) {
        try {
            FileObject folder = FileUtil.getConfigFile(FOLDER);
            FileObject file = folder == null ? null : folder.getFileObject(name);
            return file == null ? fallback : file.asText(StandardCharsets.UTF_8.name());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not read " + name, ex);
            return fallback;
        }
    }

    public static void write(String name, String text) throws IOException {
        FileObject folder = FileUtil.createFolder(FileUtil.getConfigRoot(), FOLDER);
        FileObject file = folder.getFileObject(name);
        if (file == null) {
            file = folder.createData(name);
        }
        try (OutputStream out = file.getOutputStream()) {
            out.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Writes, logging instead of throwing. Returns true on success. */
    public static boolean writeQuietly(String name, String text) {
        try {
            write(name, text);
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not save " + name, ex);
            return false;
        }
    }
}

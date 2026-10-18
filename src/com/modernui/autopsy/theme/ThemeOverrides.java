package com.modernui.autopsy.theme;

import com.modernui.autopsy.ConfigStore;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User-edited FlatLaf properties ("Key = value" lines), stored in
 * config/ModernUI/theme-overrides.properties. They are applied last, so they
 * win over the theme, palette and Options settings.
 *
 * <p>Exported theme files use the same format, with optional
 * "# base: Dark" and "# palette: NORD" header lines.
 */
public final class ThemeOverrides {

    private ThemeOverrides() {
    }

    public static Map<String, String> load() {
        return parse(ConfigStore.read(ConfigStore.THEME_OVERRIDES, ""));
    }

    public static boolean save(Map<String, String> overrides) {
        return ConfigStore.writeQuietly(ConfigStore.THEME_OVERRIDES, format(overrides, null, null));
    }

    /** Parses "Key = value" lines; '#' lines and blanks are skipped. Order is kept. */
    public static Map<String, String> parse(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null) {
            return map;
        }
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    /** Reads a "# name: value" header line from an exported theme file, or null. */
    public static String header(String text, String name) {
        if (text == null) {
            return null;
        }
        String prefix = "# " + name + ":";
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.toLowerCase().startsWith(prefix.toLowerCase())) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    public static String format(Map<String, String> overrides, String base, String palette) {
        StringBuilder out = new StringBuilder();
        out.append("# Autopsy Modern UI theme\n");
        if (base != null) {
            out.append("# base: ").append(base).append('\n');
        }
        if (palette != null) {
            out.append("# palette: ").append(palette).append('\n');
        }
        out.append("# FlatLaf properties: Key = value. @name keys are theme variables.\n");
        for (Map.Entry<String, String> e : overrides.entrySet()) {
            out.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
        }
        return out.toString();
    }
}

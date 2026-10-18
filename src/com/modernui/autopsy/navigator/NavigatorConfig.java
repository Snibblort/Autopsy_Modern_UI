package com.modernui.autopsy.navigator;

import com.modernui.autopsy.ConfigStore;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

/**
 * The navigator's layout: named groups of views, where each view is a saved
 * question about the case ("files with these extensions", "these artifact
 * types", "custom SQL"...). Stored in config/ModernUI/navigator.txt.
 */
public final class NavigatorConfig {

    /** What a view looks up. The hint explains the parameter. */
    public enum Kind {
        EXTENSIONS("File extensions", "Comma-separated, e.g. jpg, png, heic"),
        MIME("MIME type", "SQL LIKE pattern, e.g. image/% or application/pdf"),
        ARTIFACTS("Artifact types", "Comma-separated TSK names, e.g. TSK_WEB_HISTORY, TSK_WEB_DOWNLOAD"),
        TAGS("Tagged items", "Tag name, or * for every tag"),
        MODIFIED_DAYS("Modified recently", "Number of days, e.g. 7"),
        LARGER_THAN_MB("Large files", "Minimum size in MB, e.g. 100"),
        DELETED("Deleted files", "No parameter needed"),
        SQL("Custom SQL filter", "WHERE clause on tsk_files, e.g. name LIKE '%invoice%' AND size > 0");

        private final String label;
        private final String hint;

        Kind(String label, String hint) {
            this.label = label;
            this.hint = hint;
        }

        public String getHint() {
            return hint;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class View {

        public String name;
        public Kind kind;
        public String param;

        public View(String name, Kind kind, String param) {
            this.name = name;
            this.kind = kind;
            this.param = param == null ? "" : param;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class Group {

        public String name;
        public final List<View> views = new ArrayList<>();

        public Group(String name) {
            this.name = name;
        }

        Group view(String name, Kind kind, String param) {
            views.add(new View(name, kind, param));
            return this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final ChangeSupport CHANGES = new ChangeSupport(NavigatorConfig.class);

    public final List<Group> groups = new ArrayList<>();

    public static void addChangeListener(ChangeListener l) {
        CHANGES.addChangeListener(l);
    }

    public static void removeChangeListener(ChangeListener l) {
        CHANGES.removeChangeListener(l);
    }

    public static NavigatorConfig load() {
        String text = ConfigStore.read(ConfigStore.NAVIGATOR, null);
        if (text == null || text.trim().isEmpty()) {
            return defaults();
        }
        NavigatorConfig config = parse(text);
        return config.groups.isEmpty() ? defaults() : config;
    }

    public void save() {
        ConfigStore.writeQuietly(ConfigStore.NAVIGATOR, format());
        CHANGES.fireChange();
    }

    /** A triage-oriented starting layout. */
    public static NavigatorConfig defaults() {
        NavigatorConfig c = new NavigatorConfig();
        c.groups.add(new Group("Media")
                .view("Images", Kind.EXTENSIONS, "jpg, jpeg, png, gif, bmp, heic, heif, webp, tif, tiff")
                .view("Videos", Kind.EXTENSIONS, "mp4, mov, avi, mkv, wmv, m4v, 3gp, webm")
                .view("Audio", Kind.EXTENSIONS, "mp3, wav, m4a, aac, flac, ogg, amr, opus"));
        c.groups.add(new Group("Documents")
                .view("Office files", Kind.EXTENSIONS, "doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp, rtf")
                .view("PDFs", Kind.EXTENSIONS, "pdf")
                .view("Text, logs and data", Kind.EXTENSIONS, "txt, log, csv, json, xml, ini, cfg")
                .view("Databases", Kind.EXTENSIONS, "db, sqlite, sqlite3, sqlitedb"));
        c.groups.add(new Group("Suspicious")
                .view("Executables and scripts", Kind.EXTENSIONS, "exe, dll, sys, bat, cmd, ps1, vbs, js, msi, scr, com, jar, apk")
                .view("Archives", Kind.EXTENSIONS, "zip, rar, 7z, tar, gz, tgz, bz2, xz, cab, iso")
                .view("Encrypted or suspected", Kind.ARTIFACTS, "TSK_ENCRYPTION_DETECTED, TSK_ENCRYPTION_SUSPECTED")
                .view("Extension mismatches", Kind.ARTIFACTS, "TSK_EXT_MISMATCH_DETECTED")
                .view("Malware and YARA hits", Kind.ARTIFACTS, "TSK_MALWARE, TSK_YARA_HIT")
                .view("Deleted files", Kind.DELETED, ""));
        c.groups.add(new Group("User activity")
                .view("Web history", Kind.ARTIFACTS, "TSK_WEB_HISTORY")
                .view("Downloads", Kind.ARTIFACTS, "TSK_WEB_DOWNLOAD")
                .view("Web searches", Kind.ARTIFACTS, "TSK_WEB_SEARCH_QUERY")
                .view("Recent documents", Kind.ARTIFACTS, "TSK_RECENT_OBJ")
                .view("Programs run", Kind.ARTIFACTS, "TSK_PROG_RUN")
                .view("Installed programs", Kind.ARTIFACTS, "TSK_INSTALLED_PROG")
                .view("USB and devices", Kind.ARTIFACTS, "TSK_DEVICE_ATTACHED")
                .view("Modified in last 7 days", Kind.MODIFIED_DAYS, "7"));
        c.groups.add(new Group("Communication")
                .view("Messages", Kind.ARTIFACTS, "TSK_MESSAGE")
                .view("Email", Kind.ARTIFACTS, "TSK_EMAIL_MSG")
                .view("Call logs", Kind.ARTIFACTS, "TSK_CALLLOG")
                .view("Contacts", Kind.ARTIFACTS, "TSK_CONTACT")
                .view("Accounts", Kind.ARTIFACTS, "TSK_ACCOUNT, TSK_SERVICE_ACCOUNT, TSK_WEB_ACCOUNT_TYPE"));
        c.groups.add(new Group("Location")
                .view("GPS points and routes", Kind.ARTIFACTS,
                        "TSK_GPS_TRACKPOINT, TSK_GPS_TRACK, TSK_GPS_ROUTE, TSK_GPS_BOOKMARK, TSK_GPS_LAST_KNOWN_LOCATION, TSK_GPS_SEARCH")
                .view("Photos with EXIF", Kind.ARTIFACTS, "TSK_METADATA_EXIF")
                .view("Wi-Fi networks", Kind.ARTIFACTS, "TSK_WIFI_NETWORK"));
        c.groups.add(new Group("Review")
                .view("Keyword hits", Kind.ARTIFACTS, "TSK_KEYWORD_HIT")
                .view("Hash set hits", Kind.ARTIFACTS, "TSK_HASHSET_HIT")
                .view("Interesting items", Kind.ARTIFACTS, "TSK_INTERESTING_FILE_HIT, TSK_INTERESTING_ARTIFACT_HIT, TSK_INTERESTING_ITEM")
                .view("Tagged items", Kind.TAGS, "*")
                .view("Larger than 100 MB", Kind.LARGER_THAN_MB, "100"));
        return c;
    }

    // ---- text format: "G\tname" and "V\tname\tKIND\tparam", fields URL-encoded ----

    String format() {
        StringBuilder out = new StringBuilder("# Autopsy Modern UI navigator\n");
        for (Group g : groups) {
            out.append("G\t").append(enc(g.name)).append('\n');
            for (View v : g.views) {
                out.append("V\t").append(enc(v.name)).append('\t').append(v.kind.name())
                        .append('\t').append(enc(v.param)).append('\n');
            }
        }
        return out.toString();
    }

    static NavigatorConfig parse(String text) {
        NavigatorConfig c = new NavigatorConfig();
        Group current = null;
        for (String line : text.split("\\r?\\n")) {
            String[] f = line.split("\t", -1);
            if (f.length >= 2 && "G".equals(f[0])) {
                current = new Group(dec(f[1]));
                c.groups.add(current);
            } else if (f.length >= 4 && "V".equals(f[0]) && current != null) {
                try {
                    current.views.add(new View(dec(f[1]), Kind.valueOf(f[2]), dec(f[3])));
                } catch (IllegalArgumentException ex) {
                    // Unknown kind from a newer version: skip it.
                }
            }
        }
        return c;
    }

    public String toText() {
        return format();
    }

    public static NavigatorConfig fromText(String text) {
        return parse(text);
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String dec(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
            return s;
        }
    }
}

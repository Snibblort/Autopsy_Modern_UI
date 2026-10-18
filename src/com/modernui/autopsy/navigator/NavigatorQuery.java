package com.modernui.autopsy.navigator;

import com.modernui.autopsy.navigator.NavigatorConfig.View;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardArtifactTag;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.ContentTag;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskData;

/**
 * Turns navigator views into read-only case queries. Nothing here writes to
 * the case database.
 */
public final class NavigatorQuery {

    private NavigatorQuery() {
    }

    /** Regular files only (no directories, slack space or virtual entries). */
    static String regularFiles() {
        return "meta_type = " + TskData.TSK_FS_META_TYPE_ENUM.TSK_FS_META_TYPE_REG.getValue()
                + " AND type != " + TskData.TSK_DB_FILES_TYPE_ENUM.SLACK.getFileType();
    }

    /** SQL WHERE clause on tsk_files for file-based views, or null for other kinds. */
    public static String fileWhere(View v, long nowSeconds) {
        switch (v.kind) {
            case EXTENSIONS: {
                List<String> exts = new ArrayList<>();
                for (String raw : v.param.split("[,;\\s]+")) {
                    String ext = raw.trim().toLowerCase(Locale.ROOT).replaceAll("^\\.", "").replaceAll("[^a-z0-9_+-]", "");
                    if (!ext.isEmpty()) {
                        exts.add("'" + ext + "'");
                    }
                }
                if (exts.isEmpty()) {
                    return regularFiles() + " AND 1 = 0";
                }
                return regularFiles() + " AND LOWER(extension) IN (" + String.join(", ", exts) + ")";
            }
            case MIME:
                return regularFiles() + " AND mime_type LIKE '" + v.param.trim().replace("'", "''") + "'";
            case MODIFIED_DAYS:
                return regularFiles() + " AND mtime >= " + (nowSeconds - positive(v.param, 7) * 86400L);
            case LARGER_THAN_MB:
                return regularFiles() + " AND size > " + positive(v.param, 100) * 1048576L;
            case DELETED:
                return regularFiles() + " AND dir_flags = " + TskData.TSK_FS_NAME_FLAG_ENUM.UNALLOC.getValue();
            case SQL:
                return v.param.trim().isEmpty() ? "1 = 0" : v.param.trim();
            default:
                return null;
        }
    }

    private static long positive(String text, long fallback) {
        try {
            long n = Long.parseLong(text.trim());
            return n > 0 ? n : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** Artifact type names listed in a view's parameter. */
    static List<String> artifactNames(View v) {
        List<String> names = new ArrayList<>();
        for (String raw : v.param.split("[,;\\s]+")) {
            String name = raw.trim().toUpperCase(Locale.ROOT);
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /** The artifact type with this name, or null if this case doesn't know it. */
    private static BlackboardArtifact.Type typeOrNull(SleuthkitCase skc, String name) {
        try {
            BlackboardArtifact.Type type = skc.getBlackboard().getArtifactType(name);
            if (type != null) {
                return type;
            }
        } catch (TskCoreException | RuntimeException ex) {
            // fall through: maybe the enum constant name was used instead of the type name
        }
        try {
            // e.g. TSK_RECENT_OBJECT (enum name) -> TSK_RECENT_OBJ (type name in the case)
            String label = BlackboardArtifact.ARTIFACT_TYPE.valueOf(name).getLabel();
            return label.equals(name) ? null : skc.getBlackboard().getArtifactType(label);
        } catch (TskCoreException | RuntimeException ex) {
            return null;
        }
    }

    /** How many items the view would show. */
    public static long count(Case c, View v) throws TskCoreException {
        SleuthkitCase skc = c.getSleuthkitCase();
        String where = fileWhere(v, System.currentTimeMillis() / 1000);
        if (where != null) {
            return skc.countFilesWhere(where);
        }
        if (v.kind == NavigatorConfig.Kind.ARTIFACTS) {
            long total = 0;
            for (String name : artifactNames(v)) {
                BlackboardArtifact.Type type = typeOrNull(skc, name);
                if (type != null) {
                    total += skc.getBlackboardArtifactsTypeCount(type.getTypeID());
                }
            }
            return total;
        }
        if (v.kind == NavigatorConfig.Kind.TAGS) {
            return fetchTags(c, v, Integer.MAX_VALUE).size();
        }
        return 0;
    }

    /**
     * The view's items (AbstractFile or BlackboardArtifact), at most {@code max}
     * plus one, so callers can tell the list was cut short.
     */
    public static List<Object> fetch(Case c, View v, int max) throws TskCoreException {
        SleuthkitCase skc = c.getSleuthkitCase();
        String where = fileWhere(v, System.currentTimeMillis() / 1000);
        if (where != null) {
            boolean hasLimit = where.toLowerCase(Locale.ROOT).matches("(?s).*\\blimit\\s+\\d+.*");
            String limited = hasLimit ? where : where + " LIMIT " + (max + 1);
            return new ArrayList<>(skc.findAllFilesWhere(limited));
        }
        List<Object> items = new ArrayList<>();
        if (v.kind == NavigatorConfig.Kind.ARTIFACTS) {
            for (String name : artifactNames(v)) {
                BlackboardArtifact.Type type = typeOrNull(skc, name);
                if (type == null) {
                    continue;
                }
                List<? extends BlackboardArtifact> found = type.getCategory() == BlackboardArtifact.Category.ANALYSIS_RESULT
                        ? skc.getBlackboard().getAnalysisResultsByType(type.getTypeID())
                        : skc.getBlackboard().getDataArtifacts(type.getTypeID());
                for (BlackboardArtifact a : found) {
                    items.add(a);
                    if (items.size() > max) {
                        return items;
                    }
                }
            }
        } else if (v.kind == NavigatorConfig.Kind.TAGS) {
            items.addAll(fetchTags(c, v, max + 1));
        }
        return items;
    }

    private static List<Object> fetchTags(Case c, View v, int max) throws TskCoreException {
        String wanted = v.param.trim();
        boolean all = wanted.isEmpty() || "*".equals(wanted);
        Set<Object> items = new LinkedHashSet<>();
        for (ContentTag tag : c.getServices().getTagsManager().getAllContentTags()) {
            if (all || tag.getName().getDisplayName().equalsIgnoreCase(wanted)) {
                Content content = tag.getContent();
                if (content instanceof AbstractFile) {
                    items.add(content);
                }
            }
            if (items.size() >= max) {
                return new ArrayList<>(items);
            }
        }
        for (BlackboardArtifactTag tag : c.getServices().getTagsManager().getAllBlackboardArtifactTags()) {
            if (all || tag.getName().getDisplayName().equalsIgnoreCase(wanted)) {
                items.add(tag.getArtifact());
            }
            if (items.size() >= max) {
                break;
            }
        }
        return new ArrayList<>(items);
    }
}

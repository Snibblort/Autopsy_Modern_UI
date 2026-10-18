package com.modernui.autopsy.navigator;

import java.lang.ref.WeakReference;
import java.util.List;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
import org.sleuthkit.autopsy.datamodel.BlackboardArtifactNode;
import org.sleuthkit.autopsy.datamodel.FileNode;
import org.sleuthkit.autopsy.directorytree.DataResultFilterNode;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 * Shows navigator results in Autopsy's own result viewer, using Autopsy's own
 * file and artifact nodes. That way the table, thumbnails, content viewers,
 * tagging and right-click actions all work exactly as they do for Autopsy's
 * tree.
 */
final class ResultsOpener {

    private static WeakReference<DataResultTopComponent> last = new WeakReference<>(null);

    private ResultsOpener() {
    }

    /** Opens (or replaces) the navigator results tab. Call on the Swing thread. */
    static void open(String groupName, String viewName, List<Object> items, boolean truncated, boolean newTab) {
        AbstractNode root = new AbstractNode(new ItemChildren(items),
                Lookups.singleton(new NavigatorResultInfo(groupName, viewName, items.size(), truncated)));
        root.setName(viewName);
        root.setDisplayName(viewName);

        String title = viewName + (truncated ? " (first " + items.size() + ")" : "");
        String path = "Navigator ▸ " + groupName + " ▸ " + viewName;

        DataResultTopComponent previous = last.get();
        if (!newTab && previous != null && previous.isOpened()) {
            previous.close();
        }
        DataResultTopComponent tc = DataResultTopComponent.createInstance(title, path,
                new TableFilterNode(new DataResultFilterNode(root), true), items.size());
        last = new WeakReference<>(tc);
        tc.requestActive();
    }

    /** One Autopsy node per file or artifact. */
    static final class ItemChildren extends Children.Keys<Object> {

        ItemChildren(List<Object> items) {
            super(true);
            setKeys(items);
        }

        @Override
        protected Node[] createNodes(Object key) {
            if (key instanceof AbstractFile) {
                return new Node[]{new FileNode((AbstractFile) key)};
            }
            if (key instanceof BlackboardArtifact) {
                return new Node[]{new BlackboardArtifactNode((BlackboardArtifact) key)};
            }
            return new Node[0];
        }
    }
}

package com.modernui.autopsy.navigator;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.openide.util.RequestProcessor;
import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
import org.sleuthkit.autopsy.datamodel.BlackboardArtifactNode;
import org.sleuthkit.autopsy.datamodel.FileNode;
import org.sleuthkit.autopsy.directorytree.DataResultFilterNode;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 * Opens navigator results in a DataResultTopComponent, built from Autopsy's
 * own FileNode/BlackboardArtifactNode so tagging, context menus and the
 * content viewers behave the same as for the main tree.
 */
final class ResultsOpener {

    private static final RequestProcessor RP = new RequestProcessor("Modern UI result selection", 1, true);
    private static final AtomicLong OPEN_SEQUENCE = new AtomicLong();
    private static WeakReference<DataResultTopComponent> last = new WeakReference<>(null);

    private ResultsOpener() {
    }

    /** Opens (or replaces) the navigator results tab. Call on the Swing thread. */
    static void open(String groupName, String viewName, String queryKind, String queryParameter,
            List<Object> items, boolean truncated, boolean newTab) {
        int files = 0;
        int artifacts = 0;
        for (Object item : items) {
            if (item instanceof AbstractFile) {
                files++;
            } else if (item instanceof BlackboardArtifact) {
                artifacts++;
            }
        }
        AbstractNode root = new AbstractNode(new ItemChildren(items),
                Lookups.singleton(new NavigatorResultInfo(groupName, viewName, queryKind, queryParameter,
                        items.size(), files, artifacts, truncated)));
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
        long sequence = OPEN_SEQUENCE.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (tc.isOpened() && sequence == OPEN_SEQUENCE.get()) {
                tc.requestActive();
            }
        });
        if (!items.isEmpty()) {
            RP.post(() -> {
                Node[] resultNodes = tc.getRootNode().getChildren().getNodes(true);
                if (resultNodes.length > 0) {
                    SwingUtilities.invokeLater(() -> {
                        if (tc.isOpened() && sequence == OPEN_SEQUENCE.get()) {
                            tc.setSelectedNodes(new Node[]{resultNodes[0]});
                            tc.requestActive();
                        }
                    });
                }
            });
        }
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

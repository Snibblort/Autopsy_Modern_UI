package com.modernui.autopsy.navigator;

final class NavigatorResultInfo {

    final String groupName;
    final String viewName;
    final String queryKind;
    final String queryParameter;
    final int itemCount;
    final int fileCount;
    final int artifactCount;
    final boolean truncated;

    NavigatorResultInfo(String groupName, String viewName, String queryKind, String queryParameter,
            int itemCount, int fileCount, int artifactCount, boolean truncated) {
        this.groupName = groupName;
        this.viewName = viewName;
        this.queryKind = queryKind;
        this.queryParameter = queryParameter;
        this.itemCount = itemCount;
        this.fileCount = fileCount;
        this.artifactCount = artifactCount;
        this.truncated = truncated;
    }
}

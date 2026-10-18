package com.modernui.autopsy.navigator;

final class NavigatorResultInfo {

    final String groupName;
    final String viewName;
    final int itemCount;
    final boolean truncated;

    NavigatorResultInfo(String groupName, String viewName, int itemCount, boolean truncated) {
        this.groupName = groupName;
        this.viewName = viewName;
        this.itemCount = itemCount;
        this.truncated = truncated;
    }
}

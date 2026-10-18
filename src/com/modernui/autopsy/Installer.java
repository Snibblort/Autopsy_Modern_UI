package com.modernui.autopsy;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.openide.modules.ModuleInstall;

/**
 * Hooks into Autopsy's startup.
 *
 * validate() runs before NetBeans picks its look and feel and before the
 * window system reads the saved layout, so the theme is requested there (every
 * window is built in FlatLaf from the start) and a layout broken by 2.0 is
 * repaired there.
 * restored() runs later and finishes the job on the Swing thread: it confirms
 * the theme is active (switching as a fallback if needed) and starts the
 * dark-mode color fixer.
 */
public class Installer extends ModuleInstall {

    @Override
    public void validate() throws IllegalStateException {
        if (!GraphicsEnvironment.isHeadless()) {
            com.modernui.autopsy.workspace.LayoutRepair.runOnce();
            ThemeApplier.requestStartupLookAndFeel();
        }
    }

    @Override
    public void restored() {
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(ThemeApplier::finishStartup);
            com.modernui.autopsy.ui.ToolbarCustomizer.applyWhenReady();
        }
    }
}

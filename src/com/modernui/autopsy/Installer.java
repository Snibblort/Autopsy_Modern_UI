package com.modernui.autopsy;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.openide.modules.ModuleInstall;
import org.openide.windows.WindowManager;

/**
 * validate() runs before NetBeans picks a look and feel and before the window
 * system loads the saved layout, so the theme and the 2.0 layout repair go
 * there. restored() finishes up on the EDT.
 */
public class Installer extends ModuleInstall {

    @Override
    public void validate() throws IllegalStateException {
        if (!GraphicsEnvironment.isHeadless()) {
            com.modernui.autopsy.workspace.LayoutRepair.runOnce();
            // Before NetBeans sets the look and feel, so the Cyber-Sigilism
            // controls are in place before the first window is built.
            com.modernui.autopsy.sigil.SigilControls.listen();
            ThemeApplier.requestStartupLookAndFeel();
        }
    }

    @Override
    public void restored() {
        if (!GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                ThemeApplier.finishStartup();
                // Covers the fallback path, where the theme is switched after startup.
                com.modernui.autopsy.sigil.SigilControls.sync();
                applyApplicationIcons();
            });
            com.modernui.autopsy.ui.ToolbarCustomizer.applyWhenReady();
        }
    }

    private static void applyApplicationIcons() {
        List<Image> icons = new ArrayList<>();
        for (String resource : new String[]{
            "/com/modernui/autopsy/options/mascot16.png",
            "/com/modernui/autopsy/options/mascot24.png",
            "/com/modernui/autopsy/options/mascot32.png"
        }) {
            java.net.URL imageUrl = Installer.class.getResource(resource);
            if (imageUrl != null) {
                icons.add(new ImageIcon(imageUrl).getImage());
            }
        }
        if (icons.isEmpty()) {
            throw new IllegalStateException("The application icon resources are missing.");
        }
        WindowManager.getDefault().getMainWindow().setIconImages(icons);
    }
}

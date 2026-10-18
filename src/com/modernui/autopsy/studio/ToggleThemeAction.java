package com.modernui.autopsy.studio;

import com.modernui.autopsy.ModernUISettings;
import com.modernui.autopsy.ModernUISettings.Theme;
import com.modernui.autopsy.ThemeApplier;
import com.modernui.autopsy.theme.PalettePreset;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.windows.WindowManager;

/**
 * One-click light/dark switch (View menu, Workspace toolbar, Ctrl+Shift+L).
 * Applies live; a palette is set aside in favor of the plain theme.
 */
@ActionID(category = "View", id = "com.modernui.autopsy.studio.ToggleTheme")
@ActionRegistration(displayName = "Toggle Light/Dark Theme", iconBase = "com/modernui/autopsy/options/theme_toggle.png")
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 910),
    @ActionReference(path = "Toolbars/Workspace", position = 200),
    @ActionReference(path = "Shortcuts", name = "DS-L")
})
public final class ToggleThemeAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        ModernUISettings s = ModernUISettings.getInstance();
        boolean dark = s.getEffectiveTheme().isDark();
        s.setPalette(PalettePreset.NONE);
        s.setTheme(dark ? Theme.LIGHT : Theme.DARK);
        s.setEnabled(true);
        try {
            ThemeApplier.applyLive();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
                    "Saved. Restart Autopsy to switch themes.", "Modern UI", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

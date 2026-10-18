package com.modernui.autopsy.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * Registers the Modern UI tab in Tools > Options.
 */
@Messages({
    "ModernUIOptionsPanelController.displayName=Modern UI"
})
@OptionsPanelController.TopLevelRegistration(
        categoryName = "#ModernUIOptionsPanelController.displayName",
        iconBase = "com/modernui/autopsy/options/modernui_icon.png",
        position = 1900
)
public final class ModernUIOptionsPanelController extends OptionsPanelController {

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private ModernUIOptionsPanel panel;
    private boolean changed;

    @Override
    public void update() {
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        getPanel().store();
        changed = false;
    }

    @Override
    public void cancel() {
        // Nothing is saved until applyChanges().
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    /** Called by the panel whenever a control changes. */
    void changed() {
        boolean now = getPanel().isChanged();
        if (now != changed) {
            changed = now;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, !now, now);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

    private ModernUIOptionsPanel getPanel() {
        if (panel == null) {
            panel = new ModernUIOptionsPanel(this);
        }
        return panel;
    }
}

package com.modernui.autopsy.sigil;

import com.formdev.flatlaf.FlatLaf;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the classes in sigil.flat, which extend FlatLaf's UI delegates.
 *
 * NetBeans only exports com.formdev.flatlaf and com.formdev.flatlaf.util
 * from its FlatLaf module, so this module's own class loader refuses to link
 * anything that extends com.formdev.flatlaf.ui classes. This loader defines
 * those few classes itself with FlatLaf's loader as parent (which can see the
 * ui package), and hands every other com.modernui class back to the module
 * loader so there is still exactly one copy of Sigil, SigilArt and friends.
 */
final class FlatBridge extends ClassLoader {

    private static final String BRIDGED = "com.modernui.autopsy.sigil.flat.";
    private static FlatBridge instance;

    private final ClassLoader module = FlatBridge.class.getClassLoader();

    private FlatBridge(ClassLoader flatLafLoader) {
        super(flatLafLoader);
    }

    static synchronized Class<?> load(String name) throws ClassNotFoundException {
        if (instance == null) {
            instance = new FlatBridge(FlatLaf.class.getClassLoader());
        }
        return instance.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (name.startsWith(BRIDGED)) {
                    byte[] bytes = read(name);
                    c = defineClass(name, bytes, 0, bytes.length, FlatBridge.class.getProtectionDomain());
                } else if (name.startsWith("com.modernui.")) {
                    c = module.loadClass(name);
                } else {
                    c = getParent().loadClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private byte[] read(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        try (InputStream in = module.getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0;) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }
    }
}

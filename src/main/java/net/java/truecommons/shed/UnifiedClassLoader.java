/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

/** @author Christian Schlichtherle */
final class UnifiedClassLoader extends ClassLoader {

    private final ClassLoader secondary;

    static ClassLoader resolve(
            final ClassLoader primary,
            final ClassLoader secondary) {
        assert null != primary;
        assert null != secondary;
        if (primary == secondary || isChildOf(primary, secondary))
            return primary;
        if (isChildOf(secondary, primary))
            return secondary;
        class NewUnifiedClassLoader implements PrivilegedAction<ClassLoader> {
            @Override
            public ClassLoader run() {
                return new UnifiedClassLoader(primary, secondary);
            }
        }
        return AccessController.doPrivileged(new NewUnifiedClassLoader());
    }

    private static boolean isChildOf(ClassLoader c, final ClassLoader r) {
        for (ClassLoader p; null != (p = c.getParent()); c = p)
            if (p == r) return true;
        return false;
    }

    private UnifiedClassLoader(
            final ClassLoader primary,
            final ClassLoader secondary) {
        super(primary);
        this.secondary = secondary;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return secondary.loadClass(name);
    }

    @Override
    protected URL findResource(String name) {
        return secondary.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return secondary.getResources(name);
    }
}

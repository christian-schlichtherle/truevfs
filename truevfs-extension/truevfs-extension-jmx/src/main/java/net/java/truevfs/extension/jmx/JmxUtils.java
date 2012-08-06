/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.*;

/**
 * Provides utility methods for JMX.
 * 
 * @author Christian Schlichtherle
 */
final class JmxUtils {
    private static final MBeanServer mbs =
            ManagementFactory.getPlatformMBeanServer();

    static void register(final Object mbean, final ObjectName name) {
        try {
            try {
                mbs.registerMBean(mbean, name);
            } catch (InstanceAlreadyExistsException ignored) {
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static void unregister(final ObjectName name) {
        try {
            try {
                mbs.unregisterMBean(name);
            } catch (InstanceNotFoundException ignored) {
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    static Set<ObjectName> queryNames(final ObjectName name) {
        return mbs.queryNames(name, null);
    }

    static <T> T newMXBeanProxy(final ObjectName name, Class<T> clazz) {
        return JMX.newMBeanProxy(mbs, name, clazz);
    }

    private JmxUtils() { }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.*;

/**
 * Provides utility methods for JMX.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class JmxUtils {
    private static final MBeanServer mbs =
            ManagementFactory.getPlatformMBeanServer();

    public static boolean register(final ObjectName name, final Object mbean) {
        try {
            mbs.registerMBean(mbean, name);
            return true;
        } catch (InstanceAlreadyExistsException ignored) {
            return false;
        } catch (JMException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static boolean deregister(final ObjectName name) {
        try {
            mbs.unregisterMBean(name);
            return true;
        } catch (InstanceNotFoundException ignored) {
            return false;
        } catch (JMException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static Set<ObjectName> query(ObjectName name) {
        return mbs.queryNames(name, null);
    }

    public static <T> T proxy(ObjectName name, Class<T> clazz) {
        return JMX.newMBeanProxy(mbs, name, clazz);
    }

    private JmxUtils() { }
}

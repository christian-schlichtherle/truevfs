/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import net.java.truevfs.comp.jmx.sl.MBeanServerLocator;

/**
 * Provides utility methods for JMX.
 * The utility methods use the {@link MBeanServerLocator#SINGLETON} to obtain
 * the MBean server to use.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class JmxUtils {

    private static final MBeanServer mbs = MBeanServerLocator.SINGLETON.get();

    /**
     * Maps the given object {@code name} to the given {@code mbean}
     * in the configured MBean server.
     * 
     * @param  name the object name.
     * @param  mbean the MBean.
     * @return {@code true} if the MBean has been successfully registered
     *         with the given {@code name}.
     *         {@code false} if any MBean was already registered
     *         with the given {@code name}.
     * @throws IllegalArgumentException if registering the MBean failed with an
     *         {@link JMException}.
     */
    public static boolean register(
            final @CheckForNull ObjectName name,
            final Object mbean) {
        try {
            mbs.registerMBean(mbean, name);
            return true;
        } catch (InstanceAlreadyExistsException ignored) {
            return false;
        } catch (JMException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes any MBean with the given object {@code name}
     * in the configured MBean server.
     * 
     * @param  name the object name.
     * @return {@code true} if any MBean has been successfully removed
     *         with the given {@code name}.
     *         {@code false} if no MBean was registered
     *         with the given {@code name}.
     * @throws IllegalArgumentException if removing an MBean failed with an
     *         {@link JMException}.
     */
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

    /**
     * Creates a proxy object for the MBean with the given object {@code name}.
     * If no MBean is registered with the given object {@code name} in the
     * configured MBean server, then any subsequent attempt to access the
     * returned proxy object will fail with a {@link RuntimeException}.
     * 
     * @param  <T> the interface type of the proxy object.
     * @param  name the object name.
     * @param  type the interface class of the proxy object.
     * @return a new proxy object for the MBean with the given object
     *         {@code name}.
     */
    public static <T> T proxy(ObjectName name, Class<T> type) {
        return JMX.newMBeanProxy(mbs, name, type);
    }

    public static Set<ObjectName> query(
            @CheckForNull ObjectName name,
            @CheckForNull QueryExp query) {
        return mbs.queryNames(name, query);
    }

    private JmxUtils() { }
}

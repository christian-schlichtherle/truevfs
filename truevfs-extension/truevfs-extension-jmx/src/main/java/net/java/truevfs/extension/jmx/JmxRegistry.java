/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A registry for JMX views.
 * 
 * @author Christian Schlichtherle
 */
public class JmxRegistry {
    private static final MBeanServer
            mbs = ManagementFactory.getPlatformMBeanServer();

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

    private JmxRegistry() { }
}

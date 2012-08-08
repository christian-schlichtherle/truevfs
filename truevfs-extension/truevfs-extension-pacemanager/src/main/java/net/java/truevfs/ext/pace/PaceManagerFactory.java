/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import net.java.truevfs.kernel.spec.FsManager;

/**
 * Decorates a given file system manager with a pace manager.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class PaceManagerFactory {

    /**
     * Returns a new MXBean proxy for the singleton pace manager which
     * interfaces with JMX.
     * Each proxy can get used immediately but will effectively refer to the
     * singleton pace manager only once it has been
     * {@linkplain #apply installed}.
     * 
     * @return A new MXBean proxy for the singleton pace manager which
     *         interfaces with JMX.
     */
    public static PaceManagerMXBean newMXBeanProxy() {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return JMX.newMXBeanProxy(mbs, Lazy.getObjectName(), PaceManagerMXBean.class);
    }

    static final class Lazy {
        static final PaceManagerView
                view = new PaceManagerView(new PaceManagerModel());

        static {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.registerMBean(view, getObjectName());
            } catch (final MBeanRegistrationException | NotCompliantMBeanException | InstanceAlreadyExistsException ex) {
                throw new AssertionError(ex);
            }
        }

        static ObjectName getObjectName() {
            try {
                return new ObjectName(
                        PaceManagerFactory.class.getPackage().getName(),
                        "type", FsManager.class.getSimpleName());
            } catch (MalformedObjectNameException ex) {
                throw new AssertionError(ex);
            }
        }
    } // Lazy

    private PaceManagerFactory() { }
}

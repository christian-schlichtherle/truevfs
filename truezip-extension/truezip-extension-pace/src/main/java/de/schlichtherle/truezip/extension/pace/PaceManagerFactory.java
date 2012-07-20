/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.fs.spi.FsManagerDecorator;
import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.*;

/**
 * Decorates a given file system manager with a pace manager.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class PaceManagerFactory extends FsManagerDecorator {

    /**
     * Returns a new MXBean proxy for the singleton pace manager which
     * interfaces with JMX.
     * Each proxy can get used immediately but will effectively refer to the
     * singleton pace manager only once it has been
     * {@linkplain #decorate installed}.
     * 
     * @return A new MXBean proxy for the singleton pace manager which
     *         interfaces with JMX.
     */
    public static PaceManager newMXBeanProxy() {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        return JMX.newMXBeanProxy(mbs, Lazy.getObjectName(), PaceManager.class);
    }

    /**
     * Decorates the given file system manager with a new pace manager.
     * Upon the first call only, the new pace manager gets installed as the
     * singleton pace manager which interfaces with JMX.
     * 
     * @param      manager the file system manager to decorate.
     * @return     the decorated file system manager.
     * @deprecated This method is reserved for exclusive use by the
     *             {@link FsManagerLocator#SINGLETON}!
     *             Call {@link #newMXBeanProxy} instead to refer to the single
     *             pace manager which interfaces with JMX.
     *             
     */
    @Deprecated
    @Override
    public FsManager decorate(FsManager manager) {
        return Lazy.view.decorate(manager);
    }

    private static final class Lazy {
        static final PaceManagerView
                view = new PaceManagerView(new PaceManagerModel());

        static {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.registerMBean(view, getObjectName());
            } catch (final MBeanRegistrationException ex) {
                throw new AssertionError(ex);
            } catch (final NotCompliantMBeanException ex) {
                throw new AssertionError(ex);
            } catch (InstanceAlreadyExistsException ex) {
                throw new AssertionError(ex);
            }
        }

        static ObjectName getObjectName() {
            try {
                return new ObjectName(  PaceManager.class.getPackage().getName(),
                                        "type", PaceManager.class.getSimpleName());
            } catch (MalformedObjectNameException ex) {
                throw new AssertionError(ex);
            }
        }
    } // Boot
}

/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.mmbs;

import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.jmx.sl.ObjectNameModifierLocator;
import net.java.truecommons.jmx.spi.MBeanServerDecorator;

/**
 * Decorates the given MBean server with a {@link MultiplexingMBeanServer} if
 * and only if this class is <em>not</em> defined by the system class loader.
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation(priority = -100)
public final class MultiplexingMBeanServerDecorator implements MBeanServerDecorator {

    @Override
    public MBeanServer apply(final MBeanServer mbs) {
        // Iff this code is running in a container like Tomcat where each app
        // is loaded by a different class loader, then multiplex the object
        // instances by modifying their object names in a unique fashion.
        // This allows multiple container apps to register their object
        // instances with an object name which would otherwise be equal.
        return getClass().getClassLoader() != ClassLoader.getSystemClassLoader()
                ? new MultiplexingMBeanServer(mbs, ObjectNameModifierLocator.SINGLETON.get())
                : mbs;
    }
}

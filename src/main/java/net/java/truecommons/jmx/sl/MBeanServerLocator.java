/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.sl;

import global.namespace.service.wight.core.ServiceLocator;
import net.java.truecommons.jmx.spi.MBeanServerDecorator;
import net.java.truecommons.jmx.spi.MBeanServerProvider;

import javax.management.MBeanServer;
import java.util.function.Supplier;

/**
 * A container of the singleton MBean server.
 * The MBean server is created by using a {@link ServiceLocator} to search for
 * advertised implementations of the factory service specification class
 * {@link MBeanServerProvider}
 * and the decorator service specification class
 * {@link MBeanServerDecorator}.
 *
 * @author Christian Schlichtherle
 */
public final class MBeanServerLocator implements Supplier<MBeanServer> {

    /** The singleton instance of this class. */
    public static final MBeanServerLocator SINGLETON = new MBeanServerLocator();

    private MBeanServerLocator() { }

    @Override
    public MBeanServer get() { return Lazy.mbs; }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final MBeanServer mbs =
                new ServiceLocator().provider(MBeanServerProvider.class, MBeanServerDecorator.class).get();
    }
}

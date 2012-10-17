/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx.sl;

import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import net.java.truecommons.services.Container;
import net.java.truecommons.services.Locator;
import net.java.truevfs.comp.jmx.spi.MBeanServerDecorator;
import net.java.truevfs.comp.jmx.spi.MBeanServerProvider;

/**
 * A container of the singleton MBean server.
 * The MBean server is created by using a {@link Locator} to search for
 * advertised implementations of the factory service specification class
 * {@link MBeanServerProvider}
 * and the decorator service specification class
 * {@link MBeanServerDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class MBeanServerLocator implements Container<MBeanServer> {

    /** The singleton instance of this class. */
    public static final MBeanServerLocator SINGLETON = new MBeanServerLocator();

    private MBeanServerLocator() { }

    @Override
    public MBeanServer get() { return Lazy.mbs; }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final MBeanServer mbs
                = new Locator(MBeanServerLocator.class)
                .container(MBeanServerProvider.class, MBeanServerDecorator.class)
                .get();
    }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx.spi;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truecommons.services.LocatableProvider;
import net.java.truevfs.comp.jmx.sl.MBeanServerLocator;

/**
 * A service which provides an MBean server.
 * Provider services are subject to service location by the
 * {@link MBeanServerLocator#SINGLETON}.
 * <p>
 * If multiple provider services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 * <p>
 * The implementation in this class simply provides the
 * {@linkplain ManagementFactory#getPlatformMBeanServer() platform MBean server}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceSpecification
@ServiceImplementation
public class MBeanServerProvider extends LocatableProvider<MBeanServer> {

    @Override
    public MBeanServer get() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link MBeanServerProvider} returns
     * {@code -100} if and only if it equals its
     * {@linkplain #getClass() runtime class} or zero otherwise.
     * In simple terms, if this method gets called on an object of a subclass
     * then it will return zero unless it has been overridden.
     */
    @Override
    public int getPriority() {
        return MBeanServerProvider.class.equals(getClass()) ? -100 : 0;
    }
}

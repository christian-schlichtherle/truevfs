/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.spi;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truecommons.jmx.sl.MBeanServerLocator;
import net.java.truecommons.services.LocatableProvider;

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
 * @since  TrueCommons 2.3
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
     * If the {@linkplain #getClass() runtime class} of this object is
     * {@link MBeanServerProvider}, then {@code -100} gets returned.
     * Otherwise, zero gets returned.
     */
    @Override
    public int getPriority() {
        return MBeanServerProvider.class.equals(getClass()) ? -100 : 0;
    }
}

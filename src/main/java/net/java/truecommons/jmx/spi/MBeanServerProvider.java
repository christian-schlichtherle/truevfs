/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import net.java.truecommons.jmx.sl.MBeanServerLocator;

import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

/**
 * A service which provides an MBean server.
 * Provider services are subject to service location by the
 * {@link MBeanServerLocator#SINGLETON}.
 * <p>
 * If multiple provider services are locatable on the class path at run time, the service with the greatest
 * {@linkplain ServiceImplementation#priority()} gets selected.
 * <p>
 * The implementation in this class simply provides the
 * {@linkplain ManagementFactory#getPlatformMBeanServer() platform MBean server}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceInterface
@ServiceImplementation(priority = -100)
public class MBeanServerProvider implements Supplier<MBeanServer> {

    @Override
    public MBeanServer get() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}

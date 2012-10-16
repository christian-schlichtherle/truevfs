/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx.spi;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanServer;
import net.java.truecommons.services.ProviderService;
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
 * <p>
 * Implementations should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class MBeanServerProvider extends ProviderService<MBeanServer> {

    @Override
    public MBeanServer get() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx.spi;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanServer;
import net.java.truecommons.services.ProviderService;
import net.java.truevfs.comp.jmx.sl.MBeanServerLocator;

/**
 * An abstract service which provides MBean servers.
 * Provider services are subject to service location by the
 * {@link MBeanServerLocator#SINGLETON}.
 * <p>
 * If multiple provider services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class MBeanServerProvider extends ProviderService<MBeanServer> {
}

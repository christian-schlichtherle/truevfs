/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import net.java.truecommons.services.Container;
import net.java.truevfs.comp.jmx.spi.MBeanServerProvider;

/**
 * Contains the
 * {@linkplain ManagementFactory#getPlatformMBeanServer() platform MBean server}.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxPlatformMBeanServerContainer
extends MBeanServerProvider implements Container<MBeanServer> {

    @Override
    public MBeanServer get() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /** Returns -100. */
    @Override
    public int getPriority() { return -100; }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import java.util.Comparator;
import net.truevfs.kernel.spec.spi.ServiceProvider;

/**
 * Compares {@link ServiceProvider}s.
 * 
 * @author Christian Schlichtherle
 */
final class ServiceProviderComparator implements Comparator<ServiceProvider> {
    @Override
    public int compare(ServiceProvider o1, ServiceProvider o2) {
        return o1.getPriority() - o2.getPriority();
    }
}

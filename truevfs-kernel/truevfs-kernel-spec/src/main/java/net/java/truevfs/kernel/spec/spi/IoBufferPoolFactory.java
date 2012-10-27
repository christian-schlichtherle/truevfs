/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.services.LocatableFactory;
import net.java.truecommons.services.annotations.ServiceSpecification;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;

/**
 * An abstract service for creating I/O buffer pools.
 * Factory services are subject to service location by the
 * {@link IoBufferPoolLocator#SINGLETON}.
 * <p>
 * If multiple factory services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 *
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class IoBufferPoolFactory
extends LocatableFactory<IoBufferPool> {
}

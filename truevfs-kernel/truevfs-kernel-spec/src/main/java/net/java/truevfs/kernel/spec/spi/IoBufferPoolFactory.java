/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.services.LocatableFactory;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.sl.IoBufferPoolLocator;

/**
 * An abstract service for creating I/O buffer pools.
 * Factory services are subject to service location by the
 * {@link IoBufferPoolLocator#SINGLETON}.
 * <p>
 * If multiple factory services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 * <p>
 * Implementations should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class IoBufferPoolFactory
extends LocatableFactory<IoBufferPool> {
}

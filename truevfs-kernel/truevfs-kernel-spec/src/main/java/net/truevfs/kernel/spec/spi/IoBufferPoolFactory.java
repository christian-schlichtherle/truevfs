/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.java.truecommons.services.FactoryService;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.sl.IoBufferPoolLocator;

/**
 * An abstract service for creating I/O buffer pools.
 * Factory services are subject to service location by the
 * {@link IoBufferPoolLocator#SINGLETON}.
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
extends FactoryService<IoBufferPool<? extends IoBuffer<?>>> {
}

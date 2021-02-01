/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.spec.sl.IoBufferPoolLocator;

import java.util.function.Supplier;

/**
 * A service for creating I/O buffer pools.
 * Subclasses annotated with {@link ServiceImplementation} are subject to service location by the
 * {@link IoBufferPoolLocator#SINGLETON}.
 * <p>
 * If multiple factory services are located on the class path at run time, the service with the greatest
 * {@linkplain ServiceImplementation#priority()} gets selected.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface IoBufferPoolFactory extends Supplier<IoBufferPool> {
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;

/**
 * @deprecated This class is reserved for exclusive use by the
 *             {@link FsManagerLocator#SINGLETON}!
 * @author Christian Schlichtherle
 */
@Deprecated
@Immutable
public final class I5tBufferPoolDecorator extends IoBufferPoolDecorator {
    @Override
    public IoBufferPool apply(IoBufferPool pool) {
        return I5tMediator.get().instrument(pool);
    }
}
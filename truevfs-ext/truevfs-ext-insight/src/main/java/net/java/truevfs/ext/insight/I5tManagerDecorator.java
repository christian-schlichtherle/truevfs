/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

import static net.java.truevfs.ext.insight.I5tMediators.syncOperationsMediator;

/**
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the {@link net.java.truevfs.kernel.spec.sl.FsManagerLocator}
 * singleton!
 */
@Deprecated(since = "1")
public final class I5tManagerDecorator extends FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return syncOperationsMediator.instrument(manager);
    }

    /**
     * Returns {@code -200}.
     */
    @Override
    public int getPriority() {
        return -200;
    }
}

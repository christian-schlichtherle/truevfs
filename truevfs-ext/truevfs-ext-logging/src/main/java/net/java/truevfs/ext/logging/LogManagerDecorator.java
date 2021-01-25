/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the {@link net.java.truevfs.kernel.spec.sl.FsManagerLocator}
 * singleton!
 */
@Deprecated
public final class LogManagerDecorator extends FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return LogMediator.SINGLETON.instrument(manager);
    }

    /**
     * Returns {@code -300}.
     */
    @Override
    public int getPriority() {
        return -300;
    }
}

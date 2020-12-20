/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker;

import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 * @deprecated This class is reserved for exclusive use by the {@link net.java.truevfs.kernel.spec.sl.FsManagerLocator}
 * singleton!
 */
@Deprecated(since = "1")
public final class PaceManagerDecorator extends FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return new PaceMediator().instrument(manager);
    }

    /**
     * Returns {@code -100}.
     */
    @Override
    public int getPriority() {
        return -100;
    }
}

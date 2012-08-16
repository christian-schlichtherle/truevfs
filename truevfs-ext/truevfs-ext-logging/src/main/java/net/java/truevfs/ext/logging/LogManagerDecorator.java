/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @deprecated This class is reserved for exclusive use by the
 *             {@link FsManagerLocator#SINGLETON}!
 * @author Christian Schlichtherle
 */
@Deprecated
@Immutable
public final class LogManagerDecorator extends FsManagerDecorator {
    @Override
    public FsManager apply(FsManager manager) {
        return LogMediator.SINGLETON.instrument(manager);
    }

    /** Returns -100. */
    @Override
    public int getPriority() {
        return -100;
    }
}

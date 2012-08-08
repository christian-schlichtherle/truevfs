/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @deprecated This class is reserved for exclusive use by the
 *             {@link FsManagerLocator#SINGLETON}!
 * @author Christian Schlichtherle
 */
@Deprecated
@Immutable
public final class PaceManagerDecorator extends FsManagerDecorator {
    @Override
    public FsManager apply(FsManager manager) {
        return PaceManagerFactory.Lazy.view.decorate(manager);
    }

    /** Returns 100. */
    @Override
    public int getPriority() {
        return 100;
    }
}

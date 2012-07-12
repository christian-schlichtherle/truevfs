/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.comp;

import javax.annotation.concurrent.Immutable;
import net.truevfs.ext.jmx.JmxDirector;
import net.truevfs.ext.jul.JulDirector;
import net.truevfs.kernel.impl.DefaultManager;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeManagerService extends FsManagerFactory {

    private final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new DefaultManager()));

    @Override
    public FsManager manager() {
        return manager;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}

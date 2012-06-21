/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.jmx.JmxDirector;
import net.truevfs.extension.jmxjul.jul.JulDirector;
import net.truevfs.kernel.impl.ArchiveManagerService;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.spi.FsManagerService;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeManagerService extends FsManagerService {

    private final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new ArchiveManagerService().getManager()));

    @Override
    public FsManager getManager() {
        return manager;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}

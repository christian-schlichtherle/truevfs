/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.schlichtherle.truezip.kernel.se.ArchiveManagerService;
import de.truezip.extension.jmxjul.jmx.JmxDirector;
import de.truezip.extension.jmxjul.jul.JulDirector;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

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

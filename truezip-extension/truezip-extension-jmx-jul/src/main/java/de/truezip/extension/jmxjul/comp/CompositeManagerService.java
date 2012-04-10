/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.comp;

import de.schlichtherle.truezip.kernel.FailSafeManagerService;
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
                    new FailSafeManagerService().getManager()));

    @Override
    public FsManager getManager() {
        return manager;
    }
}

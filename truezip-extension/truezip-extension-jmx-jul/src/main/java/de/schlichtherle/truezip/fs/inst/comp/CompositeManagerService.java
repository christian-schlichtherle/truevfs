/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.comp;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsFailSafeManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.inst.jmx.JmxDirector;
import de.schlichtherle.truezip.fs.inst.jul.JulDirector;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class CompositeManagerService extends FsManagerService {

    private static final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new FsFailSafeManager(
                        new FsDefaultManager())));

    @Override
    public FsManager get() {
        return manager;
    }

    /**
     * Returns 100.
     * 
     * @return 100.
     */
    @Override
    public int getPriority() {
        return 100;
    }
}

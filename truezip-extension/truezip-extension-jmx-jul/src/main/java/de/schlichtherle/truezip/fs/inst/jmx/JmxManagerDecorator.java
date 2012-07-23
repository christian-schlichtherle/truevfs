/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.spi.FsManagerDecorator;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JmxManagerDecorator extends FsManagerDecorator {
    @Override
    public FsManager decorate(FsManager manager) {
        return JmxDirector.SINGLETON.instrument(manager);
    }

    /** Returns 100. */
    @Override
    public int getPriority() {
        return 100;
    }
}
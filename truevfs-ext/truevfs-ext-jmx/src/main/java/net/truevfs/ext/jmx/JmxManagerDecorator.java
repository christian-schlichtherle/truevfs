/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jmx;

import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.spi.FsManagerDecorator;

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

/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * Creates a default file system manager.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class DefaultManagerFactory implements FsManagerFactory {

    @Override
    public FsManager get() {
        return new DefaultManager();
    }
}

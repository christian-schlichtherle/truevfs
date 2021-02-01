/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.spi.FsManagerFactory;

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

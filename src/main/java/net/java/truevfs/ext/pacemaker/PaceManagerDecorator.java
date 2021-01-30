/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class PaceManagerDecorator implements FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return new PaceMediator().instrument(manager);
    }
}

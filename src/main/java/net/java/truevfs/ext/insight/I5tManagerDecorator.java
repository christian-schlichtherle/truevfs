/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;

import static net.java.truevfs.ext.insight.I5tMediators.syncOperationsMediator;

/**
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -200)
public final class I5tManagerDecorator implements FsManagerDecorator {

    @Override
    public FsManager apply(FsManager manager) {
        return syncOperationsMediator.instrument(manager);
    }
}

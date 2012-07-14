/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.pace;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.spi.FsManagerDecorator;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class PaceManagerDecorator extends FsManagerDecorator {
    @Override
    public FsManager decorate(final FsManager manager) {
        final Logger logger = Logger.getLogger(PaceManagerDecorator.class.getName());
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final PaceManager paceManager = new PaceManager(manager);
        try {
            mbs.registerMBean(paceManager,
                    new ObjectName(PaceManager.class.getPackage().getName(),
                        "type", PaceManager.class.getSimpleName()));
            return paceManager;
        } catch (final MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
            logger.log(Level.WARNING, null, ex);
            return manager;
        }
    }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class PaceManagerService extends FsManagerService {

    @Override
    public PaceManager get() {
        return Boot.MANAGER;
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

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final PaceManager MANAGER;
        static {
            final Logger logger = Logger.getLogger(
                    PaceManagerService.class.getName());
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final PaceManager manager = new PaceManager(new FsDefaultManager());
            try {
                mbs.registerMBean(manager,
                        new ObjectName(PaceManager.class.getPackage().getName(),
                            "type", PaceManager.class.getSimpleName()));
            } catch (final Exception ex) {
                logger.log(Level.WARNING, null, ex);
            }
            MANAGER = manager;
        }
    } // Boot
}

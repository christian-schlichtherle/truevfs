/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker;

import lombok.val;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truevfs.comp.jmx.JmxManager;
import net.java.truevfs.kernel.spec.*;
import org.slf4j.Logger;

import java.util.Set;

/**
 * A pace manager.
 *
 * @author Christian Schlichtherle
 */
public class PaceManager extends JmxManager<PaceMediator> {

    private static final Logger logger = new LocalizedLogger(PaceManager.class);

    private final LruCache<FsMountPoint> cachedMountPoints;
    private final Set<FsMountPoint> evictedMountPoints;

    public PaceManager(PaceMediator mediator, FsManager manager) {
        super(mediator, manager);
        this.cachedMountPoints = mediator.cachedMountPoints;
        this.evictedMountPoints = mediator.evictedMountPoints;
    }

    int getMaximumSize() {
        return mediator.getMaximumSize();
    }

    void setMaximumSize(int maximumSize) {
        mediator.setMaximumSize(maximumSize);
    }

    @Override
    protected Object newView() {
        return new PaceManagerView(this);
    }

    /**
     * Records access to a file system after the fact and tries to unmount the
     * least-recently accessed file systems which exceed the maximum number
     * of mounted file systems.
     * A file system is never unmounted if there any open streams or channels
     * associated with it or if any of its child file systems is mounted.
     *
     * @param mountPoint the mount point of the accessed file system.
     */
    public void recordAccess(FsMountPoint mountPoint) throws FsSyncException {
        cachedMountPoints.recordAccess(mountPoint);
        unmountEvictedArchiveFileSystems();
    }

    private void unmountEvictedArchiveFileSystems() throws FsSyncException {
        val iterator = evictedMountPoints.iterator();
        if (iterator.hasNext()) {
            val builder = new FsSyncExceptionBuilder();
            do {
                val evictedMountPoint = iterator.next();
                val evictedMountPointFilter = FsPrefixMountPointFilter.forPrefix(evictedMountPoint);
                // Check that neither the evicted file system nor any of its child file systems are actually mounted:
                if (!(cachedMountPoints.exists(evictedMountPointFilter::accept))) {
                    try {
                        new FsSync()
                                .manager(manager)
                                .filter(FsControllerFilter.forPrefix(evictedMountPoint))
                                .run();
                        iterator.remove();
                    } catch (FsSyncException e) {
                        if (e.getCause() instanceof FsOpenResourceException) {
                            // Do NOT remove evicted controller - the sync shall get retried at the next call to this
                            // method:
                            //iterator.remove();

                            // This is pretty much a normal situation, so just log the exception at the TRACE level:
                            logger.trace("ignoring", e);
                        } else {
                            // Prevent retrying this operation - it would most likely yield the same result:
                            iterator.remove();

                            // Mark the exception for subsequent rethrowing at the end of this method:
                            builder.warn(e);
                        }
                    }
                }
            } while (iterator.hasNext());
            builder.check();
        }
    }
}

/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker;

import net.java.truevfs.comp.inst.InstrumentingCompositeDriver;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.jmx.JmxMediator;
import net.java.truevfs.kernel.spec.*;

import java.util.Set;

import static java.lang.Math.max;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with a {@link PaceManager} and a {@link PaceController}.
 *
 * @author Christian Schlichtherle
 */
final class PaceMediator extends JmxMediator<PaceMediator> {

    /**
     * The name of the system property which determines the initial maximum number of mounted archive file systems.
     */
    private static final String maximumFileSystemsMountedPropertyKey =
            PaceMediator.class.getPackage().getName() + ".maximumFileSystemsMounted";

    /**
     * The minimum value for the maximum number of mounted archive file systems.
     * This value must not be less than two or otherwise you couldn't even copy entries from one archive file to
     * another.
     * Well, actually you could because the pace manager doesn't unmount archive file systems with open streams or
     * channels, but let's play it safe and pretend it would.
     */
    private static final int maximumFileSystemsMountedMinimumValue = 2;

    /**
     * The default value of the system property which determines the initial maximum number of mounted archive file
     * systems.
     * The value of this constant will be set to {@link #maximumFileSystemsMountedMinimumValue} unless a system
     * property with the key string {@link #maximumFileSystemsMountedPropertyKey} is set to a value which is greater
     * than {@link #maximumFileSystemsMountedMinimumValue}.
     */
    private static final int maximumFileSystemsMountedDefaultValue = max(
            maximumFileSystemsMountedMinimumValue,
            Integer.getInteger(maximumFileSystemsMountedPropertyKey, maximumFileSystemsMountedMinimumValue)
    );

    final LruCache<FsMountPoint> cachedMountPoints = new LruCache<>(maximumFileSystemsMountedDefaultValue);
    final Set<FsMountPoint> evictedMountPoints = cachedMountPoints.getEvictedView();

    int getMaximumSize() {
        return cachedMountPoints.getMaximumSize();
    }

    void setMaximumSize(final int maximumSize) {
        if (maximumFileSystemsMountedMinimumValue > maximumSize) {
            throw new IllegalArgumentException();
        }
        cachedMountPoints.setMaximumSize(maximumSize);
    }

    @Override
    public FsManager instrument(FsManager subject) {
        return activate(new PaceManager(this, subject));
    }

    @Override
    public FsCompositeDriver instrument(InstrumentingManager<PaceMediator> context, FsCompositeDriver subject) {
        return new InstrumentingCompositeDriver<>(this, subject);
    }

    // FIXME: Shouldn't the first parameter be the InstrumentingCompositeDriver?
    // So then the given controller should get decorated and memoized only once instead of decorating it time and time
    // again when there is a controller lookup.
    @Override
    public FsController instrument(InstrumentingManager<PaceMediator> context, FsController subject) {
        return new PaceController((PaceManager) context, subject);
    }

    @Override
    public FsModel instrument(InstrumentingCompositeDriver<PaceMediator> context, FsModel subject) {
        return new PaceModel(this, subject);
    }
}

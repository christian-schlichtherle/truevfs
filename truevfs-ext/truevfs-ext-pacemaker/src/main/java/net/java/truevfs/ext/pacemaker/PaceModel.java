/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker;

import lombok.val;
import net.java.truevfs.comp.inst.InstrumentingModel;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsMountPoint;

/**
 * A pace model.
 *
 * @author Christian Schlichtherle
 */
final class PaceModel extends InstrumentingModel<PaceMediator> {

    private final LruCache<FsMountPoint> cachedMountPoints;

    PaceModel(PaceMediator mediator, FsModel model) {
        super(mediator, model);
        this.cachedMountPoints = mediator.cachedMountPoints;
    }

    @Override
    public void setMounted(final boolean isMounted) {
        val wasMounted = model.isMounted();
        model.setMounted(isMounted);
        if (wasMounted) {
            if (!isMounted) {
                cachedMountPoints.remove(getMountPoint());
            }
        } else {
            if (isMounted) {
                cachedMountPoints.add(getMountPoint());
            }
        }
    }
}

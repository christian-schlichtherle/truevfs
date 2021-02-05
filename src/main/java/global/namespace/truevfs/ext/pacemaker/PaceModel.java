/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker;

import global.namespace.truevfs.commons.inst.InstrumentingModel;
import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsMountPoint;
import lombok.val;

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

/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.commons.jmx.JmxComponent;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 *
 * @author Christian Schlichtherle
 */
final class I5tSyncMediator extends I5tMediator {

    I5tSyncMediator(String subject) {
        super(subject);
    }

    @Override
    I5tStatsController newController(int offset) {
        return new I5tSyncStatsController(this, offset);
    }

    @Override
    void rotateStats(final JmxComponent origin) {
        if (!(origin instanceof I5tManager)) {
            super.rotateStats(origin);
        }
    }
}

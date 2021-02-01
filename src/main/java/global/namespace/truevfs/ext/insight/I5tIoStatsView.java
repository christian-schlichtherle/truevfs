/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.ext.insight.stats.IoStats;
import global.namespace.truevfs.ext.insight.stats.IoStatsView;

import javax.management.MBeanInfo;

/**
 * A view for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
final class I5tIoStatsView extends I5tStatsView {

    I5tIoStatsView(I5tIoStatsController controller) {
        super(controller, IoStatsView.class, true);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of I/O statistics.";
    }
}

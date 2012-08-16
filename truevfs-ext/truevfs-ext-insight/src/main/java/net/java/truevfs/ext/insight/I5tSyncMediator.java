/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.jmx.JmxColleague;

/**
 * @see    JmxIoMediator
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tSyncMediator extends I5tMediator {

    I5tSyncMediator(String subject) { super(subject); }

    @Override
    I5tStatistics newStats(int offset) {
        return new I5tSyncStatistics(this, offset);
    }

    @Override
    void rotateStats(JmxColleague origin) {
        if (!(origin instanceof I5tManager)) super.rotateStats(origin);
    }
}

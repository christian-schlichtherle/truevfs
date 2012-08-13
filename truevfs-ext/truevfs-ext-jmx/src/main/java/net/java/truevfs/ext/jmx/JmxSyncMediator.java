/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.comp.jmx.JmxColleague;

/**
 * @see    JmxIoMediator
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncMediator extends JmxMediator {

    JmxSyncMediator(String subject) { super(subject); }

    @Override
    JmxStatistics newStats(int offset) {
        return new JmxSyncStatistics(this, offset);
    }

    @Override
    void rotateStats(JmxColleague origin) {
        if (!(origin instanceof JmxManager)) super.rotateStats(origin);
    }
}

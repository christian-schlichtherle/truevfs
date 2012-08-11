/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @see    JmxIoMediator
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxSyncMediator extends JmxMediator {

    JmxSyncMediator(String subject) { super(subject, 1); }

    @Override
    JmxStatistics<?> newStatistics(int offset) {
        return new JmxSyncStatistics(this, offset);
    }

    @Override
    void rotateStatistics() { }
}

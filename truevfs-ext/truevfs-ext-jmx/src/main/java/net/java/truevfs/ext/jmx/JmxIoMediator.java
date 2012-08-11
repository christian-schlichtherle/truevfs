/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @see    JmxSyncMediator
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxIoMediator extends JmxMediator {

    JmxIoMediator(String subject) { super(subject); }

    @Override
    JmxStatistics<?> newStatistics(int offset) {
        return new JmxIoStatistics(this, offset);
    }
}

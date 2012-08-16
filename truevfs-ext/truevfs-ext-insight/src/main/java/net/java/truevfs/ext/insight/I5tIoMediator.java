/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @see    JmxSyncMediator
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tIoMediator extends I5tMediator {

    I5tIoMediator(String subject) { super(subject); }

    @Override
    I5tStatistics newStats(int offset) {
        return new I5tIoStatistics(this, offset);
    }
}

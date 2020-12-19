/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tIoMediator extends I5tMediator {

    I5tIoMediator(String subject) {
        super(subject);
    }

    @Override
    I5tStatsController newController(int offset) {
        return new I5tIoStatsController(this, offset);
    }
}

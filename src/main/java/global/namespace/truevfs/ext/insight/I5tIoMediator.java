/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 *
 * @author Christian Schlichtherle
 */
final class I5tIoMediator extends I5tMediator {

    I5tIoMediator(String subject) {
        super(subject);
    }

    @Override
    I5tStatsController newController(int offset) {
        return new I5tIoStatsController(this, offset);
    }
}

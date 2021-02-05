/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.cio.IoSocket;

import java.io.Closeable;
import java.io.IOException;

interface LogCloseable extends Closeable, LogResource  {

    IoSocket<? extends Entry> context();

    default void opening() {
        log("Opening {}");
    }

    default void log(String message) {
        try {
            log(message, context().getTarget());
        } catch (IOException e) {
            logger().trace("Couldn't resolve resource target: ", e);
        }
    }
}

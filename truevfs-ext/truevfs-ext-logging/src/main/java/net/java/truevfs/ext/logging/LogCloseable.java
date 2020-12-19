package net.java.truevfs.ext.logging;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.IoSocket;

import java.io.Closeable;
import java.io.IOException;

interface LogCloseable extends Closeable, LogResource  {

    IoSocket<? extends Entry> context();

    default void opening() {
        log("Opening {}");
    }

    default void log(String message) {
        try {
            log(message, context().target());
        } catch (IOException e) {
            logger().trace("Couldn't resolve resource target: ", e);
        }
    }
}

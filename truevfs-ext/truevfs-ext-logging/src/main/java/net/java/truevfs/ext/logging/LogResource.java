package net.java.truevfs.ext.logging;

import org.slf4j.Logger;

interface LogResource {

    Logger logger();

    default void log(final String message, final Object parameter) {
        final var logger = logger();
        logger.debug(message, parameter);
        if (logger.isTraceEnabled()) {
            logger.trace("Stack trace:", new Throwable());
        }
    }
}

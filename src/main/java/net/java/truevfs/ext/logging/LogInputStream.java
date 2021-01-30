/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.IoSocket;
import net.java.truecommons.io.DecoratingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

final class LogInputStream extends DecoratingInputStream implements LogCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LogInputChannel.class);

    private final IoSocket<? extends Entry> context;

    LogInputStream(final IoSocket<? extends Entry> context, final InputStream in) {
        super(in);
        this.context = context;
        opening();
    }

    @Override
    public IoSocket<? extends Entry> context() {
        return context;
    }

    @Override
    public Logger logger() {
        return logger;
    }
}

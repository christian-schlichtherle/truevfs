/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a call to {@link FsController#sync} cannot succeed because
 * some threads have unclosed I/O resources, e.g. streams or channels.
 * <p>
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as these I/O resources have been
 * closed and no other exceptional conditions apply.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsOpenResourceException extends IOException {
    private static final long serialVersionUID = 1L;

    final int local, total;

    public FsOpenResourceException(int local, int total) {
        super("Thread-local / total number of open I/O resources (e.g. streams or channels): %d / %d");
        this.local = local;
        this.total = total;
    }

    public int getLocal() { return local; }

    public int getTotal() { return total; }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), local, total);
    }
}

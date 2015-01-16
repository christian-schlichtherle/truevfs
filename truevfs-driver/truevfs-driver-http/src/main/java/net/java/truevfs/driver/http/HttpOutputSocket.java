/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.cio.AbstractOutputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;

/**
 * An output socket for HTTP(S) entries.
 * Right now, this is only a dummy.
 *
 * @see    HttpInputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class HttpOutputSocket extends AbstractOutputSocket<HttpNode> {

    private final HttpNode entry;

    HttpOutputSocket(
            final BitField<FsAccessOption> options,
            final HttpNode entry,
            final @CheckForNull Entry template) {
        assert null != entry;
        assert null != options;
        this.entry    = entry;
    }

    @Override
    public HttpNode target() {
        return entry;
    }

    @Override
    public OutputStream stream(final InputSocket<? extends Entry> peer)
    throws IOException {
        return entry.newOutputStream();
    }
}

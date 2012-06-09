/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.http;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.FsAccessOption;
import net.truevfs.kernel.cio.AbstractOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.util.BitField;

/**
 * An output socket for HTTP(S) entries.
 * Right now, this is only a dummy.
 * 
 * @see    HttpInputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class HttpOutputSocket extends AbstractOutputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpOutputSocket(   final               HttpEntry                entry,
                        final               BitField<FsAccessOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        this.entry    = entry;
    }

    @Override
    public HttpEntry target() {
        return entry;
    }

    @Override
    public OutputStream stream(final InputSocket<? extends Entry> peer)
    throws IOException {
        return entry.newOutputStream();
    }
}

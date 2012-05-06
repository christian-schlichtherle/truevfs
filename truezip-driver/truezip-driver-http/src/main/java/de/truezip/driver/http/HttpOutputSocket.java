/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.cio.AbstractOutputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

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
    public HttpEntry localTarget() {
        return entry;
    }

    @Override
    public OutputStream stream() throws IOException {
        return entry.newOutputStream();
    }
}

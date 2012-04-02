/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output socket for HTTP(S) entries.
 * Right now, this is only a dummy.
 * 
 * @see     HttpInputSocket
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public class HttpOutputSocket extends OutputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpOutputSocket(   final               HttpEntry                entry,
                        final               BitField<AccessOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        this.entry    = entry;
    }

    @Override
    public HttpEntry getLocalTarget() {
        return entry;
    }

    @Override
    public OutputStream newStream() throws IOException {
        return entry.getOutputStream();
    }
}
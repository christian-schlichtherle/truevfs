/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.http;

import global.namespace.truevfs.comp.cio.AbstractOutputSocket;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.FsAccessOption;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * An output socket for HTTP(S) entries.
 * Right now, this is only a dummy.
 *
 * @see    HttpInputSocket
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HttpOutputSocket extends AbstractOutputSocket<HttpNode> {

    private final HttpNode entry;

    HttpOutputSocket(
            final BitField<FsAccessOption> options,
            final HttpNode entry,
            final Optional<? extends Entry> template) {
        assert null != entry;
        assert null != options;
        this.entry    = entry;
    }

    @Override
    public HttpNode target() {
        return entry;
    }

    @Override
    public OutputStream stream(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return entry.newOutputStream();
    }
}

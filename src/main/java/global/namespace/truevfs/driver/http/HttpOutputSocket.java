/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.http;

import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.cio.InputSocket;
import global.namespace.truevfs.commons.shed.BitField;
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
public class HttpOutputSocket implements global.namespace.truevfs.commons.cio.OutputSocket<HttpNode> {

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
    public HttpNode getTarget() {
        return entry;
    }

    @Override
    public OutputStream stream(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return entry.newOutputStream();
    }
}

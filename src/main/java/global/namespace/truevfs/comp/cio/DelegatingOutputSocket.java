/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * Forwards all calls to another output socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 * @see DelegatingInputSocket
 */
public interface DelegatingOutputSocket<E extends Entry> extends DelegatingIoSocket<E>, OutputSocket<E> {

    /**
     * Returns the delegate output socket.
     */
    @Override
    OutputSocket<? extends E> getSocket();

    @Override
    default OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return getSocket().stream(peer);
    }

    @Override
    default SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return getSocket().channel(peer);
    }
}

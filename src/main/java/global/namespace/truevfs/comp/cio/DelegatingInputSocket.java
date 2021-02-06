/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * Forwards all calls to another input socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param <E> the type of the {@linkplain #getTarget() target entry} for I/O operations.
 * @author Christian Schlichtherle
 * @see DelegatingOutputSocket
 */
public interface DelegatingInputSocket<E extends Entry> extends DelegatingIoSocket<E>, InputSocket<E> {

    /**
     * Returns the delegate input socket.
     */
    @Override
    InputSocket<? extends E> getSocket();

    @Override
    default InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        return getSocket().stream(peer);
    }

    @Override
    default SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        return getSocket().channel(peer);
    }
}

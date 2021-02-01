/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.inst;

import global.namespace.truevfs.comp.cio.DecoratingInputSocket;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.Optional;

/**
 * @param  <M> the type of the mediator.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InstrumentingOutputSocket
 * @author Christian Schlichtherle
 */
public class InstrumentingInputSocket<
        M extends Mediator<M>,
        E extends Entry>
extends DecoratingInputSocket<E> {

    protected final M mediator;

    public InstrumentingInputSocket(
            final M mediator,
            final InputSocket<? extends E> socket) {
        super(socket);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer)
    throws IOException {
        return mediator.instrument(this, socket.stream(peer));
    }

    @Override
    public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
    throws IOException {
        return mediator.instrument(this, socket.channel(peer));
    }
}

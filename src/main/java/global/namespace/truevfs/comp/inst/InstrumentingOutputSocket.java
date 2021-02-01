/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.inst;

import global.namespace.truevfs.comp.cio.DecoratingOutputSocket;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.Optional;

/**
 * @param  <M> the type of the mediator.
 * @param  <E> the type of the {@linkplain #getTarget() target entry} for I/O
 *         operations.
 * @see    InstrumentingInputSocket
 * @author Christian Schlichtherle
 */
public class InstrumentingOutputSocket<
        M extends Mediator<M>,
        E extends Entry>
extends DecoratingOutputSocket<E> {

    protected final M mediator;

    public InstrumentingOutputSocket(
            final M mediator,
            final OutputSocket<? extends E> socket) {
        super(socket);
        this.mediator = Objects.requireNonNull(mediator);
    }

    @Override
    public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return mediator.instrument(this, socket.stream(peer));
    }

    @Override
    public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        return mediator.instrument(this, socket.channel(peer));
    }
}

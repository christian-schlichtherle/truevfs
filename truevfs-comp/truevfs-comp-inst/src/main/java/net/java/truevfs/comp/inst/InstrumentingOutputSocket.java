/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.DecoratingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the director.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InstrumentingInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingOutputSocket<
        D extends Director<D>,
        E extends Entry>
extends DecoratingOutputSocket<E> {
    protected final D director;

    public InstrumentingOutputSocket(
            final D director,
            final OutputSocket<? extends E> socket) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public OutputStream stream(@CheckForNull InputSocket<? extends Entry> peer)
    throws IOException {
        return director.instrument(this, socket.stream(peer));
    }

    @Override
    public SeekableByteChannel channel(
            @CheckForNull InputSocket<? extends Entry> peer)
    throws IOException {
        return director.instrument(this, socket.channel(peer));
    }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.DecoratingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the director.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InstrumentingOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingInputSocket<
        D extends Director<D>,
        E extends Entry>
extends DecoratingInputSocket<E> {
    protected final D director;

    public InstrumentingInputSocket(
            final D director,
            final InputSocket<? extends E> socket) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputStream stream(@CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException {
        return director.instrument(this, socket.stream(peer));
    }

    @Override
    public SeekableByteChannel channel(
            @CheckForNull OutputSocket<? extends Entry> peer)
    throws IOException {
        return director.instrument(this, socket.channel(peer));
    }
}

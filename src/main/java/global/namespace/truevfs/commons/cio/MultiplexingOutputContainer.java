/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import global.namespace.truevfs.commons.io.DecoratingOutputStream;
import global.namespace.truevfs.commons.shed.CompoundIterator;
import global.namespace.truevfs.commons.shed.SuppressedExceptionBuilder;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static global.namespace.truevfs.commons.cio.Entry.ALL_ACCESS;
import static global.namespace.truevfs.commons.cio.Entry.Size.DATA;
import static global.namespace.truevfs.commons.cio.Entry.UNKNOWN;

/**
 * Decorates another output service to enable concurrent writing of multiple
 * entries to the decorated container.
 * Whenever an attempt is made to write more than one entry concurrently to
 * this container, all but the first entry is transparently redirected to an
 * I/O buffer.
 * Whenever a redirected entry is {@code close()}d then, another attempt is
 * made to copy the I/O buffer into the decorated container.
 * If this container is still busy with writing an entry to the decorated
 * container, then the copying is deferred until either another I/O buffer
 * gets {@code close()}d or this container gets {@code close()}d,
 * whatever happens first.
 * <p>
 * Note that this implies that {@code close()}ing an entry or this container
 * may fail with an {@link IOException}.
 *
 * @param <E> the type of the mutable entries.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MultiplexingOutputContainer<E extends MutableEntry> extends DecoratingOutputContainer<E> {

    private final IoBufferPool pool;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the output archive.
     */
    private final Map<String, BufferedEntryOutputStream> buffers = new LinkedHashMap<>();

    /**
     * @see #isBusy
     */
    private boolean busy;

    /**
     * Constructs a new multiplexed output service.
     *
     * @param output the decorated output service.
     * @param pool   the pool for buffering entry data.
     */
    public MultiplexingOutputContainer(final IoBufferPool pool, final OutputContainer<E> output) {
        super(output);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public Collection<E> entries() throws IOException {
        return new AbstractCollection<E>() {

            final Collection<E> entries = getContainer().entries();

            @Override
            public Iterator<E> iterator() {
                return new CompoundIterator<>(entries.iterator(), new BufferedEntriesIterator());
            }

            @Override
            public int size() {
                return entries.size() + buffers.size();
            }
        };
    }

    private class BufferedEntriesIterator implements Iterator<E> {

        final Iterator<BufferedEntryOutputStream> i = buffers.values().iterator();

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public E next() {
            return i.next().getTarget();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Optional<E> entry(final String name) throws IOException {
        val entry = getContainer().entry(name);
        return entry.isPresent()
                ? entry
                : Optional.ofNullable(buffers.get(name)).map(BufferedEntryOutputStream::getTarget);
    }

    @Override
    public OutputSocket<E> output(final E local) {
        Objects.requireNonNull(local);
        return new DecoratingOutputSocket<E>() {

            {
                socket = getContainer().output(local);
            }

            @Override
            public E getTarget() {
                return local;
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return isBusy()
                        ? new BufferedEntryOutputStream(getSocket(), peer)
                        : new EntryOutputStream(getSocket().stream(peer));
            }
        };
    }

    /**
     * Returns whether the container output archive is busy writing an archive
     * entry or not.
     *
     * @return Whether the container output archive is busy writing an archive
     * entry or not.
     */
    public boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        if (isBusy()) {
            throw new IOException("This multiplexing output service is still busy with writing a stream!");
        }
        storeBuffers();
        assert buffers.isEmpty();
        getContainer().close();
    }

    final void storeBuffers() throws IOException {
        if (!isBusy()) {
            for (Iterator<BufferedEntryOutputStream> i = buffers.values().iterator(); i.hasNext(); ) {
                if (i.next().storeBuffer()) {
                    i.remove();
                }
            }
        }
    }

    /**
     * This entry output stream writes directly to this output service.
     */
    private final class EntryOutputStream extends DecoratingOutputStream {

        boolean closed;

        EntryOutputStream(final OutputStream out) {
            super(out);
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                busy = false;
                out.close();
            }
            storeBuffers();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the archive entry to an
     * {@linkplain IoBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted unless this output service is still busy.
     */
    private final class BufferedEntryOutputStream extends DecoratingOutputStream {

        final InputSocket<?> input;
        final OutputSocket<? extends E> output;
        final IoBuffer buffer;
        boolean closed;

        BufferedEntryOutputStream(
                final OutputSocket<? extends E> output,
                final Optional<? extends InputSocket<? extends Entry>> input
        ) throws IOException {
            // HC SVNT DRACONES!
            final E local = (this.output = output).getTarget();
            val buffer = this.buffer = pool.allocate();
            val peer = (input.isPresent()
                    ? Optional.<Entry>of(input.get().getTarget())
                    : Optional.<Entry>empty()).orElse(buffer);
            class InputProxy extends DecoratingInputSocket<Entry> {

                {
                    socket = buffer.input();
                }

                @Override
                public Entry getTarget() {
                    return peer;
                }
            }
            try {
                this.input = new InputProxy();
                this.out = buffer.output().stream(Optional.empty());
            } catch (final Throwable t1) {
                try {
                    buffer.release();
                } catch (Throwable t2) {
                    t1.addSuppressed(t2);
                }
                throw t1;
            }
            buffers.put(local.getName(), this);
        }

        E getTarget() {
            try {
                return output.getTarget();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void close() throws IOException {
            val builder = new SuppressedExceptionBuilder<IOException>();
            if (!closed) {
                closed = true;
                try {
                    out.close();
                    val local = output.getTarget();
                    if (this == buffers.get(local.getName())) {
                        updateProperties(local, input.getTarget());
                    } else {
                        discardBuffer();
                    }
                } catch (IOException e) {
                    builder.warn(e);
                }
            }
            try {
                storeBuffers();
            } catch (IOException e) {
                builder.warn(e);
            }
            builder.check();
        }

        void updateProperties(final E local, final Entry peer) {
            for (val type : ALL_ACCESS) {
                if (UNKNOWN == local.getTime(type)) {
                    local.setTime(type, peer.getTime(type));
                }
            }
            // Never copy any but the DATA size!
            if (UNKNOWN == local.getSize(DATA)) {
                local.setSize(DATA, peer.getSize(DATA));
            }
        }

        void discardBuffer() throws IOException {
            assert closed;
            buffer.release();
        }

        boolean storeBuffer() throws IOException {
            if (closed && !isBusy()) {
                IoSockets.copy(input, output);
                buffer.release();
                return true;
            } else {
                return false;
            }
        }
    }
}

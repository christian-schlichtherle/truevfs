/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.OutputClosedException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * Decorates another output shop in order to disconnect any entry resources
 * when this output shop gets closed.
 *
 * @see     DisconnectingInputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class DisconnectingOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private boolean closed;

    /**
     * Constructs a disconnecting output shop.
     * 
     * @param output the shop to decorate.
     */
    public DisconnectingOutputShop(OutputShop<E> output) {
        super(output);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        delegate.close();
    }

    private void checkOpen() throws IOException {
        if (closed)
            throw new OutputClosedException();
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(DisconnectingOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                checkOpen();
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public Entry getPeerTarget() throws IOException {
                checkOpen();
                return getBoundSocket().getPeerTarget();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                checkOpen();
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                checkOpen();
                return new DisconnectingOutputStream(
                        getBoundSocket().newOutputStream());
            }
        } // Output

        return new Output();
    }

    private final class DisconnectingOutputStream
    extends DecoratingOutputStream {
        DisconnectingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            checkOpen();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkOpen();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (!closed)
                delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingOutputStream
}

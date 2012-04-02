/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.InputException;
import de.truezip.kernel.io.Streams;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An I/O socket represents the address of an entry in a container and the
 * options required for doing subseqent I/O to this entry.
 * Neither the address nor the options are available via the API of this class,
 * but implementations must ensure that they are immutable throughout the life
 * cycle of a socket.
 * In contrast, the state of the entry itself is considered mutable and may
 * change anytime, even concurrently!
 * (At latest, it should change once some output has completed, but this is not
 * a requirement for implementing this API).
 * In order to reflect this, an implementation should not access the entry nor
 * throw an {@link IOException} when creating a socket.
 * <p>
 * The entry is called the <i>local target</i> of a socket and can get resolved
 * anytime by calling the abstract method {@link #getLocalTarget()}.
 * However, this operation may fail with an {@link IOException} at the
 * discretion of the implementation.
 * <p>
 * A socket may have an optional <i>remote target</i> which can get resolved
 * anytime by calling the abstract method {@link #getRemoteTarget()}.
 * If this method returns {@code null}, then the socket does not have a remote
 * target.
 * Again, this operation may fail with an {@code IOException}.
 * <p>
 * I/O sockets are designed to {@linkplain #copy copy} the contents of their
 * I/O targets fast and easily by using multithreading.
 * In addition, a socket may negotiate with its remote target in order to
 * agree upon the necessary processing when copying the entry data.
 * For example, this could get used by an implementation in order to avoid
 * redundant decompression and recompression when copying compressed entry data.
 *
 * @param  <LT> the type of the {@link #getLocalTarget() local target}
 *         for I/O operations.
 * @param  <PT> the type of the {@link #getRemoteTarget() remote target}
 *         for I/O operations.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class IOSocket<LT, PT> {

    /** You cannot instantiate this class outside its package. */
    IOSocket() { }

    /**
     * Resolves the <i>local target</i> for I/O operations.
     * <p>
     * Note that this interface contract does <em>not</em> state any other
     * terms or conditions for the returned object.
     * In particular, clients need to consider that multiple invocations of
     * this method could return different objects (e.g. defensive copies) which
     * may even fail the {@link Object#equals} test.
     * On the other hand, implementations need to consider that clients could
     * attempt to change the state of the returned object in arbitrary manner.
     * Consequently, the result of doing so is undefined, too.
     * In particular, a subsequent I/O operation may not reflect the change
     * or may even fail.
     * Sub-interfaces or implementations may add additional terms and
     * conditions in order to resolve these potential issues.
     *
     * @return The local target for I/O operations.
     * @throws IOException On any I/O failure. 
     */
    public abstract LT getLocalTarget() throws IOException;

    /**
     * Resolves the <i>remote target</i> for I/O operations.
     * <p>
     * The same considerations as for {@link #getLocalTarget} apply here, too.
     *
     * @return The remote target for I/O operations.
     * @throws IOException On any I/O failure. 
     */
    public abstract @CheckForNull PT getRemoteTarget() throws IOException;

    /**
     * Copies an input stream {@link InputSocket#newStream created}
     * by the given input socket {@code input} to an output stream
     * {@link OutputSocket#newOutputStream created} by the given output socket
     * {@code output}.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     *
     * @param  input an input socket for the input target.
     * @param  output an output socket for the output target.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>input socket</em>.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>output socket</em>.
     */
    public static void copy(final InputSocket <?> input,
                            final OutputSocket<?> output)
    throws IOException {
        // Call connect on output for early NPE check!
        Streams.copy(input, output.connect(input));
        // Disconnect for subsequent use, if any.
        input.connect(null); // or output.connect(null)
    }

    /**
     * Returns a string representing a connection of the local and remote
     * targets.
     */
    @Override
    public String toString() {
        Object lt;
        try {
            lt = getLocalTarget();
        } catch (final IOException ex) {
            lt = ex;
        }
        Object pt;
        try {
            pt = getRemoteTarget();
        } catch (final IOException ex) {
            pt = ex;
        }
        return String.format("%s[localTarget=%s, remoteTarget=%s]",
                getClass().getName(), lt, pt);
    }

    /**
     * Two sockets are considered equal if and only if they are identical.
     * 
     * @param that The object to test.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(@CheckForNull Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}

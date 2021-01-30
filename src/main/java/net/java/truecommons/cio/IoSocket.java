/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import java.io.IOException;

/**
 * An I/O socket is a <em>stateless</em> factory for I/O streams and channels
 * which operate on a {@linkplain #target() target entry}.
 * Because an I/O socket is stateless, it can get safely shared between
 * different components, in particular between different threads.
 * <p>
 * An I/O socket is typically opaque, i.e. it encapsulates all parameters
 * required for accessing the entry within its container without necessarily
 * exposing these parameters to its clients.
 * <p>
 * An I/O socket is typically short lived because its created upon demand only
 * in order to immediately create an I/O stream or channel and then gets
 * discarded.
 * <p>
 * I/O sockets are designed to {@linkplain IoSockets#copy copy} the contents of
 * their targets fast and easily by using multithreading.
 * When obtaining a stream or channel for copying entry contents, an I/O socket
 * may use the provided <i>peer</i> I/O socket in order to negotiate how the
 * entry contents shall get processed.
 * For example, this may get used by an implementation in order to avoid
 * redundant decompression and recompression when copying compressed entry
 * contents from one container to another.
 * <p>
 * Implementations must ensure that an I/O socket is (at least virtually)
 * immutable, i.e. the parameters required for accessing the entry within its
 * container must not change.
 * In contrast, this does not apply to the entry or its container: Their state
 * should always be considered mutable - even for a read-only container.
 * This implies that it may change anytime, even concurrently!
 * At least, it's expected to change once some output has completed.
 * In order to reflect this, an implementation should not access the entry nor
 * the container and it should never throw an {@link IOException} when creating
 * a I/O socket.
 * <p>
 * Implementations should be immutable.
 *
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
public interface IoSocket<E extends Entry> {

    /**
     * Resolves the <i>target</i> for I/O operations.
     * <p>
     * Note that this interface contract does <em>not</em> state any other
     * terms or conditions for the returned entry.
     * In particular, the returned object may or may not be a defensive copy
     * and it may or may not reflect the effect of subsequent I/O operations.
     * So a client may only assume that the returned entry accurately reflects
     * the state of its represented entity <em>before</em> the client does
     * subsequent I/O.
     * Implementations may add some constraints to ease the situation for
     * clients.
     *
     * @return The local target for I/O operations.
     * @throws IOException on any I/O error.
     */
    E target() throws IOException;
}

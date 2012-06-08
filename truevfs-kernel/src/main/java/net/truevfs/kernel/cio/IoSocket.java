/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

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
 * anytime by calling the abstract method {@link #localTarget()}.
 * However, this operation may fail with an {@link IOException} at the
 * discretion of the implementation.
 * <p>
 * A socket may have an optional <i>peer target</i> which can get resolved
 * anytime by calling the abstract method {@link #peerTarget()}.
 * If this method returns {@code null}, then the socket does not have a peer
 * target.
 * Again, this operation may fail with an {@code IOException}.
 * <p>
 * I/O sockets are designed to {@linkplain IoSockets#copy copy} the contents of
 * their I/O targets fast and easily by using multithreading.
 * In addition, a socket may negotiate with its peer target in order to
 * agree upon the necessary processing when copying the entry data.
 * For example, this could get used by an implementation in order to avoid
 * redundant decompression and recompression when copying compressed entry data.
 *
 * @param  <LT> the type of the {@linkplain #localTarget() local target}
 *         for I/O operations.
 * @param  <PT> the type of the {@linkplain #peerTarget() peer target}
 *         for I/O operations.
 * @param  <Local> the representational type of
 *         {@linkplain #connect(IoSocket) this socket}.
 * @param  <Peer> the type of the {@linkplain #getPeer() peer socket}.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface IoSocket<
        LT extends Entry,
        PT extends Entry,
        Local extends IoSocket<LT, PT, Local, Peer>,
        Peer extends IoSocket<? extends PT, ?, ?, ?/* super Local*/>> {

    /**
     * Resolves the <i>local target</i> for I/O operations.
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
    LT localTarget() throws IOException;

    /**
     * Resolves the nullable <i>peer target</i> for I/O operations.
     * The same considerations as for {@link #localTarget} apply here, too.
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * connected to a {@linkplain #getPeer peer socket}.
     *
     * @return The nullable peer target for I/O operations.
     * @throws IOException on any I/O error. 
     */
    @CheckForNull PT peerTarget() throws IOException;

    /**
     * Returns the nullable peer socket to which this socket is connected for
     * copying.
     * 
     * @return The nullable peer socket to which this socket is connected for
     *         copying.
     * @see    #connect(IoSocket)
     */
    @CheckForNull Peer getPeer();

    /**
     * Connects this socket to the given nullable {@code peer} socket.
     * This method shall change the peer socket of the given peer socket to
     * this socket, too.
     *
     * @param  peer the nullable peer output socket to connect to.
     * @return {@code this}
     */
    Local connect(@CheckForNull Peer peer);

    /**
     * Inherits the {@linkplain #getPeer peer socket} from the given socket.
     *
     * @param  to the socket from which to inherit the peer socket.
     * @return {@code this}
     * @throws IllegalArgumentException if {@code this} == {@code to}.
     */
    Local bind(IoSocket<?, ?, ?, ? extends Peer> to);
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides I/O sockets to address and resolve targets for I/O operations.
 * Similar to network sockets, I/O sockets are a very powerful concept:
 * They support input, output, copying, caching, proxying etc. of the contents
 * of a generic <i>local target</i> and an optional <i>peer target</i>.
 * By using I/O sockets, you'll never have to write a fast
 * {@link de.schlichtherle.truezip.socket.IOSocket#copy copy} routine again.
 * <p>
 * In order to maximize the versatility of I/O sockets, it's highly recommended
 * for any implementation that it performs any I/O initialization lazily.
 * That is, creating a socket should never throw a {@link java.io.IOException}.
 * If initializing I/O is required, it should be done in any of the methods
 * provided by the socket instead.
 * 
 * @author Christian Schlichtherle
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.schlichtherle.truezip.socket;

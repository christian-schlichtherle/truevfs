/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides generic I/O services.
 * <p>
 * Note that some decorator classes for
 * {@linkplain java.nio.channels.SeekableByteChannel seekable byte channels}
 * implement their own virtual
 * {@linkplain java.nio.channels.SeekableByteChannel#position() position}.
 * If you would like to use a decorated seekable byte channel again after you
 * have finished using such a decorating seekable byte channel, then you need
 * to synchronize their positions using the following idiom:
 * <pre>
 *     SeekableByteChannel sbc = ...
 *     try {
 *         SeekableInputChannel bic = new BufferedInputChannel(sbc);
 *         try {
 *             // Do any input on bic here...
 *             bic.seek(1);
 *         } finally {
 *             // Synchronize the positions.
 *             sbc.position(bic.position());
 *         }
 *         // This assertion would fail if we hadn't done the position
 *         // synchronization!
 *         assert sbc.position() == 1;
 *     } finally {
 *         sbc.close();
 *     }
 * </pre>
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.kernel.io;
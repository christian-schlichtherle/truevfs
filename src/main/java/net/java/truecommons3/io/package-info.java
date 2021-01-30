/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides common I/O components.
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
 * <p>
 * Unless noted otherwise, this is a {@code null}-free API:
 * No parameter and no return value of public methods in public classes is
 * allowed to be {@code null}.
 * Likewise, no public field in public classes is allowed to be {@code null},
 * although such fields should not exist in the first place.
 *
 * @author Christian Schlichtherle
 */
package net.java.truecommons3.io;

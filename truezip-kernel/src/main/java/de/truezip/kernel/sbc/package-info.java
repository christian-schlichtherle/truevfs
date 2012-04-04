/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides implementations of the
 * {@link java.nio.channels.SeekableByteChannel} interface.
 * <p>
 * Note that some decorator classes implement their own virtual
 * {@linkplain java.nio.channels.SeekableByteChannel#position() file pointer}.
 * If you would like to use a decorated seekable byte channel again after you
 * have finished using such a decorating seekable byte channel, then you need
 * to synchronize their file pointers using the following idiom:
 * <pre>
 *     SeekableByteChannel sbc = ...
 *     try {
 *         SeekableInputChannel bic = new BufferedInputChannel(sbc);
 *         try {
 *             // Do any file input on bic here...
 *             bic.seek(1);
 *         } finally {
 *             // Synchronize the file pointers.
 *             sbc.position(bic.position());
 *         }
 *         // This assertion would fail if we hadn't done the file pointer
 *         // synchronization!
 *         assert sbc.position() == 1;
 *     } finally {
 *         sbc.close();
 *     }
 * </pre>
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.kernel.sbc;

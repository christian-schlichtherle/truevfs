/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.util.zip.ZipException;
import net.java.truecommons.io.ImmutableBuffer;
import net.java.truecommons.io.PowerBuffer;

/**
 * An abstract Extra Field factory.
 *
 * @author Christian Schlichtherle
 */
abstract class AbstractExtraFieldFactory implements ExtraFieldFactory {

    @Override
    public final ExtraField newExtraField(final ImmutableBuffer buf)
    throws ZipException {
        try {
            return newExtraFieldUnchecked(buf);
        } catch (final RuntimeException ex) {
            throw (ZipException) new ZipException("Invalid Extra Field data!")
                    .initCause(ex);
        }
    }

    /**
     * Creates a new Extra Field and initializes it from the contents of the
     * given immmutable buffer {@code buf}.
     * This is semantically the same as {@link #newExtraField(PowerBuffer)} but
     * throws a {@link RuntimeException} instead of a {@link ZipException}.
     *
     * @param  ib a byte buffer with the shared content possibly holding the
     *         Header Id, Data Size and Data Block for the Extra Field.
     * @return a new Extra Field.
     * @throws RuntimeException if the buffer's content does not conform to the
     *         ZIP File Format Specification.
     * @see    #newExtraField
     */
    protected abstract ExtraField newExtraFieldUnchecked(ImmutableBuffer ib);
}

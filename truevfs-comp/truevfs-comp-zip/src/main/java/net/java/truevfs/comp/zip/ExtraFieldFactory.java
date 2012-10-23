/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.nio.ByteOrder;
import java.util.zip.ZipException;
import net.java.truecommons.io.ImmutableBuffer;

/**
 * A factory for {@link ExtraField}s.
 *
 * @author Christian Schlichtherle
 */
interface ExtraFieldFactory {

    /**
     * Creates a new Extra Field and initializes it from the contents of the
     * given immutable buffer {@code buf}.
     * The caller needs to set the buffer's {@link ImmutableBuffer#position()}
     * to the start of the Header Id (2 bytes) prior to calling this method.
     * The implementation then needs to check that the Header Id is followed by
     * the Data Size (2 bytes) and the variable length Data Block.
     * Upon failure of this check, a {@link ZipException} needs to get thrown.
     * <p>
     * Note that the buffer's content may be shared between the caller and the
     * implementation.
     * This implies that the created Extra Field may subsequently reread the
     * Header Id, Data Size or Data Block.
     * <p>
     * The implementation should not assume that the buffer's byte order is
     * {@link ByteOrder#LITTLE_ENDIAN}.
     *
     * @param  ib the immutable buffer with the shared content supposedly
     *         holding the Header Id, Data Size and Data Block for the
     *         Extra Field.
     * @return a new Extra Field.
     * @throws ZipException if the buffer's content does not conform to the
     *         ZIP File Format Specification.
     */
    ExtraField newExtraField(ImmutableBuffer ib) throws ZipException;
}

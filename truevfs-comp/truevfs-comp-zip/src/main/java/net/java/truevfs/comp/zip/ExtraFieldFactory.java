/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.nio.ByteOrder;
import java.util.zip.ZipException;
import net.java.truecommons.io.MutableBuffer;

/**
 * A factory for {@link ExtraField}s.
 *
 * @author Christian Schlichtherle
 */
interface ExtraFieldFactory {

    /**
     * Creates a new read-only Extra Field and initializes it from the contents
     * of the given mutable buffer {@code buf}.
     * The caller needs to set the buffer's {@link MutableBuffer#order()} to
     * {@link ByteOrder#LITTLE_ENDIAN} and its {@link MutableBuffer#position()}
     * to the start of the Header Id (2 bytes) prior to calling this method.
     * The implementation then needs to check that the Header Id is followed by
     * the Data Size (2 bytes) and the variable length Data Block.
     * Upon failure of this check, a {@link ZipException} must get thrown.
     * <p>
     * Upon success, the buffer's position must be advanced past the end of the
     * Data Block for subsequent reading of the following data by the caller.
     * Upon failure, the buffer's position is undefined.
     * In either case, the buffer's mark is undefined.
     * All other properties of the buffer must remain unchanged.
     * <p>
     * The buffer's content may be shared between the caller and the
     * implementation.
     * This implies that the created Extra Field may subsequently reread the
     * Header Id, Data Size or Data Block.
     * However, the implementation must not modify the buffer's content and it
     * must not subsequently use the buffer.
     * <p>
     * The implementation may assume that the buffer's byte order is
     * {@link ByteOrder#LITTLE_ENDIAN}.
     *
     * @param  buf a byte buffer with the shared content possibly holding the
     *         Header Id, Data Size and Data Block for the Extra Field.
     * @return a new read-only Extra Field.
     * @throws ZipException if the buffer's content does not conform to the
     *         ZIP File Format Specification.
     */
    ExtraField newExtraField(MutableBuffer buf) throws ZipException;
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.option;

import static de.truezip.kernel.fs.option.FsAccessOption.*;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of access options for I/O operations.
 * 
 * @see    FsAccessOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsAccessOptions {

    /** A bit field with no output options set. */
    public static final BitField<FsAccessOption>
            NONE = BitField.noneOf(FsAccessOption.class);

    /**
     * The mask of input preferences, which is
     * <code>{@link BitField}.of({@link FsAccessOption#CACHE})</code>.
     */
    public static final BitField<FsAccessOption> INPUT_PREFERENCES_MASK
            = BitField.of(CACHE);

    /**
     * The mask of output preferences, which is
     * <code>{@link BitField}.of({@link FsAccessOption#CACHE}, {@link FsAccessOption#CREATE_PARENTS}, {@link FsAccessOption#STORE}, {@link FsAccessOption#COMPRESS}, {@link FsAccessOption#GROW}, {@link FsAccessOption#ENCRYPT})</code>.
     */
    public static final BitField<FsAccessOption> OUTPUT_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT);

    /**
     * Converts the given array to a bit field of output options.
     * 
     * @param  options an array of output options.
     * @return A bit field of output options.
     */
    public static BitField<FsAccessOption> of(FsAccessOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsAccessOptions() { }
}
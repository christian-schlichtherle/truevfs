/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.option;

import static de.truezip.kernel.fs.option.FsOutputOption.*;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of output options.
 * 
 * @see    FsOutputOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsOutputOptions {

    /** A bit field with no output options set. */
    public static final BitField<FsOutputOption>
            NONE = BitField.noneOf(FsOutputOption.class);

    /**
     * The mask of output preferences, which is
     * <code>{@link BitField}.of({@link FsOutputOption#CACHE}, {@link FsOutputOption#CREATE_PARENTS}, {@link FsOutputOption#STORE}, {@link FsOutputOption#COMPRESS}, {@link FsOutputOption#GROW}, {@link FsOutputOption#ENCRYPT})</code>.
     */
    public static final BitField<FsOutputOption> OUTPUT_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT);

    /**
     * Converts the given array to a bit field of output options.
     * 
     * @param  options an array of output options.
     * @return A bit field of output options.
     */
    public static BitField<FsOutputOption> of(FsOutputOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsOutputOptions() { }
}
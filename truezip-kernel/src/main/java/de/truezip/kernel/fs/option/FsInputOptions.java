/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.option;

import static de.truezip.kernel.fs.option.FsInputOption.CACHE;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of input options.
 * 
 * @see    FsInputOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsInputOptions {

    /** A bit field with no input options set. */
    public static final BitField<FsInputOption>
            NONE = BitField.noneOf(FsInputOption.class);

    /**
     * The mask of input preferences, which is
     * <code>{@link BitField}.of({@link FsInputOption#CACHE})</code>.
     */
    public static final BitField<FsInputOption> INPUT_PREFERENCES_MASK
            = BitField.of(CACHE);

    /**
     * Converts the given array to a bit field of input options.
     * 
     * @param  options an array of input options.
     * @return A bit field of input options.
     */
    public static BitField<FsInputOption> of(FsInputOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsInputOptions() { }
}
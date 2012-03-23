/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.option;

import static de.schlichtherle.truezip.fs.option.FsInputOption.CACHE;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of input options.
 * 
 * @see    FsInputOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsInputOptions {

    /**
     * A bit field with no input options set.
     * 
     * @since TrueZIP 7.5
     */
    public static final BitField<FsInputOption>
            NONE = BitField.noneOf(FsInputOption.class);

    /**
     * The mask of input preferences, which is
     * <code>{@link BitField}.of({@link FsInputOption#CACHE})</code>.
     * 
     * @since TrueZIP 7.3
     */
    public static final BitField<FsInputOption> INPUT_PREFERENCES_MASK
            = BitField.of(CACHE);

    /**
     * Converts the given array to a bit field of input options.
     * 
     * @param  options an array of input options.
     * @return A bit field of input options.
     * @since  TrueZIP 7.5
     */
    public static BitField<FsInputOption> of(FsInputOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsInputOptions() { }
}

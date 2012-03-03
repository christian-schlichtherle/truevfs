/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsInputOption.CACHE;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common input options.
 * 
 * @see    FsInputOption
 * @see    FsOutputOptions
 * @since  TrueZIP 7.1.1
 * @author Christian Schlichtherle
 */
@Immutable
public class FsInputOptions {

    /**
     * A bit field with no input options set.
     * 
     * @since TrueZIP 7.2
     */
    public static final BitField<FsInputOption>
            NO_INPUT_OPTIONS = BitField.noneOf(FsInputOption.class);

    /**
     * @deprecated
     * @since TrueZIP 7.1.1
     * @see #NO_INPUT_OPTIONS
     */
    @Deprecated
    public static final BitField<FsInputOption>
            NO_INPUT_OPTION = NO_INPUT_OPTIONS;

    /**
     * The mask of input preferences, which is
     * <code>{@link BitField}.of({@link FsInputOption#CACHE})</code>.
     * 
     * @since TrueZIP 7.3
     */
    public static final BitField<FsInputOption> INPUT_PREFERENCES_MASK
            = BitField.of(CACHE);

    /* Can't touch this - hammer time! */
    private FsInputOptions() { }
}

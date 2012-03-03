/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common output options.
 * 
 * @see    FsOutputOption
 * @see    FsInputOptions
 * @since  TrueZIP 7.1.1
 * @author Christian Schlichtherle
 */
@Immutable
public class FsOutputOptions {

    /**
     * A bit field with no output options set.
     * 
     * @since TrueZIP 7.2
     */
    public static final BitField<FsOutputOption>
            NO_OUTPUT_OPTIONS = BitField.noneOf(FsOutputOption.class);

    /**
     * @deprecated
     * @since TrueZIP 7.1.1
     * @see #NO_OUTPUT_OPTIONS
     */
    @Deprecated
    public static final BitField<FsOutputOption>
            NO_OUTPUT_OPTION = NO_OUTPUT_OPTIONS;

    /**
     * The mask of output preferences, which is
     * <code>{@link BitField}.of({@link FsOutputOption#CACHE}, {@link FsOutputOption#CREATE_PARENTS}, {@link FsOutputOption#STORE}, {@link FsOutputOption#COMPRESS}, {@link FsOutputOption#GROW}, {@link FsOutputOption#ENCRYPT})</code>.
     * 
     * @since TrueZIP 7.3
     */
    public static final BitField<FsOutputOption> OUTPUT_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT);

    /* Can't touch this - hammer time! */
    private FsOutputOptions() { }
}

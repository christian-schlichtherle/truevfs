/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.option;

import static de.truezip.kernel.option.AccessOption.*;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of access options for I/O operations.
 * 
 * @see    AccessOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class AccessOptions {

    /** A bit field with no access options set. */
    public static final BitField<AccessOption>
            NONE = BitField.noneOf(AccessOption.class);

    /**
     * The mask of access preferences, which is
     * <code>{@link BitField}.of({@link AccessOption#CACHE}, {@link AccessOption#CREATE_PARENTS}, {@link AccessOption#STORE}, {@link AccessOption#COMPRESS}, {@link AccessOption#GROW}, {@link AccessOption#ENCRYPT})</code>.
     */
    public static final BitField<AccessOption> ACCESS_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, STORE, COMPRESS, GROW, ENCRYPT);

    /**
     * Converts the given array to a bit field of output options.
     * 
     * @param  options an array of output options.
     * @return A bit field of output options.
     */
    public static BitField<AccessOption> of(AccessOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private AccessOptions() { }
}

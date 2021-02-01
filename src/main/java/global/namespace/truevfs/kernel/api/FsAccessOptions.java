/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.shed.BitField;

import static global.namespace.truevfs.kernel.api.FsAccessOption.*;

/**
 * Provides common bit fields of access options for I/O operations.
 * 
 * @see    FsAccessOption
 * @author Christian Schlichtherle
 */
public final class FsAccessOptions {

    /** A bit field with no access options set. */
    public static final BitField<FsAccessOption>
            NONE = BitField.noneOf(FsAccessOption.class);

    /**
     * The mask of access preferences, which is
     * <code>{@link BitField}.of({@link FsAccessOption#CACHE}, {@link FsAccessOption#CREATE_PARENTS}, {@link FsAccessOption#STORE}, {@link FsAccessOption#COMPRESS}, {@link FsAccessOption#GROW}, {@link FsAccessOption#ENCRYPT})</code>.
     */
    public static final BitField<FsAccessOption> ACCESS_PREFERENCES_MASK
            = BitField.of(CACHE, CREATE_PARENTS, GROW, STORE, COMPRESS, ENCRYPT);

    /**
     * Converts the given array to a bit field of output options.
     * 
     * @param  options an array of output options.
     * @return A bit field of output options.
     */
    public static BitField<FsAccessOption> of(FsAccessOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    private FsAccessOptions() { }
}

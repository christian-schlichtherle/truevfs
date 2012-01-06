/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @see     FsOutputOption
 * @see     FsInputOptions
 * @since   TrueZIP 7.1.1
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class FsOutputOptions {

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

    /** You cannot instantiate this class. */
    private FsOutputOptions() {
    }
}

/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsInputOption.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @see     FsInputOption
 * @see     FsOutputOptions
 * @since   TrueZIP 7.1.1
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class FsInputOptions {

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

    /** You cannot instantiate this class. */
    private FsInputOptions() {
    }
}

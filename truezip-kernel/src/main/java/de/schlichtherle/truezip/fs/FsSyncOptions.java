/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @see     FsSyncOption
 * @since   TrueZIP 7.1.1
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class FsSyncOptions {

    /**
     * Equivalent to
     * {@code BitField.of(FsSyncOption.FORCE_CLOSE_INPUT, FsSyncOption.FORCE_CLOSE_OUTPUT, FsSyncOption.CLEAR_CACHE)}.
     */
    public static final BitField<FsSyncOption>
            UMOUNT = BitField.of(   FORCE_CLOSE_INPUT,
                                    FORCE_CLOSE_OUTPUT,
                                    CLEAR_CACHE);

    /** You cannot instantiate this class. */
    private FsSyncOptions() {
    }
}

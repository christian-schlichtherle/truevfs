/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

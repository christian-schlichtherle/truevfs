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
     * @since TrueZIP 7.1.1
     * @deprecated
     * @see #NO_OUTPUT_OPTIONS
     */
    @Deprecated
    public static final BitField<FsOutputOption>
            NO_OUTPUT_OPTION = NO_OUTPUT_OPTIONS;

    /** You cannot instantiate this class. */
    private FsOutputOptions() {
    }
}

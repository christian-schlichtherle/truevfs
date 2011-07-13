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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean which represents the original values of selected parameters
 * for the {@link FsContextController} operation in scope.
 * <p>
 * TODO: When adding any input operation parameters, make {@link #setOperation}
 * really thread-safe.
 *
 * @see     FsContextModel
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsOperationContext {

    private @CheckForNull BitField<FsOutputOption> outputOptions;

    /**
     * Returns the options for the output operation in scope.
     * If the operation in scope is not an output operation, then
     * {@code null} is returned.
     * Otherwise, if the output operation in scope does not accept options,
     * then an empty bit field is returned.
     * Otherwise, a bit field with the options for the output operation is
     * returned.
     * 
     * @return The options for the output operation in scope.
     * @see    FsContextController#getOutputSocket(FsEntryName, BitField, Entry)
     * @see    FsContextController#mknod(FsEntryName, Entry.Type, BitField, Entry)
     */
    public @Nullable BitField<FsOutputOption> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Sets the options for the output operation in scope.
     *
     * @param outputOptions the options for the output operation in scope.
     * @see   #getOutputOptions()
     */
    void setOutputOptions(final @Nullable BitField<FsOutputOption> outputOptions) {
        this.outputOptions = outputOptions;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[outputOptions=")
                .append(getOutputOptions())
                .append("]")
                .toString();
    }
}

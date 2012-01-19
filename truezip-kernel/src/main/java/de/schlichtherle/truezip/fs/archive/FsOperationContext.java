/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean which represents the original values of selected parameters
 * for the {@link FsContextController} operation in progress.
 *
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsOperationContext {

    private @Nullable BitField<FsOutputOption> outputOptions;

    /**
     * Returns the options for the output operation in progress.
     * If the operation in progress is not an output operation or does not
     * accept output options, then an empty bit field is returned.
     * Otherwise, a bit field with the options for the output operation in
     * progress is returned.
     * 
     * @return The options for the output operation in progress.
     * @see    FsContextController#getOutputSocket(FsEntryName, BitField, Entry)
     * @see    FsContextController#mknod(FsEntryName, Entry.Type, BitField, Entry)
     */
    @Nullable BitField<FsOutputOption> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Sets the options for the output operation in progress.
     *
     * @param outputOptions the options for the output operation in progress.
     * @see   #getOutputOptions()
     */
    void setOutputOptions(final @Nullable BitField<FsOutputOption> outputOptions) {
        this.outputOptions = outputOptions;
    }

    /**
     * Returns {@code true} if and only if the given output option is set.
     *
     * @param option The output option to test.
     */
    boolean get(FsOutputOption option) {
        return outputOptions.get(option);
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

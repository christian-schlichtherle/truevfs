/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
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
import javax.annotation.concurrent.Immutable;

/**
 * Represents the original values of selected parameters for the
 * {@linkplain FsContextController file system controller} operation in
 * progress.
 *
 * @see     FsContextController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class FsOperationContext {

    private final BitField<FsOutputOption> outputOptions;

    FsOperationContext(final BitField<FsOutputOption> outputOptions) {
        assert null != outputOptions;
        this.outputOptions = outputOptions;
    }

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
    BitField<FsOutputOption> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Returns {@code true} if and only if the given output option is set.
     *
     * @param option The output option to test.
     */
    boolean get(final FsOutputOption option) {
        return outputOptions.get(option);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[outputOptions=%s]",
                getClass().getName(),
                getOutputOptions());
    }
}

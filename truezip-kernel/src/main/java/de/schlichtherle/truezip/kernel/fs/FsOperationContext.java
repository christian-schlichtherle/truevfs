/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.option.FsAccessOption;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Represents the original values of selected parameters for the
 * {@linkplain FsContextController file system controller} operation in
 * progress.
 *
 * @see     FsContextController
 * @author  Christian Schlichtherle
 */
@Immutable
final class FsOperationContext {

    private final BitField<FsAccessOption> outputOptions;

    FsOperationContext(final BitField<FsAccessOption> outputOptions) {
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
    BitField<FsAccessOption> getOutputOptions() {
        return outputOptions;
    }

    /**
     * Returns {@code true} if and only if the given output option is set.
     *
     * @param option The output option to test.
     */
    boolean get(final FsAccessOption option) {
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
/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.ClutchInputSocket;
import de.schlichtherle.truezip.socket.ClutchOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decouples its input and output sockets upon any {@link Throwable}.
 * 
 * @param  <M> the type of the file system model.
 * @see    FsNeedsSyncException
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsClutchController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    /**
     * Constructs a new file system proxy controller.
     *
     * @param controller the decorated file system controller.
     */
    FsClutchController(FsController<? extends M> controller) {
        super(controller);
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        @NotThreadSafe
        class Input extends ClutchInputSocket<Entry> {
            @Override
            protected InputSocket<?> getLazyDelegate() throws IOException {
                return FsClutchController.this.delegate
                        .getInputSocket(name, options);
            }
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        class Output extends ClutchOutputSocket<Entry> {
            @Override
            protected OutputSocket<?> getLazyDelegate() throws IOException {
                return FsClutchController.this.delegate
                        .getOutputSocket(name, options, template);
            }
        } // Output

        return new Output();
    }
}

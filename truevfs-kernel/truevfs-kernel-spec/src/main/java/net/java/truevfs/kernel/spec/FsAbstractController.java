/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.UniqueObject;
import net.java.truevfs.kernel.spec.cio.Entry.Access;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 * <p>
 * Subclasses should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsAbstractController
extends UniqueObject implements FsController {

    private final FsModel model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsAbstractController(final FsModel model) {
        this.model = Objects.requireNonNull(model);
    }

    @Override
    public final FsModel getModel() {
        return model;
    }

    /**
     * Returns the mount point of this (virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     * 
     * @return The mount point of this (virtual) file system as
     *         defined by the {@linkplain #getModel() model}.
     */
    public final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    /**
     * Returns the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @return the {@code mounted} property of the
     *         {@linkplain #getModel() file system model}.
     */
    public final boolean isMounted() {
        return model.isMounted();
    }

    /**
     * Sets the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @param mounted the {@code mounted} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final void setMounted(boolean mounted) {
        model.setMounted(mounted);
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Map<Access, Long> times)
    throws IOException {
        boolean ok = true;
        for (final Map.Entry<Access, Long> e : times.entrySet()) {
            final long value = e.getValue();
            ok &= 0 <= value && setTime(options, name, BitField.of(e.getKey()), value);
        }
        return ok;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     * 
     * @return A string representation of this object for debugging and logging
     *         purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[model=%s]",
                getClass().getName(),
                getModel());
    }
}

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import net.truevfs.kernel.spec.cio.Entry.Access;
import net.truevfs.kernel.spec.util.BitField;
import net.truevfs.kernel.spec.util.UniqueObject;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractController<M extends FsModel>
extends UniqueObject implements FsController<M> {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsAbstractController(final M model) {
        this.model = Objects.requireNonNull(model);
    }

    @Override
    public final M getModel() {
        return model;
    }

    /**
     * Returns the mount point of this (federated virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     * 
     * @return The mount point of this (federated virtual) file system as
     *         defined by the {@linkplain #getModel() model}.
     */
    public final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    /**
     * Returns the {@code touched} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @return the {@code touched} property of the
     *         {@linkplain #getModel() file system model}.
     */
    public final boolean isTouched() {
        return model.isTouched();
    }

    /**
     * Sets the {@code touched} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @param touched the {@code touched} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final void setTouched(boolean touched) {
        model.setTouched(touched);
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
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

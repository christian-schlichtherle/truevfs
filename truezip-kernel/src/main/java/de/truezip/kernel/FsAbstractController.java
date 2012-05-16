/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractController<M extends FsModel>
implements FsController<M> {

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
    protected final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    /**
     * Returns the {@code touched} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @return the {@code touched} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final boolean isTouched() {
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
     * Two file system controllers are considered equal if and only if
     * they are identical.
     * 
     * @param  that the object to compare.
     * @return {@code this == that}. 
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(@CheckForNull Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * 
     * @return A hash code which is consistent with {@link #equals}.
     * @see Object#hashCode
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
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

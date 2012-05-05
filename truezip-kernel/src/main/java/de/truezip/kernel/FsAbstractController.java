/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * An abstract class which provides read/write access to a file system.
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractController<M extends FsModel>
implements FsController<M> {

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        boolean ok = true;
        for (Map.Entry<Access, Long> time : times.entrySet())
            ok &= setTime(  options, name,
                            BitField.of(time.getKey()),
                            time.getValue());
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

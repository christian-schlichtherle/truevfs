/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.shed;

/**
 * A unique object compares {@link #equals equal} only with itself.
 * 
 * @author Christian Schlichtherle
 */
public class UniqueObject {

    /**
     * Returns {@code this == obj}.
     * 
     * @return {@code this == obj}.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object obj) { return this == obj; }

    /**
     * Returns {@code System.identityHashCode(this)}.
     * 
     * @return {@code System.identityHashCode(this)}.
     */
    @Override
    public final int hashCode() { return System.identityHashCode(this); }
}

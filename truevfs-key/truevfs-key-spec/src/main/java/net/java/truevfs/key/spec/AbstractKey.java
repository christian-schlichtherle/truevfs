/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.io.Serializable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A JavaBean with basic properties for life cycle management.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractKey<K extends AbstractKey<K>>
implements Key<K>, Serializable {

    @Override
    public K clone() {
        try {
            return (K) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || null != obj && this.getClass().equals(obj.getClass());
    }

    @Override
    public abstract int hashCode();
}

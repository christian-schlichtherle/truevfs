/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import javax.annotation.Nullable;

/**
 * A JavaBean with basic properties for life cycle management.
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and {@code XML(En|De)coder}.
 * <p>
 * Subclasses do not need to be thread-safe.
 *
 * @param <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractKey<K extends AbstractKey<K>> implements Key<K> {

    @Override
    @SuppressWarnings("unchecked")
    public K clone() {
        try {
            return (K) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || null != obj && this.getClass().equals(obj.getClass());
    }

    @Override
    public abstract int hashCode();
}

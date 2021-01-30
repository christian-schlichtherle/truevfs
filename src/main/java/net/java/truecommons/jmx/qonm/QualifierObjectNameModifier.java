/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.qonm;

import net.java.truecommons.jmx.AbstractObjectNameModifier;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Objects;

/**
 * Adds a key-value qualifier to an object name.
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("UseOfObsoleteCollectionType")
public class QualifierObjectNameModifier extends AbstractObjectNameModifier {

    private final String key, value;
    private final KeyPropertyListStrategy keyPropertyList;

    public QualifierObjectNameModifier(final String key, String value) {
        this.key = Objects.requireNonNull(key);
        this.value = Objects.requireNonNull(value);
        KeyPropertyListStrategy s = KeyPropertyListStrategy.GET;
        try {
            final Hashtable<String, String> t = s.apply(new ObjectName(":test=test"));
            try {
                t.remove("test");
            } catch (final UnsupportedOperationException ex) {
                s = KeyPropertyListStrategy.COPY;
            }
        } catch (final MalformedObjectNameException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
        this.keyPropertyList = s;
    }

    @Override
    public ObjectName apply(final @Nullable ObjectName name) {
        if (null == name) return null;
        final String domain = name.getDomain();
        final Hashtable<String, String> table = keyPropertyList.apply(name);
        try {
            return null != table.put(key, value)
                    ? name
                    : new ObjectName(domain, table);
        } catch (final MalformedObjectNameException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public ObjectName unapply(final ObjectName name) {
        if (null == name) return null;
        final String domain = name.getDomain();
        final Hashtable<String, String> table = keyPropertyList.apply(name);
        try {
            return null == table.remove(key)
                    ? name
                    : new ObjectName(domain, table);
        } catch (final MalformedObjectNameException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private enum KeyPropertyListStrategy {
        GET {
            @Override Hashtable<String, String> apply(ObjectName name) {
                return name.getKeyPropertyList();
            }
        },

        COPY {
            @Override Hashtable<String, String> apply(ObjectName name) {
                return new Hashtable<>(name.getKeyPropertyList());
            }
        };

        abstract Hashtable<String, String> apply(ObjectName name);
    }
}

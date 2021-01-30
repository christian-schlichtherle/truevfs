/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx;

import java.util.Hashtable;
import javax.inject.Provider;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A builder for {@link ObjectName}s.
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
public final class ObjectNameBuilder implements Provider<ObjectName> {

    private final String domain;
    private final Hashtable<String, String> table = new Hashtable<String, String>();

    public ObjectNameBuilder(final Package domain) {
        this.domain = domain.getName();
    }

    public String put(String key) { return table.get(key); }

    public ObjectNameBuilder put(String key, String value) {
        table.put(key, value);
        return this;
    }

    public ObjectNameBuilder remove(String key) {
        table.remove(key);
        return this;
    }

    @Override public ObjectName get() {
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

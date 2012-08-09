/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.util.Hashtable;
import javax.inject.Provider;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author Christian Schlichtherle
 */
public final class JmxObjectNameBuilder implements Provider<ObjectName> {
    final Hashtable<String, String> table = new Hashtable<>();

    private final String domain;

    public JmxObjectNameBuilder(Package domain) {
        this.domain = domain.getName();
    }

    public JmxObjectNameBuilder put(String key, String value) {
        table.put(key, value);
        return this;
    }

    @Override
    public ObjectName get() {
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}

/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.logging;

import java.util.Formatter;
import java.util.ResourceBundle;
import javax.annotation.concurrent.Immutable;

/**
 * Lazily resolves and formats localized messages for debugging or logging
 * purposes.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class BundledMessage {
    private final ResourceBundle bundle;
    private final String key;
    private final Object[] args;

    /**
     * Memorizes the given parameters for use by {@link #toString}.
     * 
     * @param clazz the class to lookup the resource bundle for.
     * @param key the key to lookup in the resource bundle.
     * @param args the parameters for the string value in the resource bundle.
     *        Note that this parameter is <em>not</em> copied!
     */
    public BundledMessage(final Class<?> clazz, final String key, final Object... args) {
        this(ResourceBundle.getBundle(clazz.getName()), key, args);
    }

    /**
     * Memorizes the given parameters for use by {@link #toString}.
     * 
     * @param bundle the resource bundle.
     * @param key the key to lookup in the resource bundle.
     * @param args the parameters for the string value in the resource bundle.
     *        Note that this parameter is <em>not</em> copied!
     */
    public BundledMessage(final ResourceBundle bundle, final String key, final Object... args) {
        this.bundle = bundle;
        this.key = key;
        this.args = args;
    }

    /**
     * Looks up the key in the resource bundle and formats the resulting value
     * with the parameters provided to the constructor.
     * The string is formatted using a {@link Formatter}.
     * 
     * @return the localized and formatted string.
     */
    @Override
    public String toString() {
        return new Formatter().format(bundle.getString(key), args).toString();
    }
}

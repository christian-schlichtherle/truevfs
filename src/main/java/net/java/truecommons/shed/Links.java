/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

/**
 * Static utility methods for links.
 * <p>
 * This class is trivially immutable.
 *
 * @author Christian Schlichtherle
 */
public class Links {

    private Links() { }

    /**
     * Returns the nullable {@linkplain Link#get() target} of the given
     * link.
     * The returned target is {@code null} if and only if either the given
     * link is {@code null} or its target is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  link a nullable link.
     * @return The nullable {@linkplain Link#get() target} of the given
     *         link.
     */
    public static <T> T target(Link<T> link) {
        return null == link ? null : link.get();
    }
}

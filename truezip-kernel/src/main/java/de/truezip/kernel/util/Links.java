/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import de.truezip.kernel.util.Link.Type;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Static utility methods for links.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class Links {

    /* Can't touch this - hammer time! */
    private Links() { }

    /**
     * Returns a nullable (strong) link to the given target.
     * The returned link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable (strong) link to the given target.
     */
    public static @CheckForNull <T> Link<T> link(@CheckForNull T target) {
        return link(Type.STRONG, target);
    }

    /**
     * Returns a nullable typed link to the given target.
     * The returned typed link is {@code null} if and only if {@code target}
     * is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  target the nullable target.
     * @return A nullable typed link to the given target.
     */
    public static @CheckForNull <T> Link<T> link(Type type,
                                                    @CheckForNull T target) {
        return null == target ? null : type.link(target);
    }

    /**
     * Returns the nullable {@linkplain Link#getTarget() target} of the given
     * link.
     * The returned target is {@code null} if and only if either the given
     * link is {@code null} or its target is {@code null}.
     *
     * @param  <T> The type of the target.
     * @param  link a nullable link.
     * @return The nullable {@linkplain Link#getTarget() target} of the given
     *         link.
     */
    public static @CheckForNull <T> T target(@CheckForNull Link<T> link) {
        return null == link ? null : link.getTarget();
    }
}
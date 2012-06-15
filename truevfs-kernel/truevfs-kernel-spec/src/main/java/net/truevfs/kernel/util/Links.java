/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

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
    public static @CheckForNull <T> T target(@CheckForNull Link<T> link) {
        return null == link ? null : link.get();
    }
}

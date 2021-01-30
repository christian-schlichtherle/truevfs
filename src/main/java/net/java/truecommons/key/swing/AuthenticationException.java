/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that password authentication failed for some reason.
 *
 * @since  TrueCommons 2.2.2
 * @author Christian Schlichtherle
 */
final class AuthenticationException extends Exception {

    private static final long serialVersionUID = 0L;

    AuthenticationException(@Nullable String message) { super(message); }

    AuthenticationException(@Nullable Throwable cause) { super(cause); }

    AuthenticationException(
            @Nullable String message,
            @Nullable Throwable cause ) {
        super(message, cause);
    }
}

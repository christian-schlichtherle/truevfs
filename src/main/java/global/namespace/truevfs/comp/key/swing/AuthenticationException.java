/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that password authentication failed for some reason.
 *
 * @author Christian Schlichtherle
 */
final class AuthenticationException extends Exception {

    private static final long serialVersionUID = 0;

    AuthenticationException(@Nullable String message) { super(message); }

    AuthenticationException(@Nullable Throwable cause) { super(cause); }

    AuthenticationException(
            @Nullable String message,
            @Nullable Throwable cause ) {
        super(message, cause);
    }
}

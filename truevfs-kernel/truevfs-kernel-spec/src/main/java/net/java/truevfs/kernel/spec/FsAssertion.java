/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * Asserts the properties of a virtual file system operation.
 * <p>
 * As of TrueVFS 0.10, application level transactions are not supported,
 * that is, multiple file system operations cannot get composed into a single
 * application level transaction - support for this feature may be added in a
 * future version.
 * <p>
 * However, individual file system operations do come with assertions about
 * their atomicity, consistency, isolation and durability - see
 * {@link FsController}.
 *
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
@Documented
@Inherited
@Retention(RetentionPolicy.SOURCE)
@Target({ CONSTRUCTOR, METHOD })
public @interface FsAssertion {

    /**
     * Does the operation <em>always</em> either completely succeed or
     * completely fail?
     */
    Level atomic() default Level.UNDEFINED;

    /**
     * If the VFS has been in a consistent state when the operation starts,
     * then does it <em>always</em> leave the VFS in a consistent state, too?
     */
    Level consistent() default Level.UNDEFINED;

    /**
     * Is it <em>always</em> impossible for other threads to see the effects
     * of any intermediate steps of the operation?
     */
    Level isolated() default Level.UNDEFINED;

    /**
     * If the operation has written any data, then is the change
     * <em>always</em> persisted to the top-level file system?
     */
    Level durable() default Level.UNDEFINED;

    /** The property level of a virtual file system operation. */
    enum Level { UNDEFINED, NOT_APPLICABLE, NO, YES }
}

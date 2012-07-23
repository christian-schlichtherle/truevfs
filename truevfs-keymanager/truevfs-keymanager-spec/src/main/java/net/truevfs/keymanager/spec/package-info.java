/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides an extensible manager for generic keys required to create or open
 * protected resources.
 * Its primary objective is to decouple...
 * <ul>
 * <li>the process to retrieve keys required to open or create protected
 *     resources from...
 * <li>the process to use and verify these keys.
 * </ul>
 * <p>
 * The process to retrieve keys is executed by the (abstract) classes and
 * interfaces in this package.
 * <p>
 * The process to use and optionally authenticate keys is executed by the users
 * of this package - called <i>client applications</i> or <i>clients</i> for
 * short.
 * <p>
 * A protected resource can be anything which can be referenced by a
 * {@link java.net.URI}:
 * As an example, it could be a URL to an AES encrypted file which the client
 * application is going to create or overwrite.
 * The key could be a password or a key file entered or selected by the user.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package net.truevfs.keymanager.spec;

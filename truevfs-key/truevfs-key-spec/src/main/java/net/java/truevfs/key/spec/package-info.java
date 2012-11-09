/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides an extensible manager for generic keys required for writing or
 * reading protected resources, for example encrypted files.
 * The primary objective of this API is to decouple...
 * <ul>
 * <li>the process to retrieve keys required for writing and reading protected
 *     resources from...
 * <li>the process to use and validate these keys.
 * </ul>
 * <p>
 * The process to retrieve keys is executed by the (abstract) classes and
 * interfaces in this package.
 * <p>
 * The process to use and optionally validate keys is executed by the users
 * of this package - called <i>client applications</i> or <i>clients</i> for
 * short.
 * <p>
 * A protected resource can be anything which can be referenced by a
 * {@link java.net.URI}:
 * As an example, it could be a URL to an encrypted file which the client
 * application is going to create or overwrite.
 * The key could be of any type, but typically its a user selectable password.
 * <p>
 * The sub-packages of this package provide partial implementations which can
 * be easily extended to adapt to different user interface or persistence
 * technologies, e.g. Java Swing or Apple's Keychain.
 * <p>
 * Concrete implementations are provided by different plug-in modules.
 * These modules get located by the
 * {@link net.java.truevfs.key.spec.sl.KeyManagerMapLocator#SINGLETON} by
 * saerching the class path at run time.
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truevfs.key.spec;

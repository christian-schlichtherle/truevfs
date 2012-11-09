/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides a partial key manager implementation which extends the safe key
 * manager package to adapt to different user interface technologies.
 * Although this package provides services to prompt users for secret keys,
 * it's completely agnostic to concrete user interface technologies.
 * Different modules are provided to adapt to concrete user interface
 * technologies, e.g. Swing or the Console.
 *
 * @since TrueVFS 0.10
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truevfs.key.spec.prompting;

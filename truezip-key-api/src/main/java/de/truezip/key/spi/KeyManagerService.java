/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.spi;

import de.truezip.key.AbstractKeyManagerProvider;
import de.truezip.key.sl.KeyManagerLocator;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract locatable service for key managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link KeyManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class KeyManagerService extends AbstractKeyManagerProvider {
}

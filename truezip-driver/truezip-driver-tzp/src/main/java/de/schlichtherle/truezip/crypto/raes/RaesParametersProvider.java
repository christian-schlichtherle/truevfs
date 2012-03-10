/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import javax.annotation.CheckForNull;

/**
 * These {@link RaesParameters} delegate to some other instance of a
 * sibling interface or itself in order to locate the parameters required to
 * read or write a RAES file of a given type.
 * This enables implementations to retrieve RAES parameters on demand
 * rather than providing them upfront for any possible type.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @author Christian Schlichtherle
 */
public interface RaesParametersProvider extends RaesParameters {

    /**
     * Returns {@link RaesParameters} of the given {@code type}
     * or {@code null} if not available.
     *
     * @param  <P> the type of the RAES parameters.
     * @param  type the {@link RaesParameters} interface class which's
     *         implementation is searched.
     * @return {@link RaesParameters} of the given {@code type}
     *         or {@code null} if not available.

     */
    @CheckForNull <P extends RaesParameters> P get(Class<P> type);
}

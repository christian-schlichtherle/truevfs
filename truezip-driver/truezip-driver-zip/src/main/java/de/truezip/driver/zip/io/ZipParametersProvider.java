/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import javax.annotation.CheckForNull;

/**
 * A provider of {@link ZipParameters} for a given type.
 * The implementation of this interface enables to retrieve ZIP parameters on
 * demand rather than providing them upfront for any possible type.
 *
 * @author  Christian Schlichtherle
 */
public interface ZipParametersProvider extends ZipParameters {

    /**
     * Returns {@link ZipParameters} of the given {@code type}
     * or {@code null} if not available.
     *
     * @param  type the {@link ZipParameters} interface class which's
     *         implementation is required.
     * @return {@link ZipParameters} of the given {@code type}
     *         or {@code null} if not available.
     */
    @CheckForNull <P extends ZipParameters> P get(Class<P> type);
}
/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides static utility methods for ZIP parameters.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipParametersUtils {

    private ZipParametersUtils() {
    }

    /**
     * Searches for {@code ZipParameters} of the given type starting from
     * the given parameters.
     * 
     * @param  <P> the type of the ZIP parameters to search for.
     * @param  type the type of the ZIP parameters to search for.
     * @param  param the parameters for starting the search.
     * @return The parameters of the given type.
     * @throws ZipParametersException if {@code param} is {@code null} or
     *         no suitable parameters can get found.
     */
    static <P extends ZipParameters> P parameters(
            final Class<P> type,
            @CheckForNull ZipParameters param)
    throws ZipParametersException {
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (type.isInstance(param)) {
                return type.cast(param);
            } else if (param instanceof ZipParametersProvider) {
                param = ((ZipParametersProvider) param).get(type);
            } else {
                break;
            }
        }
        throw new ZipParametersException("No suitable ZIP parameters available!");
    }
}

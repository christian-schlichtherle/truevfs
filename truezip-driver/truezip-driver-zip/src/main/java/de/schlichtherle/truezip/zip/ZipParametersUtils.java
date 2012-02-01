/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides static utility methods for ZIP parameters.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
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
    @SuppressWarnings("unchecked")
    static <P extends ZipParameters> P parameters(
            final Class<P> type,
            @CheckForNull ZipParameters param)
    throws ZipParametersException {
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (type.isAssignableFrom(param.getClass())) {
                return (P) param;
            } else if (param instanceof ZipParametersProvider) {
                param = ((ZipParametersProvider) param).get(type);
            } else {
                break;
            }
        }
        throw new ZipParametersException("No suitable ZIP parameters available!");
    }
}

/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides static utility methods for ZIP crypto features.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class ZipCryptoUtils {
    private ZipCryptoUtils() {
    }

    /**
     * Searches for {@code ZipCryptoParameters} of the given type starting from
     * the given parameters.
     * 
     * @param  <P> the type of the ZIP crypto parameters to search for.
     * @param  type the type of the ZIP crypto parameters to search for.
     * @param  param the parameters for starting the search.
     * @return The parameters of the given type.
     * @throws ZipCryptoParametersException if {@code param} is {@code null} or
     *         no suitable crypto parameters can get found.
     */
    @SuppressWarnings("unchecked")
    static <P extends ZipCryptoParameters> P parameters(
            final Class<P> type,
            final @CheckForNull ZipCryptoParameters param)
    throws ZipCryptoParametersException {
        // Order is important here to support multiple interface implementations!
        if (null == param) {
            throw new ZipCryptoParametersException("No crypto parameters available!");
        } else if (type.isAssignableFrom(param.getClass())) {
            return (P) param;
        } else if (param instanceof ZipCryptoParametersProvider) {
            return parameters(type,
                    ((ZipCryptoParametersProvider) param).get(type));
        } else {
            throw new ZipCryptoParametersException("No suitable crypto parameters available!");
        }
    }
}

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

/**
 * These {@link ZipCryptoParameters} delegate to some other instance of a
 * sibling interface or itself in order to locate the parameters required to
 * read or write a certain ZIP crypto type.
 * This enables implementations to retrieve ZIP crypto parameters on demand
 * rather than providing them upfront for any possible type.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface ZipCryptoParametersProvider extends ZipCryptoParameters {

    /**
     * Returns {@link ZipCryptoParameters} of the given {@code type}
     * or {@code null} if not available.
     *
     * @param  type the {@link ZipCryptoParameters} interface class which's
     *         implementation is searched.
     * @return {@link ZipCryptoParameters} of the given {@code type}
     *         or {@code null} if not available.
     */
    @CheckForNull <P extends ZipCryptoParameters> P get(Class<P> type);
}

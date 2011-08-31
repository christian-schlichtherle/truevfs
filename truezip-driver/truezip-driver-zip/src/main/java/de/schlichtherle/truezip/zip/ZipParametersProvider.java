/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A provider of {@link ZipParameters} for a given type.
 * The implementation of this interface enables to retrieve ZIP parameters on
 * demand rather than providing them upfront for any possible type.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
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

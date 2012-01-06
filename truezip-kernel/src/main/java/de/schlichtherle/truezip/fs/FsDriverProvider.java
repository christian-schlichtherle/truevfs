/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;

/**
 * A provider for an immutable map of file system schemes to drivers.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface FsDriverProvider {

    /**
     * Returns a dedicated immutable map of file system schemes to drivers.
     * Neither the keys nor the values of the returned map may be {@code null}
     * and subsequent calls must return the same map.
     *
     * @return A dedicated immutable map of file system schemes to drivers.
     */
    Map<FsScheme, FsDriver> get();
}

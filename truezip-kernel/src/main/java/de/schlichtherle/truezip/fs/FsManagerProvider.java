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

/**
 * A provider for the singleton file system manager.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface FsManagerProvider {

    /**
     * Returns the singleton file system manager.
     * <p>
     * Calling this method several times must return the <em>same</em> file
     * system manager in order to ensure integrity of the virtual file system
     * space.
     *
     * @return The file system manager.
     */
    FsManager get();
}

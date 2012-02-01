/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * Indicates that a file system is a false positive file system and that this
 * exception may get cached until the federated (archive) file system gets
 * {@linkplain FsController#sync(de.schlichtherle.truezip.util.BitField) synced}
 * again.
 * <p>
 * This exception type is reserved for use by a
 * {@link FsController file system controller} in order to reroute file system
 * operations to the parent file system of a false positive federated (archive)
 * file system.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class FsCacheableFalsePositiveException extends FsFalsePositiveException {
    FsCacheableFalsePositiveException(IOException cause) {
        super(cause);
    }
}

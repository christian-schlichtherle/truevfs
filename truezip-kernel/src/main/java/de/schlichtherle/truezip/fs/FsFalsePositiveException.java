/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * Indicates that a file system is a false positive file system.
 * <p>
 * This exception type is reserved for use by
 * {@link FsController file system controllers} in order to reroute file system
 * operations to the parent file system of a false positive federated (archive)
 * file system.
 *
 * @see     FsFalsePositiveController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public class FsFalsePositiveException extends FsControllerException {
    public FsFalsePositiveException(IOException cause) {
        super(cause);
        assert null != cause;
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}

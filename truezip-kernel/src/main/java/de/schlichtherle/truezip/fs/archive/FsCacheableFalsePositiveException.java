/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * A cacheable false positive exception.
 * <p>
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to reroute file system operations to the parent file system of a false
 * positive federated file system, i.e. a false positive archive file.
 * Unless there is a bug, an exception of this type <em>never</em> pops up to
 * a TrueZIP application and is <em>always</em> associated with another
 * {@link IOException} as its {@link #getCause()}.
 * <p>
 * ONLY THE TRUEZIP KERNEL SHOULD THROW AN EXCEPTION OF THIS TYPE!
 * DO NOT CREATE OR THROW AN EXCEPTION OF THIS TYPE (INCLUDING SUB-CLASSES)
 * ANYWHERE ELSE!
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
@DefaultAnnotation(NonNull.class)
final class FsCacheableFalsePositiveException extends FsFalsePositiveException {
    FsCacheableFalsePositiveException(IOException cause) {
        super(cause);
    }
}

/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that a file system is a false positive file system.
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to reroute file system operations to the parent file system of a false
 * positive federated file system, i.e. a false positive archive file.
 * Unless there is a bug, an exception of this type <em>never</em> pops up to
 * a TrueZIP application and is <em>always</em> associated with another
 * {@link IOException} as its {@link #getCause()}.
 *
 * @see     FsFederatingController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
@DefaultAnnotation(NonNull.class)
public class FsFalsePositiveException extends FsException {

    public FsFalsePositiveException(IOException cause) {
        super(cause);
        assert null != cause;
        assert !(cause instanceof FsException);
    }

    @Override
    public @NonNull IOException getCause() {
        return (IOException) super.getCause();
    }

    @Override
    public final FsFalsePositiveException initCause(Throwable cause) {
        assert null != super.getCause();
        assert super.getCause() instanceof IOException;
        super.initCause(cause);
        throw new AssertionError("The preceeding statement should have thrown an IllegalStateException");
    }
}

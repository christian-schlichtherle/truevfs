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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOException;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * federated file system to its parent file system.
 * Unless this is an instance of the sub-class {@link FsSyncWarningException},
 * an exception of this class implies that some or all
 * of the data of the federated file system have been lost.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class FsSyncException extends SequentialIOException {
    private static final long serialVersionUID = 4893219420357369739L;

    /**
     * This constructor is for exclusive use by {@link FsSyncExceptionBuilder}.
     *
     * @deprecated This method is only public in order to allow reflective
     *             access - do <em>not</em> call it directly!
     */
    @Deprecated
    public FsSyncException(String message) {
        super(message);
    }

    public FsSyncException(FsModel model, IOException cause) {
        super(model.getMountPoint().toString(), cause);
    }

    FsSyncException(FsModel model, IOException cause, int priority) {
        super(model.getMountPoint().toString(), cause, priority);
    }

    /** Returns the nullable cause of this exception. */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}

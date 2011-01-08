/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.concurrency;

import de.schlichtherle.truezip.io.fs.FSException1;

/**
 * Indicates that a controller is not write locked and the lock cannot get
 * acquired by the current thread for some reason.
 * Note that the write lock is required for any change to the state of the
 * archive controller - not only the state of the archive file system.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FSNotWriteLockedException extends FSException1 {

    private static final long serialVersionUID = 2345952581284762637L;

    FSNotWriteLockedException(FSConcurrencyModel model) {
        super(model);
    }

    FSNotWriteLockedException(FSConcurrencyModel model, FSNotWriteLockedException ex) {
        super(model, ex);
    }
}

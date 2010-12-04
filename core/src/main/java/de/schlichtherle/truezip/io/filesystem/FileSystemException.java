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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.ChainableIOException;
import java.io.IOException;

/**
 * Indicates an exceptional condition in a {@link FileSystemController}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileSystemException extends ChainableIOException {

    private static final long serialVersionUID = 2947623946725372554L;

    FileSystemException(String message) {
        super(message);
    }

    protected FileSystemException(FileSystemModel model) {
        super(model.getMountPoint().toString());
    }

    protected FileSystemException(FileSystemModel model, IOException cause) {
        super(model.getMountPoint().toString(), cause);
    }

    FileSystemException(FileSystemModel model, IOException cause, int priority) {
        super(model.getMountPoint().toString(), cause, priority);
    }

    /** Returns the nullable cause of this exception. */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}

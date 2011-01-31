/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import java.io.FileNotFoundException;

/**
 * Indicates that a file system entry could not get read or written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileBusyException extends FileNotFoundException {
    private static final long serialVersionUID = 2056108562576389242L;

    FileBusyException(String message) {
        super(message);
    }

    FileBusyException(Exception cause) {
        super(null == cause ? null : cause.toString());
        super.initCause(cause);
    }
}

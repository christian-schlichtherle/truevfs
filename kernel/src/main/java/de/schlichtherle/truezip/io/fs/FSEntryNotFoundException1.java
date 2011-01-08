/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Indicates that a file system entry does not exist or is not accessible.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FSEntryNotFoundException1 extends FileNotFoundException {

    private static final long serialVersionUID = 2972350932856838564L;

    private final FSPath1 path;

    public FSEntryNotFoundException1(
            final FSModel1 model,
            final FSEntryName1 name,
            final String msg) {
        super(msg);
        this.path = model.getMountPoint().resolve(name);
    }

    public FSEntryNotFoundException1(
            final FSModel1 model,
            final FSEntryName1 name,
            final IOException cause) {
        super(cause == null ? null : cause.toString());
        super.initCause(cause);
        this.path = model.getMountPoint().resolve(name);
    }

    @Override
    public String getMessage() {
        final String msg = super.getMessage();
        return msg != null
                ? new StringBuilder(path.toString()).append(" (").append(msg).append(")").toString()
                : path.toString();
    }
}

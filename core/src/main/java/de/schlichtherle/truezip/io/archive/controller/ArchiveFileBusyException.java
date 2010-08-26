/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.Archive;

/**
 * Indicates that an archive file could not get updated because some input or
 * output streams for its entries are still open.
 * The client application may recover from this exceptional condition by
 * unmounting the respective archive file, optionally forcing all input or
 * output streams to be closed.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileBusyException
extends ArchiveFileException {

    private static final long serialVersionUID = 1937356783082645716L;

    private final int numStreams;

    ArchiveFileBusyException(Archive archive, int numStreams) {
        super(archive);
        this.numStreams = numStreams;
    }

    /**
     * Returns the number of open entry streams, whereby an open stream
     * is a stream which's {@code close()} method hasn't been called.
     */
    public final int getNumStreams() {
        return numStreams;
    }
}

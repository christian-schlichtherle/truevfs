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

import de.schlichtherle.truezip.io.ArchiveControllers;
import de.schlichtherle.truezip.io.archive.metadata.ArchiveEntryStreamClosedException;

/**
 * Indicates that an archive file could not get updated because some input or
 * output streams for its entries are still open.
 * The canonical path name of the archive file is provided as the detail
 * message.
 * <p>
 * In order to recover from this exception, client applications may call
 * {@link ArchiveControllers#umount(String, boolean, boolean, boolean, boolean, boolean)}
 * in order to force all entry streams for all archive files to close and
 * prepare to catch the resulting {@link ArchiveFileBusyWarningException}.
 * A subsequent try to create the archive entry stream will then succeed
 * unless other exceptional conditions apply.
 * However, if the client application is still using a disconnected stream,
 * it will receive an {@link ArchiveEntryStreamClosedException} on the next
 * call to any other method than {@code close()}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileBusyException extends ArchiveControllerException {
    private static final long serialVersionUID = 1937861953461235716L;

    ArchiveFileBusyException(ArchiveControllerException priorException, String cPath) {
        super(priorException, cPath);
    }

    @Override
    public int getPriority() {
        return -2;
    }
}

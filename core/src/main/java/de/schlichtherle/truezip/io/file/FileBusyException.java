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

package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.archive.controller.ArchiveBusyException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveBusyWarningException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryStreamClosedException;
import java.io.FileNotFoundException;

/**
 * Indicates that an archive entry cannot get accessed because either...
 * <ul>
 * <li> the client application is trying to input or output to the same archive
 *      file concurrently and the respective archive driver does not support
 *      this, or
 * <li> the archive file needs an implicit umount which cannot get performed
 *      because the client application is still using some other open streams
 *      for the same archive file.
 * </ul>
 * <p>
 * In order to recover from this exception, client applications may call
 * {@link File#umount()} or {@link File#update()} in order to force all
 * entry streams for all archive files to close and prepare to catch the
 * resulting {@link ArchiveBusyWarningException}.
 * A subsequent try to create the archive entry stream will then succeed
 * unless any other exceptional condition occurs.
 * However, if the client application is still using a disconnected stream,
 * it will receive an {@link ArchiveEntryStreamClosedException} on the next
 * call to any other method than {@code close()}.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileBusyException extends FileNotFoundException {
    private static final long serialVersionUID = 2056108562576389242L;

    /**
     * For use by
     * {@link de.schlichtherle.truezip.io.archive.input.ArchiveInputBusyException} and
     * {@link de.schlichtherle.truezip.io.archive.output.ArchiveOutputBusyException} only.
     */
    protected FileBusyException(final String msg) {
        super(msg);
    }

    FileBusyException(ArchiveBusyException cause) {
        super(cause != null ? cause.toString() : null);
        initCause(cause);
    }
}

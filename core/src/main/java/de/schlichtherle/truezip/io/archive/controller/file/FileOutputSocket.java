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
package de.schlichtherle.truezip.io.archive.controller.file;

import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption.PRESERVE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.UNKNOWN;

/**
 * @see FileInputSocket
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FileOutputSocket extends CommonOutputSocket<FileEntry> {
    private final FileEntry entry;
    private final BitField<OutputOption> options;

    public FileOutputSocket(final FileEntry entry) {
        this(entry, BitField.noneOf(OutputOption.class));
    }

    public FileOutputSocket(    final FileEntry entry,
                                final BitField<OutputOption> options) {
        this.entry = entry;
        this.options = options;
    }

    @Override
    public FileEntry getTarget() {
        return entry;
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        class OutputStream extends FileOutputStream {
            OutputStream() throws FileNotFoundException {
                super(entry, options.get(APPEND));
            }

            @Override
            public void close() throws IOException {
                super.close();
                if (!options.get(PRESERVE))
                    return;
                final CommonEntry peer = getPeerTarget();
                if (null != peer)
                    for (final Access access : BitField.allOf(Access.class))
                        if (UNKNOWN != peer.getTime(access))
                            entry.setTime(access, peer.getTime(access));
            }
        }
        if (options.get(CREATE_PARENTS))
            entry.getParentFile().mkdirs();
        return new OutputStream();
    }
}

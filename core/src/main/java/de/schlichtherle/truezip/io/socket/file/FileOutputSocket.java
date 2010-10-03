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
package de.schlichtherle.truezip.io.socket.file;

import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileOutputSocket extends CommonOutputSocket<FileEntry> {
    private final FileEntry entry;

    public FileOutputSocket(final FileEntry entry) {
        this.entry = entry;
    }

    @Override
    public FileEntry getTarget() {
        return entry;
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return new FileOutputStream(entry);
    }
}
